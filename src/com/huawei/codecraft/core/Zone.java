package com.huawei.codecraft.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// 地图划分为不同的区域，每个区域不连通
public class Zone {
    public int id;  // 区域号
    public  Map<Integer, ArrayList<Station>> stationsMap = new HashMap<>(); // 类型，以及对应的工作站集合

    public ArrayList<Robot> robots = new ArrayList<>();
    public Zone(int id) {
        this.id = id;
    }
    public Zone() {
    }

    @Override
    public String toString() {
        return "Zone{" +
                "id=" + id +
                ", stationsMap=" + stationsMap +
                ", robots=" + robots +
                '}';
    }
}
