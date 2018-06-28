import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

public class Stock {

    @JsonIgnore
    private int id;

    private String symbol;
    private double price;
    private int volume;
    private Date date;

    public Stock(){}


    public Stock(int id, String symbol, double price, int volume, Date date){
        this.id = id;
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
