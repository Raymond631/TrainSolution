package com.geekcun.trainsolution.pojo;

import com.bin.david.form.annotation.SmartColumn;
import com.bin.david.form.annotation.SmartTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@SmartTable(name = "计算结果")
public class Result {
    @SmartColumn(id = 1, name = "路径")
    private List<String> path;
    @SmartColumn(id = 2, name = "总价格")
    private double price;
    @SmartColumn(id = 3, name = "总耗时")
    private String timeCost;
}
