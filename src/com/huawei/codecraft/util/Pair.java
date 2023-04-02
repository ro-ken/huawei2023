package com.huawei.codecraft.util;

import com.huawei.codecraft.core.Station;

public class Pair implements Comparable{
    public Station key;
    public double value;  // 价值，排序用,统一用时间，从小到大排序，小的价值高

    public Pair(Station key, double value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public int compareTo(Object o) {
        Pair op = (Pair)o;
        return Double.compare(value,op.value);
    }
    
    public Station getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }

    public double getValue() {
        return value;
    }
}