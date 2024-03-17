package com.geekcun.trainsolution.pojo;

public class TrainTime {
    private String startTime;
    private String endTime;
    private int arriveDayDiff;

    public TrainTime() {
    }

    public TrainTime(String startTime, String endTime, int arriveDayDiff) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.arriveDayDiff = arriveDayDiff;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getArriveDayDiff() {
        return arriveDayDiff;
    }

    public void setArriveDayDiff(int arriveDayDiff) {
        this.arriveDayDiff = arriveDayDiff;
    }

    @Override
    public String toString() {
        return "TrainTime{" +
                "startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", arriveDayDiff=" + arriveDayDiff +
                '}';
    }
}
