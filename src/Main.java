import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


public class Main {


    public static void main(String[] args) throws SQLException {

        String day;
        String symbol;
        Scanner scan = new Scanner(System.in);
        System.out.print("Do you need to load the database? (y/n) ");
        String s = scan.next();

        if(s.equals("y")){
            loadJson();
        }


        while(true){
            System.out.print("Enter company symbol: ");
            symbol = scan.next();
            System.out.print("Enter date (yyyy-mm-dd): ");
            day = scan.next();
            dailyView(day, symbol);

            System.out.print("\nWould you like to see information about another company or day? (y/n) ");
            s = scan.next();
            if(s.equals("n")){
                System.out.println("\n\t\tThank you for using our Stock Aggregation Tool! Have a good one!");
                break;
            }
        }

    }

    public static void loadJson() throws SQLException {
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

        int i = 0;
        for (Stock s : stockList) {
            s.setId(i);
            i++;
        }

        String insertSQL = "INSERT INTO stocks (id, symbol, price, volume, date) " + "VALUES (?, ?, ?, ?, ?)";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(insertSQL)
        ) {
            for (Stock s : stockList) {
                stmt.setInt(1, s.getId());
                stmt.setString(2, s.getSymbol());
                stmt.setDouble(3, s.getPrice());
                stmt.setInt(4, s.getVolume());
                stmt.setTimestamp(5, new java.sql.Timestamp(s.getDate().getTime()));
               if(stmt.executeUpdate() != 1){
                    //error thrown
                    System.out.println("No rows were affected");
               }
            }
        }
    }

    public static void dailyView(String day, String symbol) throws SQLException {

        //Set up date params
        String endOfTheDay = day.substring(0, day.length() - 1) + (Integer.parseInt(day.substring(day.length() - 1, day.length())) + 1);

        String maxSQL = "SELECT price " +
                "FROM stocks WHERE price = " +
                "(SELECT MAX(price) " +
                "FROM stocks " +
                "WHERE symbol = ? AND date BETWEEN ? AND ?)";

        String minSQL = "SELECT price " +
                "FROM stocks WHERE price = " +
                "(SELECT MIN(price) " +
                "FROM stocks " +
                "WHERE symbol = ? AND date BETWEEN ? AND ?)";

        String volumeSQL = "SELECT SUM(volume) AS volume FROM stocks WHERE (symbol = ?) AND (date BETWEEN ? AND ?)";

        String closingSQL = "SELECT price FROM stocks " +
                "WHERE symbol = ? AND date BETWEEN ? AND ? " +
                "ORDER BY date DESC " +
                "LIMIT 1";

        ResultSet maxResults = null;
        ResultSet minResults = null;
        ResultSet volumeResults = null;
        ResultSet closingResults = null;

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

            if(maxResults.next()){
                System.out.println("Maximum stock price for " + symbol + " on " + day + ": " + maxResults.getDouble("price"));
            }
            if(minResults.next()){
                System.out.println("Minimum stock price for " + symbol + " on " + day + ": " + minResults.getDouble("price"));
            }
            if(volumeResults.next()){
                System.out.println("Total volume traded for " + symbol + " on " + day + ": " + volumeResults.getInt("volume"));
            }
            if(closingResults.next()){
                System.out.println("Closing price for " + symbol + " on " + day + ": " + closingResults.getDouble("price"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            maxResults.close();
            minResults.close();
            volumeResults.close();
        }

    }
}
