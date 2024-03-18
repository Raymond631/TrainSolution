package com.geekcun.trainsolution;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.geekcun.trainsolution.dao.DatabaseHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private AutoCompleteTextView fromStationNameEditText;
    private AutoCompleteTextView toStationNameEditText;

    private EditText maxTransferEditText;
    private EditText resultLengthEditText;

    private List<String> fromStationNameList = new ArrayList<>();
    private List<String> toStationNameList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fromStationNameEditText = findViewById(R.id.fromStationName);
        toStationNameEditText = findViewById(R.id.toStationName);
        maxTransferEditText = findViewById(R.id.maxTransfer);
        resultLengthEditText = findViewById(R.id.resultLength);

        // 数据库初始化
        try (DatabaseHelper dbHelper = new DatabaseHelper(MainActivity.this, "train.db", null, 1)) {
            String path = dbHelper.copyDBFile();
            Log.e(TAG, "拷贝成功，路径: " + path);
            getStationName(dbHelper);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 输入框自动补全
        fromStationNameEditText.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, fromStationNameList));
        toStationNameEditText.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, toStationNameList));
    }

    public void getStationName(DatabaseHelper dbHelper) {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            String sql = "SELECT DISTINCT from_station_name from train_data_processed";
            try (Cursor cursor = db.rawQuery(sql, null)) {
                while (cursor.moveToNext()) {
                    fromStationNameList.add(cursor.getString(0));
                }
            }
            sql = "SELECT DISTINCT to_station_name from train_data_processed";
            try (Cursor cursor = db.rawQuery(sql, null)) {
                while (cursor.moveToNext()) {
                    toStationNameList.add(cursor.getString(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void search(View view) {
        Intent intent = new Intent(this, TableActivity.class);

        String fromStationName = fromStationNameEditText.getText().toString();
        String toStationName = toStationNameEditText.getText().toString();
        int maxTransfer = Integer.parseInt(maxTransferEditText.getText().toString());
        int resultLength = Integer.parseInt(resultLengthEditText.getText().toString());

        intent.putExtra("fromStationName", fromStationName);
        intent.putExtra("toStationName", toStationName);
        intent.putExtra("maxTransfer", maxTransfer);
        intent.putExtra("resultLength", resultLength);

        startActivity(intent);
    }
}