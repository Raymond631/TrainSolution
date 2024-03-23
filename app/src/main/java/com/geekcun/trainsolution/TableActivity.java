package com.geekcun.trainsolution;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import com.bin.david.form.core.SmartTable;
import com.bin.david.form.data.style.FontStyle;
import com.bin.david.form.utils.DensityUtils;
import com.geekcun.trainsolution.dao.DatabaseHelper;
import com.geekcun.trainsolution.pojo.Result;
import com.geekcun.trainsolution.pojo.Trip;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.YenShortestPathIterator;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class TableActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.myLooper());
    private DatabaseHelper dbHelper;
    private SmartTable<Result> resultTable;
    private boolean isFirst = true;

    private String fromStationName;
    private String toStationName;
    private int maxTransfer;
    private int resultLength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_table);
        FontStyle.setDefaultTextSize(DensityUtils.sp2px(this, 15));

        resultTable = findViewById(R.id.table);
        resultTable.getConfig().setShowXSequence(false);
        dbHelper = new DatabaseHelper(TableActivity.this, "train.db", null, 1);

        Intent intent = getIntent();
        fromStationName = intent.getStringExtra("fromStationName");
        toStationName = intent.getStringExtra("toStationName");
        maxTransfer = intent.getIntExtra("maxTransfer", 5);
        resultLength = intent.getIntExtra("resultLength", 20);

        Thread thread = new Thread(this::getShortPaths);
        thread.start();
    }

    public void getShortPaths() {
        long start = System.currentTimeMillis();

        DirectedWeightedMultigraph<String, DefaultWeightedEdge> graph = buildGraph();
        yenShortestPath(graph, fromStationName, toStationName);

        long end = System.currentTimeMillis();
        Log.v("计算耗时", String.valueOf((end - start)));
    }

    private DirectedWeightedMultigraph<String, DefaultWeightedEdge> buildGraph() {
        DirectedWeightedMultigraph<String, DefaultWeightedEdge> graph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);

        String sql = "select from_station_name, to_station_name, min(price) as price from train_data_processed group by from_station_name, to_station_name";
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, null)
        ) {
            while (cursor.moveToNext()) {
                String fromStationName = cursor.getString(0);
                String toStationName = cursor.getString(1);
                double price = cursor.getDouble(2);

                graph.addVertex(fromStationName);
                graph.addVertex(toStationName);
                graph.addEdge(fromStationName, toStationName);
                graph.setEdgeWeight(fromStationName, toStationName, price);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return graph;
    }

    private void yenShortestPath(DirectedWeightedMultigraph<String, DefaultWeightedEdge> graph, String source, String target) {
        try {
            YenShortestPathIterator<String, DefaultWeightedEdge> iterator = new YenShortestPathIterator<>(graph, source, target);
            int count = 0;
            while (iterator.hasNext()) {
                // 数量足够
                if (count++ >= resultLength) {
                    break;
                }

                GraphPath<String, DefaultWeightedEdge> path = iterator.next();
                // 中转超限
                if (path.getLength() > maxTransfer + 1) {
                    continue;
                }

                List<List<Trip>> routes = getTrips(path.getVertexList());
                String timeCost = findOptimalTrips(routes);
                Result result = new Result(path.getVertexList(), path.getWeight(), timeCost);

                // 刷新Table
                if (isFirst) {
                    isFirst = false;
                    handler.post(() -> resultTable.setData(Collections.singletonList(result)));
                } else {
                    handler.post(() -> {
                        resultTable.addData(Collections.singletonList(result), true);
                        resultTable.getMatrixHelper().flingBottom(200);
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<List<Trip>> getTrips(List<String> stationList) {
        // 查询每段车程的时间
        List<List<Trip>> routes = new ArrayList<>();
        String sql = "SELECT start_time, arrive_time, arrive_day_diff FROM train_data_processed WHERE from_station_name = ? AND to_station_name = ? AND price = ( SELECT MIN( price ) FROM train_data_processed WHERE from_station_name = ? AND to_station_name = ? );";
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            for (int i = 0, length = stationList.size() - 1; i < length; i++) {
                String u = stationList.get(i);
                String v = stationList.get(i + 1);
                // 查询数据
                try (Cursor cursor = db.rawQuery(sql, new String[]{u, v, u, v})) {
                    List<Trip> partOfPath = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        partOfPath.add(
                                new Trip(
                                        LocalTime.parse(cursor.getString(0)),
                                        LocalTime.parse(cursor.getString(1)),
                                        cursor.getInt(2),
                                        Duration.ZERO,
                                        new ArrayList<>()
                                )
                        );
                    }
                    routes.add(partOfPath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return routes;
    }

    /**
     * @author CherryRum
     */
    public String findOptimalTrips(List<List<Trip>> routes) {
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

    private Duration timeSubtraction(LocalTime endTime, LocalTime startTime, int dayDiff) {
        Duration res = Duration.between(startTime, endTime);
        res = res.plusDays(dayDiff);
        return res;
    }

    private Duration timeSubtraction(LocalTime endTime, LocalTime startTime) {
        Duration res = Duration.between(startTime, endTime);
        if (startTime.isAfter(endTime)) {
            res = res.plusDays(1);
        }
        return res;
    }
}
