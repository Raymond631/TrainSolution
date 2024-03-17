package com.geekcun.trainsolution;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.bin.david.form.core.SmartTable;
import com.bin.david.form.data.style.FontStyle;
import com.bin.david.form.utils.DensityUtils;
import com.geekcun.trainsolution.dao.DatabaseHelper;
import com.geekcun.trainsolution.pojo.Result;
import com.geekcun.trainsolution.pojo.Train;
import com.geekcun.trainsolution.pojo.TrainTime;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.YenShortestPathIterator;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TableActivity extends AppCompatActivity {
    private final Handler handler = new Handler();
    private String TAG = "TableActivity";
    private SmartTable<Result> resultTable;
    private boolean isFirst = true;
    private DatabaseHelper dbHelper;
    private String fromStationName;
    private String toStationName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_table);
        FontStyle.setDefaultTextSize(DensityUtils.sp2px(this, 15));

        resultTable = (SmartTable<Result>) findViewById(R.id.table);
        dbHelper = new DatabaseHelper(TableActivity.this, "train.db", null, 1);

        Intent intent = getIntent();
        fromStationName = intent.getStringExtra("fromStationName");
        toStationName = intent.getStringExtra("toStationName");
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Thread(() -> {
            getShortPaths(fromStationName, toStationName);
        }).start();
    }


    public void getShortPaths(String source, String target) {
        DirectedWeightedMultigraph<String, DefaultWeightedEdge> graph = buildGraph();
        yenShortestPath(graph, source, target);
    }

    private DirectedWeightedMultigraph<String, DefaultWeightedEdge> buildGraph() {
        DirectedWeightedMultigraph<String, DefaultWeightedEdge> graph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);

        String sql = "select from_station_name, to_station_name, min(price) as price from train_data_processed group by from_station_name, to_station_name";
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, null);
        ) {
            int count = 0;
            // 处理查询结果
            while (cursor.moveToNext()) {
                count++;
                Log.e("edgeId", String.valueOf(count));

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
                if (count++ > 20) {
                    break;
                }
                GraphPath<String, DefaultWeightedEdge> path = iterator.next();

                List<String> p = path.getVertexList();
                List<Train> trainList = new ArrayList<>();
                for (int i = 0; i < p.size() - 1; i++) {
                    String u = p.get(i);
                    String v = p.get(i + 1);
                    double price = graph.getEdgeWeight(graph.getEdge(u, v));
                    trainList.add(new Train(u, v, price));
                }
                Result result = new Result(p, path.getWeight(), "");

                if (isFirst) {
                    isFirst = false;
                    handler.post(() -> resultTable.setData(Collections.singletonList(result)));
                } else {
                    handler.post(() -> {
                        resultTable.addData(Collections.singletonList(result), true);
                        resultTable.getMatrixHelper().flingBottom(200);
                        resultTable.getMatrixHelper().flingLeft(200);
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String computeTimeCost(List<Train> trainList) {
        // 查询每段车程的时间
        List<List<TrainTime>> pathList = new ArrayList<>();
        String sql = "SELECT start_time, arrive_time, arrive_day_diff FROM train_data_processed WHERE from_station_name = ? AND to_station_name = ? AND price = ?";
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            for (Train train : trainList) {
                String fromStationName = train.getFromStationName();
                String toStationName = train.getToStationName();
                double price = train.getPrice();

                try (Cursor cursor = db.rawQuery(sql, new String[]{fromStationName, toStationName, String.valueOf(price)})) {
                    List<TrainTime> partOfPath = new ArrayList<>();
                    while (cursor.moveToFirst()) {
                        partOfPath.add(new TrainTime(
                                cursor.getString(0),
                                cursor.getString(1),
                                cursor.getInt(2)
                        ));
                    }
                    pathList.add(partOfPath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 笛卡尔积
        List<List<TrainTime>> result = new ArrayList<>();
        cartesianProduct(pathList, result, 0, new ArrayList<>());

        // 计算最短时间
        int minTimeCost = Integer.MAX_VALUE;
        for (List<TrainTime> solution : result) {
            // 每趟车的跨天
            int dayDiff = solution.stream().mapToInt(TrainTime::getArriveDayDiff).sum();
            // 中转跨天
            for (int index = 0; index < solution.size() - 1; index++) {
                if (solution.get(index + 1).getStartTime().compareTo(solution.get(index).getEndTime()) < 0) {
                    dayDiff += 1;
                }
            }
            int timeSecond = timeStrCompute(solution.get(solution.size() - 1).getEndTime(), solution.get(0).getStartTime(), dayDiff);
            if (timeSecond < minTimeCost) {
                minTimeCost = timeSecond;
            }
        }
        int hour = minTimeCost / 3600;
        int minute = minTimeCost % 3600 / 60;
        return String.format("%d时%d分", hour, minute);
    }

    private void cartesianProduct(List<List<TrainTime>> input, List<List<TrainTime>> result, int index, List<TrainTime> current) {
        if (index == input.size()) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (TrainTime i : input.get(index)) {
            current.add(i);
            cartesianProduct(input, result, index + 1, current);
            current.remove(current.size() - 1);
        }
    }

    private int timeStrCompute(String endTime, String startTime, int dayDiff) {
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);
        Duration d = Duration.between(start, end).plusDays(dayDiff);
        return (int) d.getSeconds();
    }


}
