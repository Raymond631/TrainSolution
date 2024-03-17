package com.geekcun.trainsolution;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.geekcun.trainsolution.dao.DatabaseHelper;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private EditText fromStationNameEditText;
    private EditText toStationNameEditText;
    private Button button;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fromStationNameEditText = findViewById(R.id.fromStationName);
        toStationNameEditText = findViewById(R.id.toStationName);
        button = findViewById(R.id.search);

        // 数据库初始化
        dbHelper = new DatabaseHelper(MainActivity.this, "train.db", null, 1);
        try {
            String path = dbHelper.copyDBFile();
            Log.e(TAG, "拷贝成功，路径: " + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void search(View view) {
        Intent intent = new Intent(this, TableActivity.class);

        String fromStationName = fromStationNameEditText.getText().toString();
        String toStationName = toStationNameEditText.getText().toString();
        intent.putExtra("fromStationName", fromStationName);
        intent.putExtra("toStationName", toStationName);

        startActivity(intent);
    }
}