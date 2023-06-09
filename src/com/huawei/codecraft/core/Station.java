package com.huawei.codecraft.core;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.*;

import java.util.*;


/**
 * ClassName: Station
 * Package: com.huawei.codecraft.core
 * Description: 工作台
 *
 * @Author: ro_kin
 * @Data:2023/3/13 19:47
 */
public class Station implements Comparable {
    public static final StationItem[] item;
    public static double emptyMinDistance;  // 加速减速临界值 ,空载   ,速度 0 -> 6 ->0 全程距离
    public static double fullMinDistance;  // 加速减速临界值 ，满载   ,速度 0 -> 6 ->0 全程距离

    public PriorityQueue<Pair> canSellStations = new PriorityQueue<>();
    public Map<Integer, PriorityQueue<Pair>> canBuyStationsMap = new HashMap<>();  // Pair 存储的是 到其他station的fps

    public Station availNextStation;
    public Station closest89;
    public int fastestComposeFps;
    public int fastestComposeMoney;
    public Zone zone;  //所属的区域
    public Path paths;  //存储路径信息
    public int id;
    public int type;  // 1-9
    public Point pos;

    public int leftTime;  // 剩余时间，帧数 -1：未生产 ； 0 阻塞；>0 剩余帧数
    public int rowStatus;  // 原材料 二进制位表描述，例如 48(110000) 表示拥有物品 4 和 5
    public int proStatus;  // 产品格 状态 0 无 1 有
    public boolean[] bookRow;  // 原料空格是否预定
    public int[] bookRawNum;  // 原料被预定的个数
    public boolean bookPro;  // 产品空格是否预定

    public int bookNum = 0;  // 已经有多少机器人在往这个点赶
    public StationStatus place = StationStatus.EMPTY;  // 工作台位置的状态，是否被其他机器人占用

    // 流水线参数
    public double cycleAvgValue;  // 生产一个周期的平均价值  ,赚的钱数/ 花费的时间

    static {
        item = new StationItem[10];
        item[1] = new StationItem(1, new int[]{}, 50, new int[]{4, 5, 9});
        item[2] = new StationItem(2, new int[]{}, 50, new int[]{4, 6, 9});
        item[3] = new StationItem(3, new int[]{}, 50, new int[]{5, 6, 9});
        item[4] = new StationItem(4, new int[]{1, 2}, 500, new int[]{7, 9});
        item[5] = new StationItem(5, new int[]{1, 3}, 500, new int[]{7, 9});
        item[6] = new StationItem(6, new int[]{2, 3}, 500, new int[]{7, 9});
        item[7] = new StationItem(7, new int[]{4, 5, 6}, 1000, new int[]{8, 9});
        item[8] = new StationItem(8, new int[]{7}, 1, new int[]{});
        item[9] = new StationItem(9, new int[]{1, 2, 3, 4, 5, 6, 7}, 1, new int[]{});

    }


    /**
     * 构造函数
     *
     * @param id   id
     * @param type 类型
     * @param x    x坐标
     * @param y    y坐标
     */
    public Station(int id, int type, double x, double y) {
        this.id = id;
        this.type = type;
        pos = new Point(x, y);
        Main.pointStaMap.put(pos, this); // 键值对赋值
        bookPro = false;
        bookRow = new boolean[8];
        bookRawNum = new int[8];
        paths = new Path(pos);
        if (type > 3) {
            leftTime = -1;  // 初始化，马上要用
        }


        emptyMinDistance = calcMinDistance(true);
        fullMinDistance = calcMinDistance(false);
    }

    @Override
    public String toString() {
        return "[" +
                "sId=" + id +
                ", type =" + type +
                ", pos =" + pos +
                ']';
    }


    /**
     * 判断是否有某类型的货物
     *
     * @param goodStatus 货物状态
     * @param type       类型
     * @return 是否有某类型的货物
     */
    public boolean bitJudge(int goodStatus, int type) {
        return (goodStatus >> type & 1) == 1;
    }

    /**
     * 计算卖给9的钱和时间，算最佳
     *
     * @param pos 点位置
     * @return 平均价值
     */
    private double calc456CycleAvgValue(Point pos) {
        double theoryMoney = calcEarnMoney(pos);   // 不算碰撞
        double allMoney = fastestComposeMoney + theoryMoney;
        double allFps = fastestComposeFps + calcGoBackDistanceToFps(pos);
        return allMoney / allFps;
    }

    /**
     * 计算卖给89的钱和时间，算最佳
     *
     * @param pos 点位置
     * @return 平均价值
     */
    private double calc7CycleAvgValue(Point pos) {
        double allMoney = calcEarnMoney(pos);
        double allFps = calcGoBackDistanceToFps(pos);        // 时间金钱三部分，123-456-7-89
        for (PriorityQueue<Pair> queue : canBuyStationsMap.values()) {
            if (queue.size() > 0) {
                Station st = Objects.requireNonNull(queue.peek()).key;  // 取价值最高的计算钱和fps
                allMoney += st.calcEarnMoney(pos);      // 456 - 7
                allFps += st.calcGoBackDistanceToFps(pos);
                allMoney += st.fastestComposeMoney;// 123-456
                allFps += st.fastestComposeFps;// 123-456
            } else {
                return 0;
            }
        }

        return allMoney / allFps;
    }

    /**
     * 计算所赚的钱
     *
     * @param pos 点位置
     * @return 所赚的钱
     */
    public double calcEarnMoney(Point pos) {
        int baseMoney = Goods.item[type].earn;
        int fps1 = pathToFps(false, pos);
        double theoryMoney = (baseMoney * Robot.calcTimeValue(fps1));   // 不算碰撞
        return theoryMoney;
    }

    /**
     * 要求每种原材料赚的最多的钱，也要求出fps
     */
    private void calcFastestComposeFpsAndMoney() {
        fastestComposeFps = 0;
        fastestComposeMoney = 0;
        for (PriorityQueue<Pair> queue : canBuyStationsMap.values()) {
            if (queue.size() > 0) {
                Station st = queue.peek().key;  // 取价值最高的计算钱和fps
                fastestComposeFps += st.calcGoBackDistanceToFps(pos);
                fastestComposeMoney += st.calcEarnMoney(pos);
            } else {
                fastestComposeFps += Main.unreachableCost;
            }
        }
    }

    /**
     * 计算来回花费时间
     *
     * @param p 点信息
     * @return 来回花费时间
     */
    public int calcGoBackDistanceToFps(Point p) {
        int go = pathToFps(false, p);
        int back = pathToFps(true, p);
        return go + back;
    }

    /**
     * 计算最短距离
     *
     * @param isEmpty 是否为空
     * @return 最短距离
     */
    private double calcMinDistance(boolean isEmpty) {
        double a = isEmpty ? Robot.emptyA : Robot.fullA;
        return Math.pow(Robot.getMaxSpeed(), 2) / (a);
    }

    /**
     * 计算单条流水线平均价值
     *
     * @param other 其他点
     * @return 单条流水线平均价值
     */
    public double calcSingleCycleAvgValue(Point other) {
        int baseMoney = Goods.item[type].earn;
        int fps1 = pathToFps(false, other);
        int fps2 = pathToFps(true, other);
        double theoryMoney = (baseMoney * Robot.calcTimeValue(fps1));   // 不算碰撞
        double cycleAvgValue = theoryMoney / (fps1 + fps2);  // 来回时间，先不算转向花费
        return cycleAvgValue;
    }


    /**
     * 判断是否有东西可以卖
     *
     * @return 是否有东西可以卖
     */
    public boolean canSell() {
        return proStatus == 1 && !bookPro;
    }

    /**
     * 判断是否有东西可以买
     *
     * @param tp 类型
     * @return 是否有东西可以买
     */
    public boolean canBuy(int tp) {
        return !positionIsFull(tp) && !bookRow[tp];
    }


    /**
     * 计算可以购买的数量
     *
     * @return 可以购买的数量
     */
    public int canBuyNum() {
        int num = 0;
        for (int tp : getRaws()) {
            if (canBuy(tp)) {
                num++;
            }
        }
        return num;
    }

    /**
     * 计算可以购买的原料
     *
     * @return 可以购买的原料
     */
    public ArrayList<Integer> canBuyRaws() {
        ArrayList<Integer> raws = new ArrayList<>();
        for (int tp : getRaws()) {
            if (canBuy(tp)) {
                raws.add(tp);
            }
        }
        return raws;
    }

    /**
     * 计算可以买的工作站
     */
    private void calcCanBuyStations() {
        // 4-7节点
        for (int tp : getRaws()) {
            PriorityQueue<Pair> queue = new PriorityQueue<>();
            canBuyStationsMap.put(tp, queue);
            ArrayList<Station> stations = zone.stationsMap.get(tp);

            if (stations == null) continue;

            for (Station st : stations) {
                double value = 0;
                if (type < 7) {
                    // type = (4,5，6) //节点为123
                    value = calcGoBackDistanceToFps(st.pos);    //存储来回花费时间
                } else {
                    // type =  7节点为456，
                    // 分别计算 456map的时间，加上456 到本工作站的时间
                    for (PriorityQueue<Pair> p : st.canBuyStationsMap.values()) {
                        if (p.size() > 0) {
                            value += p.peek().value;    // 选取最前面一个也就是最近的一个
                        } else {
                            value += Main.unreachableCost;
                        }
                    }
                    value += calcGoBackDistanceToFps(st.pos);   // 加上本身来回时间
                }
                if (value < Main.unreachableJudgeCost) {
                    Pair pair = new Pair(st, value);    // 以时间为标准
                    queue.add(pair);
                }
            }
        }
    }


    /**
     * 选择下一个可用的工作台
     * 没有可用返回 null
     *
     * @return 工作台
     */
    public Station chooseAvailableNextStation() {

        Station res = null;
        double minFps = 100000;

        for (Pair pair : canSellStations) {
            Station oth = pair.getKey();
            if (oth.canBuy(type) && oth.place != StationStatus.BLOCK) {

                double fps = pair.value;
                if (oth.place == StationStatus.CANBUMP) {   // 周围有机器人，加上权重
                    fps += oth.type == 7 ? Robot.destBumpCost : Robot.dest456BumpCost;
                }
                if (fps < minFps) {
                    minFps = fps;
                    res = oth;
                }
            }
        }

        availNextStation = res;
        return res;
    }


    /**
     * 选择上一个可用的工作台
     * 4567工作台使用
     *
     * @param type 类型
     * @return 工作台
     */
    public Station chooseAvailablePreStation(int type) {
        for (Pair pair : canBuyStationsMap.get(type)) {
            Station oth = pair.getKey();
            if (!oth.bookRow[type] && !oth.positionIsFull(type)) {
                availNextStation = oth;
                return oth;
            }
        }
        return null;
    }

    /**
     * 选择最近的工作台
     *
     * @param type 类型
     * @return 工作台
     */
    private Station chooseClosestStation(int type) {
        ArrayList<Station> stations = zone.stationsMap.get(type);
        if (stations.size() == 1) return stations.get(0);
        // 好几个选一个
        double minFps = 1000000;
        Station minSta = null;
        for (Station st : stations) {
            double fps = pathToFps(false, st.pos);
            if (fps > Main.unreachableJudgeCost) continue;
            if (fps < minFps) {
                minFps = fps;
                minSta = st;
            }
        }
        return minSta;
    }

    @Override
    public int compareTo(Object o) {
        Station st = (Station) o;
        return Double.compare(st.cycleAvgValue, cycleAvgValue);
    }

    /**
     * 清除位置
     *
     * @param type 类型
     */
    public void clearPosition(int type) {
        rowStatus = rowStatus & (~(1 << type));
    }

    /**
     * 计算fps
     *
     * @param isEmpty 是否为空
     * @param dest    终点
     * @return 所用fps
     */
    public int pathToFps(boolean isEmpty, Point dest) {
        return paths.getPathFps(isEmpty, dest);
    }


    /**
     * 返回当前的空位，并且没有被预定的
     *
     * @return 空位工作台
     */
    public ArrayList<Integer> getEmptyRaw() {
        ArrayList<Integer> empty = new ArrayList<>();
        for (int tp : getRaws()) {
            if (canBuy(tp)) {
                empty.add(tp);
            }
        }
        return empty;
    }

    /**
     * 计算安全距离
     *
     * @return 安全距离
     */
    public double getSafeDis() {
        double dis1 = Robot.stationSafeDisCoef * Robot.fullRadius;
        double dis2 = 0;
        if (type <= 3) {
            dis2 = emptyMinDistance / 2;
        } else {
            dis2 = fullMinDistance / 2;
        }
        return dis1 + dis2;
    }

    /**
     * 判断是否有空位
     *
     * @return 是否有空位
     */
    public boolean haveEmptyPosition() {
        for (int tp : item[type].call) {
            if (canBuy(tp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否有空位
     *
     * @return 是否有空位
     */
    public boolean haveEmptyPositionLast() {
        for (int tp : item[type].call) {
            if (canBuy(tp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 敌方工作台初始化
     */
    public void fighterStationInitialization() {
        // 周围没有机器人，取消初始化
        if (zone == null) {
            return;
        }

        // 初始化 1 ，给每个生产型节点设置售出节点的优先级队列
        if (type <= 7) {
            int[] canSell = item[type].canSell;
            for (int tp : canSell) {
                if (!zone.fighterStationsMap.containsKey(tp)) {
                    continue;
                }
                //                ArrayList<Station> stations = Main.stationsMap.get(tp);
                for (Station st : zone.fighterStationsMap.get(tp)) {
                    //                    Main.printLog(this+" : "+st);
                    double value = pathToFps(false, st.pos);  //以时间排序
                    if (value < Main.unreachableJudgeCost) {
                        Pair pair = new Pair(st, value);
                        canSellStations.add(pair);
                    }
                }
            }
        }
    }

    /**
     * 工作台的第一次初始化，判断工作台能卖给哪些机器人
     */
    public void initialization() {

        // 周围没有机器人，取消初始化
        if (zone == null) {
            return;
        }

        // 初始化 1 ，给每个生产型节点设置售出节点的优先级队列
        if (type <= 7) {
            int[] canSell = item[type].canSell;
            for (int tp : canSell) {
                if (!zone.stationsMap.containsKey(tp)) {
                    continue;
                }
//                ArrayList<Station> stations = Main.stationsMap.get(tp);
                for (Station st : zone.stationsMap.get(tp)) {
//                    Main.printLog(this+" : "+st);
                    double value = pathToFps(false, st.pos);  //以时间排序
                    if (value < Main.unreachableJudgeCost) {
                        Pair pair = new Pair(st, value);
                        canSellStations.add(pair);
                    }
                }
            }
        }
    }

    /**
     * 初始化2
     */
    public void initialization2() {

        // 周围没有机器人，取消初始化
        if (zone == null) {
            return;
        }

        // 初始化 2 ，先456，在7
        // 计算任务456的原料供应station排序
        // 计算任务456的最近123的时间
        // 若有9，计算456的流水线价值
        // 若有7，计算7号任务原料供应station排序，以及流水线价值
        if (type <= 3 && zone.stationsMap.containsKey(9)) {
            closest89 = chooseClosestStation(9);
            if (closest89 != null) {
                cycleAvgValue = calcSingleCycleAvgValue(closest89.pos);
            } else {
                cycleAvgValue = 0;  // 没有价值
            }
        }

        if (type >= 4 && type <= 6) {
            calcCanBuyStations();
            calcFastestComposeFpsAndMoney();
            if (zone.stationsMap.containsKey(9)) {
                closest89 = chooseClosestStation(9);
                if (closest89 != null) {
                    cycleAvgValue = calc456CycleAvgValue(closest89.pos);
                } else {
                    cycleAvgValue = 0;  // 没有价值
                }
            }
        }
        if (type == 7) {
            calcCanBuyStations();
            if (canSellStations.peek() != null) {
                closest89 = Objects.requireNonNull(canSellStations.peek()).key;
            }
            if (closest89 != null) {
                cycleAvgValue = calc7CycleAvgValue(closest89.pos);
            } else {
                cycleAvgValue = 0;  // 没有价值
            }
        }
    }

    /**
     * 判断是否为满
     *
     * @return 是否为满
     */
    public boolean positionFull() {
        for (int tp : item[type].call) {
            if (!positionIsFull(tp)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否为满
     *
     * @param type 类型
     * @return 是否为满
     */
    public boolean positionIsFull(int type) {
        return bitJudge(rowStatus, type);
    }


    /**
     * 判断原料格没人预定
     *
     * @return 原料格没人预定
     */
    public boolean positionNoBook() {
        for (int tp : item[type].call) {
            if (bookRow[tp]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 设置位置信息
     *
     * @param type 类型
     */
    public void setPosition(int type) {
        rowStatus = rowStatus | (1 << type);
    }


    /**
     * 返回所有原料
     *
     * @return 所有原料
     */
    public int[] getRaws() {
        return item[type].call;
    }

    /**
     * 判断预定的数量
     *
     * @return 预定的数量
     */
    public int bookRawNum() {
        int i = 0;
        for (int tp : item[type].call) {
            if (bookRow[tp]) {
                i++;
            }
        }
        return i;
    }

    /**
     * 计算预定的总数
     *
     * @return 预定的总数
     */
    public int getBookRawNum() {
        int i = 0;
        for (int tp : item[type].call) {
            i += bookRawNum[tp];
        }
        return i;
    }

    /**
     * 周围有敌方机器人，要改变状态
     *
     * @param rp 雷达信息
     */
    public void changeStatus(RadarPoint rp) {
        // 每个st改标志位
        Point point = rp.getPoint();
        if (!pos.inCorner()) {
            // 不在墙角，如果有多个机器人比较近，认为不可撞开
            place = StationStatus.CANBUMP;  // 先设为可撞开
            for (RadarPoint rp1 : Main.curEnemys) {
                if (rp == rp1) continue;
                double dis = rp1.getPoint().calcDistance(point);
                if (dis < 3) {
                    // 有两个机器人较近，变为阻塞
                    place = StationStatus.BLOCK;
                }
            }
        } else {
            // 如果在角落，就认为是不可撞开的
            place = StationStatus.BLOCK;
        }
    }
}

