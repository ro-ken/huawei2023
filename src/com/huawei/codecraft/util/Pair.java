package com.huawei.codecraft.util;

public class Pair implements Comparable{
    Station key;
    double value;  // 价值，排序用,统一用时间，从小到大排序，小的价值高

    public Pair(Station key, double value) {
        this.key = key;
        this.value = value;
    }

    public Station getKey() {
        return key;
    }

    public double getValue() {
        return value;
    }

    @Override
    public int compareTo(Object o) {
        Pair op = (Pair)o;
        return Double.compare(value,op.value);
    }
}