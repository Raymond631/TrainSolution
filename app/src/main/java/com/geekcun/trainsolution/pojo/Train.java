package com.geekcun.trainsolution.pojo;

public class Train {
    private String fromStationName;
    private String toStationName;
    private double price;

    public Train() {
    }

    public Train(String fromStationName, String toStationName, double price) {
        this.fromStationName = fromStationName;
        this.toStationName = toStationName;
        this.price = price;
    }

    public String getFromStationName() {
        return fromStationName;
    }

    public void setFromStationName(String fromStationName) {
        this.fromStationName = fromStationName;
    }

    public String getToStationName() {
        return toStationName;
    }

    public void setToStationName(String toStationName) {
        this.toStationName = toStationName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Train{" +
                "fromStationName='" + fromStationName + '\'' +
                ", toStationName='" + toStationName + '\'' +
                ", price=" + price +
                '}';
    }
}
