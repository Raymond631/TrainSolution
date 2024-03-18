package com.geekcun.trainsolution;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
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
    private Thread thread;
    private String TAG = "TableActivity";
    private SmartTable<Result> resultTable;
    private boolean isFirst = true;
    private DatabaseHelper dbHelper;

    private DirectedWeightedMultigraph<String, DefaultWeightedEdge> graph;

    private String fromStationName;
    private String toStationName;
    private int maxTransfer;
    private int resultLength;

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
        maxTransfer = intent.getIntExtra("maxTransfer", 5);
        resultLength = intent.getIntExtra("resultLength", 20);

        thread = new Thread(this::getShortPaths);
        thread.start();
    }


    public void getShortPaths() {
        buildGraph();
        yenShortestPath(fromStationName, toStationName);
    }

    private void buildGraph() {
        graph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);

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
    }

    private void yenShortestPath(String source, String target) {
        try {
            YenShortestPathIterator<String, DefaultWeightedEdge> iterator = new YenShortestPathIterator<>(graph, source, target);
            int count = 0;
            while (iterator.hasNext()) {
                if (Thread.currentThread().isInterrupted()) {
                    graph = null;
                    throw new InterruptedException();
                }
                if (count++ >= resultLength) {
                    graph = null;
                    break;
                }
                GraphPath<String, DefaultWeightedEdge> path = iterator.next();
                if (path.getLength() > maxTransfer + 1) {
                    continue;
                }

                Result result = new Result(path.getVertexList(), path.getWeight(), "");
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

    // 点击函数
    public void addTime(View view) {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        List<Result> results = resultTable.getTableData().getT();
        for (int i = 0, len1 = results.size(); i < len1; i++) {
            List<String> path = results.get(i).getPath();  // 路径

            List<Train> trainList = new ArrayList<>();  // 分段路径
            for (int j = 0, len2 = path.size(); j < len2 - 1; j++) {
                String u = path.get(j);
                String v = path.get(j + 1);
                double price = 0;
                String sql = "select min(price) as price from train_data_processed where from_station_name = ? and to_station_name = ?";
                try (SQLiteDatabase db = dbHelper.getReadableDatabase(); Cursor cursor = db.rawQuery(sql, new String[]{u, v})) {
                    if (cursor.moveToFirst()) {
                        price = cursor.getDouble(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                trainList.add(new Train(u, v, price));
            }
            results.get(i).setTimeCost(computeTimeCost(trainList));
            handler.post(() -> resultTable.setData(results));
        }
    }


    private String computeTimeCost(List<Train> trainList) {
        // 查询每段车程的时间
        List<List<TrainTime>> pathList = new ArrayList<>();
        String sql = "SELECT start_time, arrive_time, arrive_day_diff FROM train_data_processed WHERE from_station_name = ? AND to_station_name = ? AND price = ?";
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            for (Train train : trainList) {
                try (Cursor cursor = db.rawQuery(sql, new String[]{train.getFromStationName(), train.getToStationName(), String.valueOf(train.getPrice())})) {
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
        List<List<TrainTime>> result = cartesianProduct(pathList);

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

    private <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
        List<List<T>> result = new ArrayList<>();
        result.add(new ArrayList<>());

        for (List<T> list : lists) {
            List<List<T>> temp = new ArrayList<>();
            for (List<T> res : result) {
                for (T item : list) {
                    List<T> newList = new ArrayList<>(res);
                    newList.add(item);
                    temp.add(newList);
                }
            }
            result = temp;
        }
        return result;
    }

//    private void cartesianProduct(List<List<TrainTime>> input, List<List<TrainTime>> result, int index, List<TrainTime> current) {
//        if (index == input.size()) {
//            result.add(new ArrayList<>(current));
//            return;
//        }
//        for (TrainTime i : input.get(index)) {
//            current.add(i);
//            cartesianProduct(input, result, index + 1, current);
//            current.remove(current.size() - 1);
//        }
//    }

    private int timeStrCompute(String endTime, String startTime, int dayDiff) {
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);
        Duration d = Duration.between(start, end).plusDays(dayDiff);
        return (int) d.getSeconds();
    }


}
