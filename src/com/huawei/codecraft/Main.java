package com.huawei.codecraft;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

import com.huawei.codecraft.core.*;
import com.huawei.codecraft.util.*;
import com.huawei.codecraft.way.Astar;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.way.Pos;


public class Main {

    private static final Scanner inStream = new Scanner(System.in);
    private static final PrintStream outStream = new PrintStream(new BufferedOutputStream(System.out));
    private static PrintStream log = null;
    private static PrintStream bluelog = null;
    private static PrintStream redlog = null;
    private static PrintStream path = null;
    public static int frameID=0;
//    public static MyThread thread;

    public static boolean isBlue = false;           // 是红方还是蓝方 true 蓝方，false 红方
    public static final Robot[] robots = new Robot[4];
    public static final Station[] stations =  new Station[50];
    public static final Station[] fighterStations =  new Station[50];
    public static final Station[] stationsBlue = new Station[50];   // 记录对手得工作台
    public static final Station[] stationsRed = new Station[50];   // 记录对手得工作台
    public static  Map<Integer, ArrayList<Station>> stationsMap; // 类型，以及对应的工作站集合
    public static  Map<Integer, ArrayList<Station>> fighterStationsMap; // 对手的类型，以及对应的工作站集合
    public static final Map<Integer, ArrayList<Station>> stationsMapBlue = new HashMap<>(); // 类型，以及对应的工作站集合
    public static final Map<Integer, ArrayList<Station>> stationsMapRed = new HashMap<>(); // 类型，以及对应的工作站集合
    public static final Map<Point, Station> pointStaMap = new HashMap<>(); // 坐标工作站map
    public static final int[][] wallMap = new int[100][100];   // 存储墙的地图
    public static final double unreachableCost = 1000000000;    // 不可达费用
    public static final double unreachableJudgeCost = 1000000;    // 代价超过这个数就不能送了
    public static Map<Integer, Pos> robotPos = new HashMap<>();  // 记录起始机器人的位置
    public static Map<Integer,Zone> zoneMap = new HashMap<>();
    public static Mapinfo mapinfo;
    public static int stationNum = 0;
    public static int fighterStationNum = 0;
    public static final int fighterStationNumStart = 50;
    public static final int duration = 4 * 60 * 50;     // 比赛时长
    public static final int JudgeDuration = duration - 30 * 50;    //最后20s需判断买入的商品能否卖出
    public static final int AttackJudgeDuration = duration - 10 * 50;    //最后20s需判断买入的商品能否卖出
    public static final int fps = 50;
    public static final boolean test = true;    // 是否可写入
    public static final boolean writePath = false;    // 是否可写入
    public static final boolean slowMode = false;   // 等待模式，让每帧时间尽可能长，留出时间给后台处理
    public static final int robotNum = 4;
    public static int mapSeq = 0;   // 地图序号
    public static final int attackRobotNum = 1;     // 设置的进攻机器人的个数
    public static final HashSet<Integer> testRobot = new HashSet<>();
    public static final HashSet<Station> blockStations = new HashSet<>();   // 附近有敌方机器人的工作站
    public static ArrayList<WaterFlow> waterFlows = new ArrayList<>();  // 生产流水线
    public static int[] clockCoef = new int[]{0,0,0,0}; // 碰撞旋转系数
    public static LimitedQueue<HashSet<RadarPoint>> enemysQueue = new LimitedQueue<>(5);    // 保存前10帧敌方机器人的位置
    public static HashSet<RadarPoint> curEnemys = new HashSet<>();    // 记录当前帧机器人的位置

    public static void main(String[] args) throws FileNotFoundException {

        if (test){
            if (writePath){
                path = new PrintStream("./4.txt");
            }
            log = new PrintStream("./log.txt");
            redlog = new PrintStream("./redlog.txt");
            bluelog = new PrintStream("./bluelog.txt");
            System.setOut(log);//把创建的打印输出流赋给系统。即系统下次向 ps输出
        }

        testRobot.add(0);
        testRobot.add(1);
        testRobot.add(2);
        testRobot.add(3);

        schedule();
    }

    // 核心代码，分析如何运动
    private static void handleFrame() {

        clearClockCoef();
        getCurEnemys();

        // 先计算每个机器人的参数，后面好用
        for (int i = 0; i < robotNum; i++) {
            if (!testRobot.contains(i)) continue;
            calcParam(i);
        }

        for (int i = 0; i < robotNum; i++) {
            if (!testRobot.contains(i)) continue;
//            if (robots[i].blockByWall){
//                robots[i].leaveWall();
//                continue;   // 被墙阻塞，先远离
//            }

            if (robots[i].attack != null){
                robots[i].attack();
                continue;
            }

            if (robots[i].nextStation == null) continue;
            if (robots[i].isArrive()){
                handleArrive(i);
            }
            robots[i].rush();
        }
    }

    private static void getCurEnemys() {
        // 获取当前敌方机器人位置
        curEnemys = new HashSet<>();
        Attack.curTargets.clear();  // 回合开始先清除
        for (int i = 0; i < robotNum; i++) {
            curEnemys.addAll(robots[i].handleEnemy());
        }
        enemysQueue.add(curEnemys);

        for (RadarPoint rp : curEnemys) {
            Point point = rp.getPoint();
            ArrayList<Station> nearStations = point.getNearStations();
            for (Station st : nearStations) {
                st.changeStatus(rp);
//                st.place = StationStatus.EMPTY
                if (st.place != StationStatus.EMPTY){
                    Main.blockStations.add(st);
                }
            }
        }
    }

    private static void clearClockCoef() {
        // 把旋转系数清空
        for (int i = 0; i < 4; i++) {
            clockCoef[i] = 0;
        }
    }

    private static void schedule() {
        initialization();
        Main.printLog("blue:"+isBlue);
        Ok();
        long t0,t1,t2 = System.currentTimeMillis();
        while (inStream.hasNextLine()) {
            t0 = System.currentTimeMillis();
//            printLog("system handle = t0-t2 = " + (t0-t2) );
            String line = inStream.nextLine();
            String[] parts = line.split(" ");
            frameID = Integer.parseInt(parts[0]);
            printLog(frameID);
            readUtilOK();

            Frame(frameID);
            t1 = System.currentTimeMillis();
//            printLog("read time = t1-t0 = " + (t1-t0) );
            handleFrame();

            if (slowMode){
                while (t2-t0<13){
                    t2 = System.currentTimeMillis();
                }
            }

            Ok();
            t2 = System.currentTimeMillis();
//            printLog("handle time = t2-t1 = " + (t2-t1) );
        }
    }

    private static void calcParam(int i) {
        if (robots[i].nextStation == null && robots[i].attack == null){

            robots[i].selectBestStation();
            Main.printLog("blockStations " + blockStations);
            robots[i].printRoute();

            // 判断是否够一个来回
            if (frameID > JudgeDuration && robots[i].nextStation != null){
                if (!robots[i].canBugJudge2()){
                    Attack.addRobot(robots[i]);
                    return;
                }
            }

        }
        if (robots[i].nextStation == null && robots[i].attack == null){
            robots[i].goToEmptyPlace();
            return;
        }
//              printLog("pos:next:["+robots[i].pos + "," + robots[i].route.next+"]");

        if (robots[i].waitStationMode){
            robots[i].goToNearStation();
            return;
        }

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

    private static void handleArrive(int i) {
        // 有物品就买，没有就等待,逐帧判断
        if (robots[i].nextStation == robots[i].srcStation && robots[i].nextStation.proStatus == 1){

            if (frameID > JudgeDuration){
                if (!robots[i].canBugJudge()){
                    Attack.addRobot(robots[i]);
                    return;
                }
            }
            Buy(i);
            robots[i].releaseSrc();
//            robots[i].destStation.bookNum++;
            printLog("buy");
            robots[i].changeTarget();
            // 有物品就卖，没有就等待,逐帧判断
        } else if (robots[i].nextStation == robots[i].destStation && !robots[i].nextStation.positionIsFull(robots[i].carry)){
            Sell(i);
            robots[i].releaseDest();    // 释放dest 资源
            robots[i].destStation.setPosition(robots[i].srcStation.type);       // 卖了以后对应物品空格置1
            printLog("sell");
            robots[i].changeTarget();

        }else {
            robots[i].waitStationMode = true;
        }
    }

    private static void initialization() {
        long t1 = System.currentTimeMillis();

        initMap();      //  初始化地图
        initZone();     //  初始化区域

        initStations();     // 初始化工作站
        initFighterStations(); // 初始化对方工作台的路径，只初始化可以卖的路径 

        initMapSeq();
        initZone2();
        initAttack();

        if (writePath && test){
            for (int i = 0; i < stationNum; i++) {
                path.println("{station " + i + ":empty Path:" + stations[i].paths.emptyPathMap + "}");
                path.println("{station " + i + ":full Path:" + stations[i].paths.fullPathMap + "}");
            }
        }

        long t2 = System.currentTimeMillis();
        printLog("init time = " + (t2 - t1) + "ms");
    }

    private static void initAttack() {
        Attack.init();      // 初始化攻击类

        if (mapSeq == 0){
            initMapSeq0();  // 通用攻击策略
        }

        // 4/0
        if (mapSeq == 4){
            initMapSeq4();
        }

        // 1/3
        if (mapSeq == 2){
            initMapSeq2();
        }

        if (mapSeq == 5){
            if (isBlue){
                Attack.addRobot(robots[0]);
            }else {
                Attack.addRobot(robots[0]);
            }
        }
        
        if (mapSeq == 6){
            if (Main.isBlue){
                Attack.addRobot(robots[0]);
            }else {
                Attack.addRobot(robots[0]);
            }
        }
    }

    private static void initMapSeq0() {
        if (isBlue){
            Attack.addRobot(robots[0]);
        }else {
            Attack.addRobot(robots[0]);
        }
    }

    private static void initMapSeq2() {
        // 没有工作台的机器人负责干扰
        Robot robot = null;
        for (Zone zone : zoneMap.values()) {
            if (zone.robots.size() == 1){
                // 获取唯一的一个机器人
                robot = zone.robots.get(0);
                break;
            }
        }
        if (robot != null){
            if (isBlue){
                Attack.addRobot(robot,0);
            }else {
                Attack.addRobot(robot,0);
            }
        }
    }

    private static void initMapSeq4() {
        if (isBlue){
            // 蓝方负责干扰
            Attack.addRobot(robots[0],0, AttackType.FOLLOWING);
            Attack.addRobot(robots[1],1,AttackType.FOLLOWING);
            Attack.addRobot(robots[2],2,AttackType.FOLLOWING);
            Attack.addRobot(robots[3],3,AttackType.FOLLOWING);

        }else {
            // 红方负责送货，不干扰
        }
    }

    private static void initMapSeq() {

        if (fighterStationNum == 0 || stationNum == 0){
            mapSeq = 4;     // 蓝方无工作台
        }
        else if (zoneMap.size()>=2){
            mapSeq = 2;      // 两个区域是2号
        }
        else if (Main.stationsBlue[0].type == 3 && Main.stationsBlue[1].type == 3 && Main.stationsBlue[2].type == 7) {
            mapSeq = 5;
        }
        else if (Main.stationsBlue[0].type == 1 && Main.stationsBlue[1].type == 2 && Main.stationsBlue[2].type == 4) {
            mapSeq = 6;
        }
        else if (Astar.narrowPathCount >= 5) {
            mapSeq = 3;
        }
        else {
            mapSeq = 1;
        }

        Main.printLog("mapSeq:" + mapSeq);

    }

    private static void initZone2() {
        // 给zone加一个优先队列
        for (Zone zone : zoneMap.values()) {
            zone.setPrioQueue();
        }
    }

    private static void initZone() {
        Mapinfo.init(wallMap);
        mapinfo = new Mapinfo();
        mapinfo.setZone(zoneMap);
        Mapinfo.printMapOriginal();
    }

    private static void initStationMap(){
        for (int i = 0; i < stationNum; i++) {
            Station station = isBlue ? stationsBlue[i] : stationsRed[i];
            stations[i] = station;
        }
        for (int i = 0; i < fighterStationNum; i++) {
            Station station = isBlue ? stationsRed[i] : stationsBlue[i];
            fighterStations[i] = station;
        }
        stationsMap = isBlue ? new HashMap<>(stationsMapBlue) : new HashMap<>(stationsMapRed);
        fighterStationsMap = isBlue ? new HashMap<>(stationsMapRed) : new HashMap<>(stationsMapBlue);
    }

    private static void initFighterStations() {
        for (int i = 0; i < fighterStationNum; i++) {
            fighterStations[i].fighterStationInitialization();           // 第一次初始化，能卖给哪些节点
        }
    }

    private static void initStations() {
        for (int i = 0; i < stationNum; i++) {
            stations[i].initialization();           // 第一次初始化，能卖给哪些节点
        }
        // 先初始化123，在456，在7
        for (int i = 1; i <= 7; i++) {
            if (stationsMap.containsKey(i)){
                ArrayList<Station> curStations = stationsMap.get(i);
                for (Station st : curStations){
                    st.initialization2();
                }
            }
        }
    }

    private static void initMap() {
        String line;
        int row = 101;
        int stationIdBlue = 0;
        int stationIdRed = 0;
        int robotId= 0;
        
        line = inStream.nextLine();
        isBlue = "BLUE".equals(line);
        while (inStream.hasNextLine()) {
            row --;
            double y = row * 0.5 - 0.25;
            line = inStream.nextLine();
            if ("OK".equals(line)) {
                stationNum = isBlue ? stationIdBlue : stationIdRed;
                fighterStationNum = isBlue ? stationIdRed : stationIdBlue;
                initStationMap(); // 初始化工作台，确认属于哪一方
                printLog("blue station:");
                for (Integer key: stationsMapBlue.keySet()){
                    printLog("type = " + key + " , nums = " +stationsMapBlue.get(key).size());
                }
                printLog("blue total = "+ stationIdBlue);
                printLog("red station:");
                for (Integer key: stationsMapRed.keySet()){
                    printLog("type = " + key + " , nums = " +stationsMapRed.get(key).size());
                }
                printLog("red total = "+ stationIdRed);
                printLog("total num = "+ (stationIdRed + stationIdRed));
                return ;
            }

            for (int i=0;i<100;i++){
                double x = i * 0.5 + 0.25;
                char c = line.charAt(i);
                if (c == '.') {
                    wallMap[100-row][i] = -1;    // 给地图赋值
                    continue;
                }
                else if (c == '#') {
                    wallMap[100-row][i] = -2;    // 给地图赋值
                    continue;
                }
                else if (c == 'A'){
                    if (isBlue){
                        wallMap[100-row][i] = robotId + 100;    // 给地图赋值
                        robots[robotId] = new Robot(robotId,x,y,robotId);
                        robotPos.put(robotId + 100, new Pos(100-row, i));
                        robotId++;
                    }else {
                        wallMap[100-row][i] = -1;    // 给地图赋值
                    }
                }
                else if (c == 'B') {
                    if (!isBlue){
                        wallMap[100-row][i] = robotId + 100;    // 给地图赋值
                        robots[robotId] = new Robot(robotId,x,y,robotId);
                        robotPos.put(robotId + 100, new Pos(100-row, i));
                        robotId++;
                    }else {
                        wallMap[100-row][i] = -1;    // 给地图赋值
                    }
                } 
                else if (c <= '9' && c >= '1'){
                    if (isBlue) {
                        wallMap[100-row][i] = stationIdBlue;    // 给地图赋值
                    }
                    else {
                        wallMap[100-row][i] = stationIdBlue + fighterStationNumStart;    // 红色方，对方的工作台从50开始
                    }
                    int type = Character.getNumericValue(c);
                    Station station = new Station(stationIdBlue,type,x,y);
                    stationsBlue[stationIdBlue] = station;
                    if (!stationsMapBlue.containsKey(type)){
                        ArrayList<Station> list = new ArrayList<>();
                        list.add(station);
                        stationsMapBlue.put(type,list);
                    }else {
                        stationsMapBlue.get(type).add(station);
                    }
                    stationIdBlue++;
                }
                else if (c <= 'i' && c >= 'a') {
                    if (!isBlue) {
                        wallMap[100-row][i] = stationIdRed;    // 给地图赋值
                    }
                    else {
                        wallMap[100-row][i] = stationIdRed + fighterStationNumStart;    // 蓝色方，对方的工作台从50开始
                    }
                    int type = c - 'a' + 1;
                    Station station = new Station(stationIdRed,type,x,y);
                    stationsRed[stationIdRed] = station;
                    if (!stationsMapRed.containsKey(type)){
                        ArrayList<Station> list = new ArrayList<>();
                        list.add(station);
                        stationsMapRed.put(type,list);
                    }else {
                        stationsMapRed.get(type).add(station);
                    }
                    stationIdRed++;
                }
            }
        }
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
//                for (Station st : sts) {
//                    printLog("id" + st.id + " value fps" + st.cycleAvgValue);
//                }
//                printLog(sts);

            }else {
                // 最多选择4条流水线
                ArrayList<Station> sts = zone.getStations();
                Collections.sort(sts);
//                for (int i = 0; i < sts.size(); i++) {
//                    printLog(sts.get(i)+ ":" + sts.get(i).cycleAvgValue);
//                }
                for (int i = 0; i < zone.robots.size(); i++) {
                    if (sts.size() > i && sts.get(i).cycleAvgValue>0){    // 没有那么多工作站，不分了
                        WaterFlow flow = new WaterFlow(sts.get(i),zone);
                        flow.assignRobot(1);    // 一个机器人负责一个
                        waterFlows.add(flow);
                    }
                }
            }
        }
//        printLog(waterFlows);
    }

    // 根据得到的 ZoneInfo 判断地图和工作台 
    private static int processZoneInfo() {
        for (Map.Entry<Integer, Zone> entry : zoneMap.entrySet()) {
            Zone zone = entry.getValue();

            // 蓝色方，并且对方的工作台数量不为0
            if ((isBlue && zone.robots.size() == 1 && zone.stationsMap.size() != 0) || (!isBlue && zone.robots.size() == 1) && zone.stationsMap.size() == 0) {
                Attack.addRobot(zone.robots.get(0));
                return 2;
            }
            // 如果连通的机器人数量为4，而且工作台数量为0
            if ((isBlue && zone.robots.size() == 4 && zone.stationsMap.size() == 0) || (!isBlue &&  zone.robots.size() == 4 && zone.fighterStationsMap.size() == 0)) {
                return 4;
            }
        }
        return -1;
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
            }else if (stationId < stationNum + robots.length){
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
            else {
                Robot robot = robots[stationId - stationNum - robots.length];
                //                Main.printLog(line); //数据量大
                for (int i = 0; i < 360; i++) {
                    robot.radar[i] = Double.parseDouble(parts[i]);
                }
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
            if (isBlue){
                bluelog.println(log);
            }else {
                redlog.println(log);
            }
        }
    }
}
