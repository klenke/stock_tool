import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.sql.*;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


public class Main {

    static boolean verbose = false;

    public static void main(String[] args) throws SQLException {

        String day;
        String symbol;
        Scanner scan = new Scanner(System.in);
        System.out.print("Do you need to load the database? (y/n) ");
        String s = scan.next();

        if(s.equals("y")){
            //clears table and reloads data
            loadJson();
        }

        while(true){
            System.out.print("Would you like more verbose output? (y/n) ");
            s = scan.next();
            if(s.equals("y")){
                verbose = true;
            } else if (s.equals("n")){
                verbose = false;
            } else {
                continue;
            }
            System.out.print("Enter company symbol: ");
            symbol = scan.next();
            System.out.print("Enter date (yyyy-mm-dd): ");
            day = scan.next();
            dailyView(day, symbol);

            System.out.print("\nWould you like to see information about another company or day? (y/n) ");
            s = scan.next();
            if(s.equals("n")){
                System.out.println("\n\n\t\tThank you for using our Stock Aggregation Tool! Have a good one!\n");
                break;
            }
        }

    }

    public static void loadJson() throws SQLException {

        //read from the json
        List<Stock> stockList;
        try {
            ObjectMapper mapper = new ObjectMapper();
            stockList = Arrays.asList(mapper.readValue(new File("files/week1-stocks.json"), Stock[].class));
        } catch (JsonParseException e) {
            e.printStackTrace();
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //set the id
        int i = 0;
        for (Stock s : stockList) {
            s.setId(i);
            i++;
        }

        //set up SQL queries
        String clearTable = "TRUNCATE TABLE stocks";
        String insertSQL = "INSERT INTO stocks (id, symbol, price, volume, date) " + "VALUES (?, ?, ?, ?, ?)";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement clear = conn.prepareStatement(clearTable);
                PreparedStatement stmt = conn.prepareStatement(insertSQL)
        ) {
            if(clear.executeUpdate() != 0){
                //TRUNCATE TABLE expected to return 0 on success
                System.out.println("Error clearing database");
            } else {
                for (Stock s : stockList) {
                    stmt.setInt(1, s.getId());
                    stmt.setString(2, s.getSymbol());
                    stmt.setDouble(3, s.getPrice());
                    stmt.setInt(4, s.getVolume());
                    stmt.setTimestamp(5, new java.sql.Timestamp(s.getDate().getTime()));
                    if (stmt.executeUpdate() != 1) {
                        //error thrown
                        System.out.println("No rows were affected");
                    }
                }
            }
        }
    }

    public static void dailyView(String day, String symbol) throws SQLException {

        //Set up date params
        String endOfTheDay = day.substring(0, day.length() - 1) + (Integer.parseInt(day.substring(day.length() - 1, day.length())) + 1);
        String beginOfTheMonth = day.substring(0, 7) + "-00";
        String endOfTheMonth = day.substring(0,7) + "-31";

        String maxSQL = "SELECT price, date " +
                "FROM stocks WHERE price = " +
                "(SELECT MAX(price) " +
                "FROM stocks " +
                "WHERE symbol = ? AND date BETWEEN ? AND ?)";

        String minSQL = "SELECT price, date " +
                "FROM stocks WHERE price = " +
                "(SELECT MIN(price) " +
                "FROM stocks " +
                "WHERE symbol = ? AND date BETWEEN ? AND ?)";

        String volumeSQL = "SELECT SUM(volume) AS volume FROM stocks WHERE (symbol = ?) AND (date BETWEEN ? AND ?)";

        String closingSQL = "SELECT price, date FROM stocks " +
                "WHERE symbol = ? AND date BETWEEN ? AND ? " +
                "ORDER BY date DESC " +
                "LIMIT 1";

        ResultSet maxResults = null;
        ResultSet minResults = null;
        ResultSet volumeResults = null;
        ResultSet closingResults = null;
        //format stock pricing
        NumberFormat nf = NumberFormat.getCurrencyInstance();

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement maxStmt = conn.prepareStatement(maxSQL, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                PreparedStatement minStmt = conn.prepareStatement(minSQL, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                PreparedStatement volumeStmt = conn.prepareStatement(volumeSQL, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                PreparedStatement closingStmt = conn.prepareStatement(closingSQL, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
        ) {
            maxStmt.setString(1, symbol);
            maxStmt.setString(2, day);
            maxStmt.setString(3, endOfTheDay);

            minStmt.setString(1, symbol);
            minStmt.setString(2, day);
            minStmt.setString(3, endOfTheDay);

            volumeStmt.setString(1, symbol);
            volumeStmt.setString(2, day);
            volumeStmt.setString(3, endOfTheDay);

            closingStmt.setString(1, symbol);
            closingStmt.setString(2, day);
            closingStmt.setString(3, endOfTheDay);

            maxResults = maxStmt.executeQuery();
            minResults = minStmt.executeQuery();
            volumeResults = volumeStmt.executeQuery();
            closingResults = closingStmt.executeQuery();

            System.out.println("\nDaily Stock Information (" + day + "):");
            if(maxResults.next()){
                if(verbose){
                    System.out.print("\tMaximum stock price for " + symbol + " on " + day + ":\t" + maxResults.getDouble("price"));
                    String d = maxResults.getString("date");
                    System.out.println("\tat" + d.substring(10, d.length() - 3));
                } else {
                    System.out.println("\tMax:\t\t" + maxResults.getDouble("price"));
                }
            }
            if(minResults.next()){
                if(verbose){
                    System.out.print("\tMinimum stock price for " + symbol + " on " + day + ":\t" + minResults.getDouble("price"));
                    String d = minResults.getString("date");
                    System.out.println("\tat" + d.substring(10, d.length() - 3));
                } else {
                    System.out.println("\tMin:\t\t" + minResults.getDouble("price"));
                }
            }
            if(volumeResults.next()){
                if(verbose){
                    System.out.println("\tTotal volume traded for " + symbol + " on " + day + ":\t" + volumeResults.getInt("volume"));
                } else {
                    System.out.println("\tVolume:\t\t" + volumeResults.getInt("volume"));
                }

            }
            if(closingResults.next()){
                if(verbose){
                    System.out.println("\tClosing price for " + symbol + " on " + day + ":\t\t" + closingResults.getDouble("price"));
                } else {
                    System.out.println("\tClosing:\t" + closingResults.getDouble("price"));
                }

            }

            //AGGREGATE FULL MONTH INFO
            maxStmt.setString(1, symbol);
            maxStmt.setString(2, beginOfTheMonth);
            maxStmt.setString(3, endOfTheMonth);

            minStmt.setString(1, symbol);
            minStmt.setString(2, beginOfTheMonth);
            minStmt.setString(3, endOfTheMonth);

            volumeStmt.setString(1, symbol);
            volumeStmt.setString(2, beginOfTheMonth);
            volumeStmt.setString(3, endOfTheMonth);

            maxResults = maxStmt.executeQuery();
            minResults = minStmt.executeQuery();
            volumeResults = volumeStmt.executeQuery();


            System.out.println("\nMonthly Stock Information (" + day.substring(0,7) + "):");
            if(maxResults.next()){
                if(verbose){
                    System.out.print("\tMaximum stock price for " + symbol + " in " + day.substring(0,7) + ":\t" + maxResults.getDouble("price"));
                    String d = maxResults.getString("date");
                    System.out.println("\ton " + d.substring(5, 10) + " at " + d.substring(10, d.length()-3));
                } else {
                    System.out.println("\tMax:\t\t" + maxResults.getDouble("price"));
                }
            }
            if(minResults.next()){
                if(verbose){
                    System.out.print("\tMinimum stock price for " + symbol + " in " + day.substring(0,7) + ":\t" + minResults.getDouble("price"));
                    String d = minResults.getString("date");
                    System.out.println("\ton " + d.substring(5, 10) + " at " + d.substring(10, d.length()-3));
                } else {
                    System.out.println("\tMin:\t\t" + minResults.getDouble("price"));
                }
            }
            if(volumeResults.next()){
                if(verbose){
                    System.out.println("\tTotal volume traded for " + symbol + " in " + day.substring(0,7) + ":\t" + volumeResults.getInt("volume"));
                } else {
                    System.out.println("\tVolume:\t\t" + volumeResults.getInt("volume"));
                }

            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (maxResults != null) {
                maxResults.close();
            }
            if (minResults != null) {
                minResults.close();
            }
            if (volumeResults != null) {
                volumeResults.close();
            }
            if (closingResults != null) {
                closingResults.close();
            }
        }

    }
}
