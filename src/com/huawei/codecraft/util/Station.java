package com.huawei.codecraft.util;

import com.huawei.codecraft.Main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

class Pair{
    Station key;
    Integer value;

    public Pair(Station key, Integer value) {
        this.key = key;
        this.value = value;
    }

    public Station getKey() {
        return key;
    }

    public Integer getValue() {
        return value;
    }
}

/**
 * @Author: ro_kin
 * @Data:2023/3/13 19:47
 * @Description: TODO
 */
public class Station{
    public static final StationItem[] item;
    public static double emptyMinDistance; // 加速减速临界值 ,空载
    public static double fullMinDistance; // 加速减速临界值 ，满载

    public PriorityQueue<Pair> canSellStations;
    public Station availNextStation;

    int Id;
    public int type;   // 1-9
    double x;
    double y;
    public int leftTime;   // 剩余时间，帧数 -1：未生产 ； 0 阻塞；>0 剩余帧数
    public int rowStatus; // 原材料 二进制位表描述，例如 48(110000) 表示拥有物品 4 和 5
    public int proStatus; // 产品格 状态 0 无 1 有
    public boolean[] bookRow;    // 原料空格是否预定
    public boolean bookPro;      // 产品空格是否预定

    static {
        item = new StationItem[10];
        item[1] = new StationItem(1,new int[]{},50,new int[]{4,5,9});
        item[2] = new StationItem(2,new int[]{},50,new int[]{4,6,9});
        item[3] = new StationItem(3,new int[]{},50,new int[]{5,6,9});
        item[4] = new StationItem(4,new int[]{1,2},500,new int[]{7,9});
        item[5] = new StationItem(5,new int[]{1,3},500,new int[]{7,9});
        item[6] = new StationItem(6,new int[]{2,3},500,new int[]{7,9});
        item[7] = new StationItem(7,new int[]{4,5,6},1000,new int[]{8,9});
        item[8] = new StationItem(8,new int[]{7},1,new int[]{});
        item[9] = new StationItem(9,new int[]{1,2,3,4,5,6,7},1,new int[]{});

        emptyMinDistance = calcMinDistance(true);
        fullMinDistance = calcMinDistance(false);

    }

    public boolean positionIsFull(int type){
        return bitJudge(rowStatus,type);
    }

    //是否有某类型的货物
    public boolean bitJudge(int goodStatus,int type){
        return (goodStatus>>type & 1) == 1;
    }

    @Override
    public String toString() {
        return "Sta{" +
                "id=" + Id +
                ", type=" + type +
                ", x=" + x +
                ", y=" + y +
                '}';
    }

    private static double calcMinDistance(boolean isEmpty) {
        double a = isEmpty ? Robot.emptyA:Robot.fullA;
        return Math.pow(Robot.maxSpeed,2)/(a);
    }

    public Station(int id, int type, double x, double y) {
        Id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        bookPro = false;
        bookRow = new boolean[8];
    }

    // 距离换算成时间
    public double distanceToSecond(boolean isEmpty, double ox,double oy){
        //两种情况， 加速，匀速，减速  or  加速 ，减速
        double minDistance = isEmpty?emptyMinDistance:fullMinDistance;
        double a = isEmpty ? Robot.emptyA:Robot.fullA;
        double distance = calcDistance(ox,oy);
        double second ;
        if (distance <= minDistance){
            second = Math.pow(distance/a,0.5);
        }else {
            second = Math.pow(minDistance/a,0.5) + (distance-minDistance)/a;
        }
        return second;
    }

    public double calcDistance(double ox, double oy) {
        double tmp = Math.pow(x-ox,2) + Math.pow(y-oy,2);
        return Math.pow(tmp,0.5);
    }

    // 距离换算成帧数
    public int distanceToFps(boolean isEmpty, double ox,double oy){
        double second = distanceToSecond(isEmpty,ox,oy);
        int fps = (int) (second * 50);
        return fps;
    }

    public void initialization() {

        canSellStations = new PriorityQueue<>(new Comparator<Pair>() {
            @Override
            public int compare(Pair o1, Pair o2) {
                return o2.getValue() - o1.getValue();   // 大顶堆
            }
        });
        if (type<=7){
            int[] canSell = item[type].canSell;
            for(int tp:canSell){
                if(!Main.map.containsKey(tp)){
                    continue;
                }
                ArrayList<Station> stations = Main.map.get(tp);
                for(int i=0;i<stations.size();i++) {
                    Station st = stations.get(i);
                    Integer value = calcValue(st.x,st.y,false);
                    Pair pair = new Pair(st,value);
                    canSellStations.add(pair);
                }
            }
        }
    }

    public int calcValue(double x1,double y1,boolean isEmpty) {

        if (type>7) return 0;
        int baseMoney = Goods.item[type].earn;
        int fps = distanceToFps(isEmpty,x1,y1);
        int theoryMoney = (int) (baseMoney * Robot.calcTimeValue(fps));

        return theoryMoney;
    }

    // 没有可用返回 null
    public Station chooseAvailableNextStation() {
        for(Pair pair : canSellStations){
            Station oth = pair.getKey();
            if (!oth.bookRow[type] && !oth.positionIsFull(type)){
                availNextStation = oth;
                return oth;
            }
        }
        return null;
    }

}

class StationItem{
    int type;
    int[] call;
    int period;
    int[] canSell;  // product can sell for who

    public StationItem(int type, int[] call, int period,int[] canSell) {
        this.type = type;
        this.call = call;
        this.period = period;
        this.canSell = canSell;
    }
}