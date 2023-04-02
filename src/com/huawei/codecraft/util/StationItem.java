package com.huawei.codecraft.util;

public class StationItem{
    public int type;
    public int[] call;
    public int period;
    public int[] canSell;  // product can sell for who

    public StationItem(int type, int[] call, int period,int[] canSell) {
        this.type = type;
        this.call = call;
        this.period = period;
        this.canSell = canSell;
    }
}