package com.huawei.codecraft;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

import com.huawei.codecraft.core.*;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.way.Pos;


public class Main {

    private static final Scanner inStream = new Scanner(System.in);
    private static final PrintStream outStream = new PrintStream(new BufferedOutputStream(System.out));
    private static PrintStream log = null;
    public static int frameID=0;
//    public static MyThread thread;

    public static final Robot[] robots = new Robot[4];
    public static final Station[] stations = new Station[50];
    public static final Map<Integer, ArrayList<Station>> stationsMap = new HashMap<>(); // 类型，以及对应的工作站集合
    public static final Map<Point, Station> pointStaMap = new HashMap<>(); // 坐标工作站map
    public static final int[][] wallMap = new int[100][100];   // 存储墙的地图
    public static final double unreachableCost = 1000000000;    // 不可达费用
    public static final double unreachableJudgeCost = 1000000;    // 代价超过这个数就不能送了
    public static Map<Integer, Pos> robotPos = new HashMap<>();  // 记录起始机器人的位置
    public static Map<Integer,Zone> zoneMap = new HashMap<>();
    public static Mapinfo mapinfo;
    public static int stationNum = 0;
    public static final int duration = 5 * 60 * 50;
    public static final int JudgeDuration = duration - 20 * 50;    //最后20s需判断买入的商品能否卖出
    public static final int JudgeDuration2 = duration - 20 * 50;    //最后20s需判断选择是否最佳，是否还有商品没有卖
    public static final int fps = 50;
    public static final boolean test = true;    // 是否可写入
    public static final int robotNum = 4;
    public static int mapSeq;   // 是第几号地图，做优化
    public static boolean specialMapMode = false;   // 是否针对地图做优化
    public static ArrayList<WaterFlow> waterFlows = new ArrayList<>();  // 生产流水线
    public static int[] clockCoef = new int[]{1, 1, 1, 1}; // 碰撞旋转系数

    public static void main(String[] args) throws FileNotFoundException {

        if (test){
            log = new PrintStream("./log.txt");
            System.setOut(log);//把创建的打印输出流赋给系统。即系统下次向 ps输出
//            printLog("这行语句将会被写到log.txt文件中");
        }

        schedule();
    }

    private static void schedule() {
        initialization();
        Ok();
        while (inStream.hasNextLine()) {
            String line = inStream.nextLine();
            String[] parts = line.split(" ");
            frameID = Integer.parseInt(parts[0]);
            printLog(frameID);
            readUtilOK();
            Frame(frameID);
            handleFrame();
            Ok();
        }
    }

    // 核心代码，分析如何运动
      private static void handleFrame() {

            // 先计算每个机器人的参数，后面好用
          for (int i = 0; i < robotNum; i++) {
//              if (i!= 3) continue;

              if (robots[i].nextStation == null){
                  robots[i].selectBestStation();
              }
              if (robots[i].nextStation == null) continue;
              Main.printLog("pos:next:"+robots[i].pos + "," + robots[i].route.next);
              if (robots[i].blockDetect()){
                  // 若发生阻塞，需要重新规划路线
                  robots[i].setNewPath();
              }
              if (robots[i].route.arriveNext()){
                  robots[i].route.updateNext();
              }

              robots[i].route.calcParamEveryFrame();    // 通用参数
              robots[i].calcMoveEquation();     //  运动方程
          }

        for (int i = 0; i < robotNum; i++) {
//            if (i!= 3) continue;
            if (robots[i].nextStation == null) continue;
            if (robots[i].isArrive()){
//                    Main.printLog("arrive");
                // 有物品就买，没有就等待,逐帧判断
                if (robots[i].nextStation == robots[i].srcStation && robots[i].nextStation.proStatus == 1){
                    if (frameID > JudgeDuration){
                        if (!robots[i].canBugJudge()){
                            continue;
                        }
                    }
                    Buy(i);
                    robots[i].srcStation.bookPro = false;       //解除预定
                    robots[i].srcStation.bookNum--;       //
                    robots[i].destStation.bookNum++;       //
                    printLog("buy");
                    robots[i].changeTarget();
                    // 有物品就卖，没有就等待,逐帧判断
                } else if (robots[i].nextStation == robots[i].destStation && !robots[i].nextStation.positionIsFull(robots[i].carry)){
                    Sell(i);
                    robots[i].destStation.bookRow[robots[i].srcStation.type] = false;   //解除预定
                    robots[i].destStation.setPosition(robots[i].srcStation.type);       // 卖了以后对应物品空格置1
//                        bookRow[robots[i].srcStation.type] = false;   //解除预定
                    robots[i].destStation.bookNum--;       //解除预定
                    printLog("sell");
                    robots[i].changeTarget();
                }
            }
            // 如果到中间点要换下一个点
            robots[i].rush();
        }
    }

    private static void initialization() {
        long t1 = System.currentTimeMillis();

        initMap();      //  初始化地图
        initZone();     //  初始化区域
        initMapSeq();      // 初始化地图序列
        initSpecialMapParam();    // 初始化地图序列参数
        initStations();     // 初始化工作站
        initWaterFlow();    // 初始化流水线

        long t2 = System.currentTimeMillis();
        printLog("init time = " + (t2 - t1) + "ms");
    }

    private static void initZone() {
        mapinfo = new Mapinfo(wallMap);
        mapinfo.setZone(zoneMap);
        printLog(mapinfo);
        printLog(zoneMap);
    }

    private static void initStations() {
        for (int i = 0; i < stationNum; i++) {
            stations[i].initialization();           // 第一次初始化，能卖给哪些节点
        }
        // 先初始化123，在456，在7
        for (int i = 1; i <= 7; i++) {
            if (stationsMap.containsKey(i)){
                ArrayList<Station> stations = stationsMap.get(i);
                for (Station st : stations){
                    st.initialization2();
                }
            }
        }
    }

    private static void initMap() {
        String line;
        int row = 101;
        int stationId = 0;
        int robotId= 0;
        while (inStream.hasNextLine()) {
            row --;
            double y = row * 0.5 - 0.25;
            line = inStream.nextLine();
            if ("OK".equals(line)) {
                stationNum = stationId;
                for (Integer key: stationsMap.keySet()){
                    printLog("type = " + key + " , nums = " +stationsMap.get(key).size());
                }
                printLog("total = "+ stationNum);

                return ;
            }

            for (int i=0;i<100;i++){
                double x = i * 0.5 + 0.25;
                char c = line.charAt(i);
                if (c == '.') {
                    wallMap[100-row][i] = -1;    // 给地图赋值
                    continue;
                }
                if (c == '#') {
                    wallMap[100-row][i] = -2;    // 给地图赋值
                    continue;
                }
                if (c == 'A'){
                    wallMap[100-row][i] = robotId + 100;    // 给地图赋值
                    robots[robotId] = new Robot(robotId,x,y,robotId);
                    robotPos.put(robotId + 100, new Pos(100-row, i));
                    robotId++;
                }else {
                    wallMap[100-row][i] = stationId;    // 给地图赋值
                    int type = Character.getNumericValue(c);
                    Station station = new Station(stationId,type,x,y);
                    stations[stationId] = station;
                    if (!stationsMap.containsKey(type)){
                        ArrayList<Station> list = new ArrayList<>();
                        list.add(station);
                        stationsMap.put(type,list);
                    }else {
                        stationsMap.get(type).add(station);
                    }
                    stationId ++;
                }
            }
        }
    }

    // 初始化地图顺序
    private static void initMapSeq() {
        if (stations[0].type == 1){
            mapSeq = 1;
        }else if (stations[0].type == 6){
            mapSeq = 2;
        }else if (stations[0].type == 3){
            mapSeq = 3;
        }else if (stations[0].type == 7){
            mapSeq = 4;
        }else {
            mapSeq = -1;    // 未初始化
        }
        Main.printLog("mapSeq:"+mapSeq);
    }

    public static void initSpecialMapParam() {
//        if (mapSeq == 1) {
//            Route.emergencyAngle = Robot.pi/10;
//        }
//        if (mapSeq == 2) {
//            Route.emergencyDistanceCoef = 0.6;
//        }
//        if (mapSeq == 3) {
//            Route.perceptionAngleRange = Robot.pi/10;
//        }
//        if (mapSeq == 4) {
//            Route.lineSpeedCoef = 1.5;
//        }
    }



    // 选择最有价值的生产流水线投入生产，明确一条流水线有哪些节点
    private static void initWaterFlow() {
        for (Zone zone : zoneMap.values()) {
            if (zone.stationsMap.containsKey(7)){
                // 最多开2条流水线
                ArrayList<Station> sts = zone.stationsMap.get(7);
                if(sts.size() == 1){
                    WaterFlow flow = new WaterFlow(sts.get(0),zone);
                    flow.assignRobot(4);//分配4个
                    waterFlows.add(flow);
                }else {
                    Collections.sort(sts);
                    for (int i=0;i<2;i++){
                        WaterFlow flow = new WaterFlow(sts.get(i),zone);
                        flow.assignRobot(2);    // 每条流水线两个机器人   todo 可尝试更换策略
                        waterFlows.add(flow);
                    }
                }
                for (Station st : sts) {
                    printLog("id" + st.id + " value fps" + st.cycleAvgValue);
                }
                printLog(sts);

            }else {
                // 最多选择4条流水线
                ArrayList<Station> sts = zone.getStations();
                Collections.sort(sts);
                for (int i = 0; i < sts.size(); i++) {
                    printLog(sts.get(i)+ ":" + sts.get(i).cycleAvgValue);
                }
                for (int i = 0; i < zone.robots.size(); i++) {
                    if (sts.size() > i && sts.get(i).cycleAvgValue>0){    // 没有那么多工作站，不分了
                        WaterFlow flow = new WaterFlow(sts.get(i),zone);
                        flow.assignRobot(1);    // 一个机器人负责一个
                        waterFlows.add(flow);
                    }
                }
            }
        }
        printLog(waterFlows);
    }

    private static void readUtilOK() {
        String line;
        line = inStream.nextLine(); // stationNum
//        printLog(line);
        assert stationNum == Integer.parseInt(line);
        int stationId = -1;
        while (inStream.hasNextLine()) {
            stationId ++;
            line = inStream.nextLine();
//            printLog(line);
            if ("OK".equals(line)) {
                return;
            }
            String[] parts = line.split(" ");
            if (stationId < stationNum){
                Station station = stations[stationId];
                station.type = Integer.parseInt(parts[0]);
                station.leftTime = Integer.parseInt(parts[3]);
                station.rowStatus = Integer.parseInt(parts[4]);
                station.proStatus = Integer.parseInt(parts[5]);
            }else{
                Robot robot = robots[stationId-stationNum];
                robot.StationId = Integer.parseInt(parts[0]);
                robot.carry = Integer.parseInt(parts[1]);
                robot.timeValue = Double.parseDouble(parts[2]);
                robot.bumpValue = Double.parseDouble(parts[3]);
                robot.angV = Double.parseDouble(parts[4]);
                robot.lineVx = Double.parseDouble(parts[5]);
                robot.lineVy = Double.parseDouble(parts[6]);
                robot.turn = Double.parseDouble(parts[7]);
                robot.pos.x = Double.parseDouble(parts[8]);
                robot.pos.y = Double.parseDouble(parts[9]);
            }
        }
    }

    public static void Sell(int robotId){
        outStream.printf("sell %d\n", robotId);
    }
    public static void Buy(int robotId){
        outStream.printf("buy %d\n", robotId);
    }
    public static void Forward(int robotId,int speed){
        outStream.printf("forward %d %d\n", robotId, speed);
    }
    public static void Forward(int robotId,double speed){
        outStream.printf("forward %d %f\n", robotId, speed);
    }

    public static void Rotate(int robotId,double angleSpeed){
        outStream.printf("rotate %d %f\n", robotId, angleSpeed);
    }
    public static void Destroy(int robotId){
        outStream.printf("destroy %d\n", robotId);
    }
    public static void Ok(){
        outStream.print("OK\n");
        outStream.flush();
    }
    public static void Frame(int frameID){
        outStream.printf("%d\n", frameID);
    }
    public static void printLog(Object log){
        if (test){
            System.out.println(log);
        }
    }

}































