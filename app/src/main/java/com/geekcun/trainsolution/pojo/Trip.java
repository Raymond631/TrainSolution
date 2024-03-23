package com.geekcun.trainsolution.pojo;

import lombok.Data;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;

@Data
public class Trip implements Comparable<Trip> {
    private LocalTime departure;
    private LocalTime arrival;
    private int dayDiff;
    private Duration totalDuration;
    private ArrayList<Trip> path;

    public Trip(LocalTime departure, LocalTime arrival, int dayDiff, Duration totalDuration, ArrayList<Trip> path) {
        this.departure = departure;
        this.arrival = arrival;
        this.dayDiff = dayDiff;
        this.totalDuration = totalDuration;
        this.path = new ArrayList<>(path);
        this.path.add(this);
    }

    public Trip(LocalTime departure, LocalTime arrival, Duration totalDuration, ArrayList<Trip> path) {
        this.departure = departure;
        this.arrival = arrival;
        this.totalDuration = totalDuration;
        this.path = new ArrayList<>(path);
        this.path.add(this);
    }

    @Override
    public int compareTo(Trip other) {
        return this.totalDuration.compareTo(other.totalDuration);
    }
}
