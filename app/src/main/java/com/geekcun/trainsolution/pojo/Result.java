package com.geekcun.trainsolution.pojo;

import com.bin.david.form.annotation.SmartColumn;
import com.bin.david.form.annotation.SmartTable;

import java.util.List;

@SmartTable(name = "计算结果")
public class Result {
    @SmartColumn(id = 1, name = "路径")
    private List<String> path;
    @SmartColumn(id = 2, name = "总价格")
    private double price;
    @SmartColumn(id = 3, name = "总耗时")
    private String timeCost;

    public Result() {
    }

    public Result(List<String> path, double price, String timeCost) {
        this.path = path;
        this.price = price;
        this.timeCost = timeCost;
    }


    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getTimeCost() {
        return timeCost;
    }

    public void setTimeCost(String timeCost) {
        this.timeCost = timeCost;
    }

    @Override
    public String toString() {
        return "Result{" +
                "path=" + path +
                ", price=" + price +
                ", timeCost=" + timeCost +
                '}';
    }
}
