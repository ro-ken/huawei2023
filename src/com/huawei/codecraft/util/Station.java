package com.huawei.codecraft.util;

import com.huawei.codecraft.Main;

import java.util.*;


/**
 * @Author: ro_kin
 * @Data:2023/3/13 19:47
 * @Description: TODO
 */
public class Station implements Comparable{
    public static final StationItem[] item;
    public static double emptyMinDistance; // 加速减速临界值 ,空载   ,速度 0 -> 6 ->0 全程距离
    public static double fullMinDistance; // 加速减速临界值 ，满载   ,速度 0 -> 6 ->0 全程距离

    public PriorityQueue<Pair> canSellStations;
    public Map<Integer,PriorityQueue<Pair>> canBuyStationsMap;  // Pair 存储的是
//    public Map<Integer,PriorityQueue<Pair>> canBuyStationsMap;
    public Station availNextStation;
    public Station closest89;
    public int fastestComposeFps;
    public int fastestComposeMoney;
    public int zoneId = 1;  // 联通区域编号

    public int id;
    public int type;   // 1-9
    public Point pos;

    public int leftTime;   // 剩余时间，帧数 -1：未生产 ； 0 阻塞；>0 剩余帧数
    public int rowStatus; // 原材料 二进制位表描述，例如 48(110000) 表示拥有物品 4 和 5
    public int proStatus; // 产品格 状态 0 无 1 有
    public boolean[] bookRow;    // 原料空格是否预定
    public boolean bookPro;      // 产品空格是否预定
    public int bookNum = 0; // 已经有多少机器人在往这个点赶
    public int bookNum2 = 0; // 专用

    // 流水线参数
    public double cycleAvgValue;    // 生产一个周期的平均价值  ,赚的钱数/ 花费的时间
    public boolean taskBook;    // 任务是否被预定，针对456
    public WaterFlow waterFlow; // 目前处于那一条流水线

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

    // 构造函数
    public Station(int id, int type, double x, double y) {
        this.id = id;
        this.type = type;
        pos = new Point(x,y);
        bookPro = false;
        bookRow = new boolean[8];
    }
    
    @Override
    public String toString() {
        return "Station{" +
                "Id=" + id +
                ", type=" + type +
                ", rowStatus=" + rowStatus +
                ", proStatus=" + proStatus +
                ", bookRow=" + Arrays.toString(bookRow) +
                ", bookPro=" + bookPro +
                '}';
    }

    //是否有某类型的货物
    public boolean bitJudge(int goodStatus,int type){
        return (goodStatus>>type & 1) == 1;
    }

    private double calc456CycleAvgValue(Point pos) {
        // 计算卖给9的钱和时间，算最佳
        double theoryMoney = calcEarnMoney(pos);   // 不算碰撞
        double allMoney = fastestComposeMoney + theoryMoney;
        double allFps = fastestComposeFps + calcGoBackDistanceToFps(pos);
        return allMoney/allFps;
    }

    private double calc7CycleAvgValue(Point pos) {
        // 计算卖给89的钱和时间，算最佳

        double allMoney = calcEarnMoney(pos);
        double allFps = calcGoBackDistanceToFps(pos);        // 时间金钱三部分，123-456-7-89
        for (PriorityQueue<Pair> queue : canBuyStationsMap.values()){
            Station st = queue.peek().key;  // 取价值最高的计算钱和fps
            allMoney += st.calcEarnMoney(pos);      // 456 - 7
            allFps += st.calcGoBackDistanceToFps(pos);

            allMoney += st.fastestComposeMoney ;// 123-456
            allFps += st.fastestComposeFps ;// 123-456
        }

        return allMoney/allFps;
    }
    
    public double calcEarnMoney(Point pos) {
        int baseMoney = Goods.item[type].earn;
        int fps1 = distanceToFps(false,pos);
        double theoryMoney = (baseMoney * Robot.calcTimeValue(fps1));   // 不算碰撞
        return theoryMoney;
    }

    private void calcFastestComposeFpsAndMoney() {
        // 要求每种原材料赚的最多的钱，也要求出fps
        fastestComposeFps = 0;
        fastestComposeMoney = 0;
        for (PriorityQueue<Pair> queue : canBuyStationsMap.values()){
            Station st = queue.peek().key;  // 取价值最高的计算钱和fps
            fastestComposeFps += st.calcGoBackDistanceToFps(pos);
            fastestComposeMoney += st.calcEarnMoney(pos);
        }
    }

    // 计算来回花费时间
    public int calcGoBackDistanceToFps(Point p){
        int go = distanceToFps(false,p);
        int back = distanceToFps(true,p);
        return go + back;
    }

    private static double calcMinDistance(boolean isEmpty) {
        double a = isEmpty ? Robot.emptyA:Robot.fullA;
        return Math.pow(Robot.maxSpeed,2)/(a);
    }

    public double calcSingleCycleAvgValue(Point other) {
        int baseMoney = Goods.item[type].earn;
        int fps1 = distanceToFps(false,other.x,other.y);
        int fps2 = distanceToFps(true,other.x,other.y);
        double theoryMoney = (baseMoney * Robot.calcTimeValue(fps1));   // 不算碰撞
        double cycleAvgValue = theoryMoney/(fps1 + fps2);     // 来回时间，先不算转向花费
        return cycleAvgValue;
    }
    
    public int calcValue(double x1,double y1,boolean isEmpty) {

        if (type>7) return 0;
        int baseMoney = Goods.item[type].earn;
        int fps = distanceToFps(isEmpty,x1,y1);
        int theoryMoney = (int) (baseMoney * Robot.calcTimeValue(fps));

        return theoryMoney;
    }

    // 是否有东西可以卖
    public boolean canSell() {
        return proStatus == 1 && !bookPro;
    }

    public boolean canBuy(int tp) {
        return !positionIsFull(tp) && !bookRow[tp];
    }

    private void calcCanBuyStations() {
        // 4-7节点
        canBuyStationsMap = new HashMap<>();
        for(int tp:item[type].call){
            PriorityQueue<Pair> queue = new PriorityQueue<>();
            canBuyStationsMap.put(tp,queue);
            ArrayList<Station> stations = Main.map.get(tp);
            for (Station st : stations) {
                double value = 0;
                if (type < 7){
                    // type = (4,5，6) //节点为123
                    value = calcGoBackDistanceToFps(st.pos);    //存储来回花费时间
                }else {
                    // type =  7节点为456，
                    // 分别计算 456map的时间，加上456 到本工作站的时间
                    for (PriorityQueue<Pair> p:st.canBuyStationsMap.values()){
                        value += p.peek().value;    // 选取最前面一个也就是最近的一个
                    }
                    value += calcGoBackDistanceToFps(st.pos);   // 加上本身来回时间
                }
                Pair pair = new Pair(st, value);    // 以时间为标准
                queue.add(pair);
            }
        }
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

    // 4567工作台使用
    public Station chooseAvailablePreStation(int type) {
        for(Pair pair : canBuyStationsMap.get(type)){
            Station oth = pair.getKey();
            if (!oth.bookRow[type] && !oth.positionIsFull(type)){
                availNextStation = oth;
                return oth;
            }
        }
        return null;
    }

    private Station chooseClosestStation(int type) {
        ArrayList<Station> stations = Main.map.get(type);
        if (stations.size() == 1) return stations.get(0);
         // 好几个选一个
        double minDis = 10000;
        Station minSta = null;
        for(Station st:stations){
            double dis = pos.calcDistance(st.pos);
            if (dis < minDis){
                minDis = dis;
                minSta = st;
            }
        }
        return minSta;
    }
    
    @Override
    public int compareTo(Object o) {
        Station st = (Station) o;
        return Double.compare(st.cycleAvgValue,cycleAvgValue);
    }

    public void clearPosition(int type){
        rowStatus = rowStatus & (~(1<<type));
    }

    // 距离换算成时间, 0 -> v -> 0
    public double distanceToSecond(boolean isEmpty, double ox,double oy){
        //两种情况， 加速，匀速，减速  or  加速 ，减速
        double minDistance = isEmpty?emptyMinDistance:fullMinDistance;
        double a = isEmpty ? Robot.emptyA:Robot.fullA;
        double distance = pos.calcDistance(ox,oy);
        double second ;
        if (distance <= minDistance){
            second = Math.sqrt(distance/a)*2;   // t = sqrt(2*d/2 /a) * 2
        }else {
            second = Math.sqrt(minDistance/a)*2 + (distance-minDistance)/a;
        }
        return second;
    }

    // 距离换算成帧数
    public int distanceToFps(boolean isEmpty, double ox,double oy){
        double second = distanceToSecond(isEmpty,ox,oy);
        int fps = (int) (second * 50);
        return fps;
    }

    // 距离换算成帧数
    public int distanceToFps(boolean isEmpty, Point p){
        return distanceToFps(isEmpty,p.x,p.y);
    }

    // 返回当前的空位，并且没有被预定的
    public ArrayList<Integer> getEmptyRaw() {
        ArrayList<Integer> empty = new ArrayList<>();
        for (int tp : item[type].call) {
            if (!bookRow[tp] && !positionIsFull(tp)){
                empty.add(tp);
            }
        }
        return empty;
    }

    public double getSafeDis() {
        double dis1 = Robot.stationSafeDisCoef * Robot.fullRadius;
        double dis2 = 0;
        if (type <=3){
            dis2 = emptyMinDistance/2;
        }else {
            dis2 = fullMinDistance/2;
        }
        return dis1 + dis2;
    }

    public boolean haveEmptyPosition() {
        for (int tp : item[type].call) {
            if (canBuy(tp)){
                return true;
            }
        }
        return false;
    }

    public void initialization() {
        // 初始化 1 ，给每个生产型节点设置售出节点的优先级队列
        canSellStations = new PriorityQueue<>();
        if (type<=7){
            int[] canSell = item[type].canSell;
            for(int tp:canSell){
                if(!Main.map.containsKey(tp)){
                    continue;
                }
                ArrayList<Station> stations = Main.map.get(tp);
                for (Station st : stations) {
//                    double value = calcValue(st.pos.x, st.pos.y, false);
                    double value = distanceToFps(false,st.pos.x,st.pos.y);  //以时间排序
                    Pair pair = new Pair(st, value);
                    canSellStations.add(pair);
                }
            }
        }
    }

    public void initialization2() {
        // 初始化 2 ，先456，在7
        // 计算任务456的原料供应station排序
        // 计算任务456的最近123的时间
        // 若有9，计算456的流水线价值
        // 若有7，计算7号任务原料供应station排序，以及流水线价值
        if (type <= 3 && Main.have9){
            closest89 = chooseClosestStation(9);
            cycleAvgValue = calcSingleCycleAvgValue(closest89.pos);
        }

        if (type>=4 && type <=6){
            calcCanBuyStations();
            calcFastestComposeFpsAndMoney();
            if (Main.have9){
                closest89 = chooseClosestStation(9);
                cycleAvgValue = calc456CycleAvgValue(closest89.pos);
            }
        }
        if (type == 7){
            calcCanBuyStations();
            closest89 = canSellStations.peek().getKey();
            cycleAvgValue = calc7CycleAvgValue(closest89.pos);
        }
    }

    public boolean positionFull() {
        for (int tp : item[type].call) {
            if (!positionIsFull(tp)){
                return false;
            }
        }
        return true;
    }

    public boolean positionIsFull(int type){
        return bitJudge(rowStatus,type);
    }

    
    // 原料格没人预定
    public boolean positionNoBook() {
        for (int tp : item[type].call) {
            if (bookRow[tp]){
                return false;
            }
        }
        return true;
    }

    public void setPosition(int type){
        rowStatus = rowStatus | (1<<type);
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