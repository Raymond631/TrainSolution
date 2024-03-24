package com.geekcun.trainsolution.utils;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import com.geekcun.trainsolution.pojo.Trip;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class TimeUtils {
    /**
     * @author CherryRum
     */
    public static String findOptimalTrips(List<List<Trip>> routes) {
        // 最小堆
        PriorityQueue<Trip> pq = new PriorityQueue<>();
        for (Trip trip : routes.get(0)) {
            pq.add(new Trip(trip.getDeparture(), trip.getArrival(), timeSubtraction(trip.getArrival(), trip.getDeparture(), trip.getDayDiff()), new ArrayList<>()));
        }

        Trip bestTrips = null;
        while (!pq.isEmpty()) {
            Trip current = pq.poll();
            assert current != null;

            // 计算出时间最短的方案
            if (current.getPath().size() == routes.size()) {
                bestTrips = current;
                break;
            }

            int nextIndex = current.getPath().size();
            for (Trip nextTrip : routes.get(nextIndex)) {
                Duration waitTime = timeSubtraction(nextTrip.getDeparture(), current.getArrival());
                Duration totalDuration = current.getTotalDuration().plus(waitTime).plus(timeSubtraction(nextTrip.getArrival(), nextTrip.getDeparture(), nextTrip.getDayDiff()));
                pq.add(new Trip(nextTrip.getDeparture(), nextTrip.getArrival(), totalDuration, current.getPath()));
            }
        }

        assert bestTrips != null;
        return DateUtil.formatBetween(bestTrips.getTotalDuration().getSeconds() * 1000, BetweenFormatter.Level.MINUTE);
    }

    private static Duration timeSubtraction(LocalTime endTime, LocalTime startTime, int dayDiff) {
        Duration res = Duration.between(startTime, endTime);
        res = res.plusDays(dayDiff);
        return res;
    }

    private static Duration timeSubtraction(LocalTime endTime, LocalTime startTime) {
        Duration res = Duration.between(startTime, endTime);
        if (startTime.isAfter(endTime)) {
            res = res.plusDays(1);
        }
        return res;
    }
}
