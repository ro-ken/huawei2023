package com.huawei.codecraft;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;
import com.huawei.codecraft.util.*;

public class Main {

    private static final Scanner inStream = new Scanner(System.in);
    private static final PrintStream outStream = new PrintStream(new BufferedOutputStream(System.out));
    private static PrintStream log = null;
    public static int frameID=0;
//    public static MyThread thread;

    public static Robot[] robots = new Robot[4];
    public static Station[] stations = new Station[50];
    public static Map<Integer, ArrayList<Station>> map = new HashMap<>(); // 类型，以及对应的工作站
    public static int stationNum = 0;
    public static final int duration = 3 * 60 * 50;
    public static final int JudgeDuration = 3 * 50 * 50;    //最后10s需判断买入的商品能否卖出
    public static final int fps = 50;
    public static final boolean test = false;    // 是否可写入
    public static final int robotNum = 4;

    public static void main(String[] args) throws FileNotFoundException {

        if (test){
            log = new PrintStream("./log.txt");
            System.setOut(log);//把创建的打印输出流赋给系统。即系统下次向 ps输出
//            printLog("这行语句将会被写到log.txt文件中");
        }

        schedule();
    }

    // 核心代码，分析如何运动
    private static void analyse() {

        for (int i = 0; i < robotNum; i++) {
//                printLog(robots[i].toString());

            if (robots[i].nextStation == null){
                robots[i].selectBestStation();
                robots[i].calcRoute();
                robots[i].rush();
            }else {

                // 碰撞检测
//                robots[i].calcMoveEquation();

                if (robots[i].isArrive()){
                    // 有物品就买，没有就等待,逐帧判断
                    if (robots[i].nextStation == robots[i].srcStation && robots[i].nextStation.proStatus == 1){
                        if (frameID > JudgeDuration){
                            if (!robots[i].canBugJudge()){
                                continue;
                            }
                        }
                        printBuy(i);
                        robots[i].srcStation.bookPro = false;       //解除预定
                        printLog("buy");
                        robots[i].changeTarget();
                        // 有物品就卖，没有就等待,逐帧判断
                    } else if (robots[i].nextStation == robots[i].destStation && !robots[i].nextStation.positionIsFull(robots[i].carry)){
                        printSell(i);
                        robots[i].destStation.bookRow[robots[i].srcStation.type] = false;   //解除预定

                        printLog("sell");
                        robots[i].changeTarget();
                    }
                }else{
                    // 没到目的地就继续前进
                    robots[i].rush();
                }
            }
        }

//
//        for (int i = 0; i < robotNum; i++) {
////            if ()
//            robots[i].calcMoveEquation();
//        }
////        // 对每个机器人依次进行碰撞计算
//        for (int i = 0; i < robotNum-1; i++) {
//            if (robots[i].isTempPlace) continue;
//            for (int j = i+1; j < robotNum; j++) {
//                 robots[i].calcBump(robots[j]);
//            }
//        }

    }


    public static void printSell(int robotId){
        outStream.printf("sell %d\n", robotId);
    }
    public static void printBuy(int robotId){
        outStream.printf("buy %d\n", robotId);
    }
    public static void printForward(int robotId,int speed){
        outStream.printf("forward %d %d\n", robotId, speed);
    }
    public static void printForward(int robotId,double speed){
        outStream.printf("forward %d %f\n", robotId, speed);
    }

    public static void printRotate(int robotId,double angleSpeed){
        outStream.printf("rotate %d %f\n", robotId, angleSpeed);
    }
    public static void printDestroy(int robotId){
        outStream.printf("destroy %d\n", robotId);
    }
    public static void printOk(){
        outStream.print("OK\n");
        outStream.flush();
    }
    public static void printFrame(int frameID){
        outStream.printf("%d\n", frameID);
    }
    public static void printLog(Object log){
        if (test){
            System.out.println(log);
        }
    }

    private static void schedule() {
        initMap();
        initialization();
//        Thread thread = new MyThread();
//        thread.start();
        outStream.println("OK");
        outStream.flush();


        while (inStream.hasNextLine()) {
            String line = inStream.nextLine();
            String[] parts = line.split(" ");
            frameID = Integer.parseInt(parts[0]);
            printLog(frameID);
            readUtilOK();

            printFrame(frameID);

            long t1 = System.currentTimeMillis();
            analyse();
            long t2 = System.currentTimeMillis();
            printLog("time:"+String.valueOf(t2-t1));

            printOk();
        }
    }

    private static void initialization() {
        for (int i = 0; i < stationNum; i++) {
            stations[i].initialization();
        }
    }


    private static boolean readUtilOK() {
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
                return true;
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
            // do something;
        }
        return false;
    }

    private static boolean initMap() {
        String line;
        int row = 101;
        int stationId = 0;
        int robotId= 0;
        while (inStream.hasNextLine()) {
            row --;
            double y = row * 0.5 - 0.25;
            line = inStream.nextLine();
//            printLog(line);
            if ("OK".equals(line)) {
                stationNum = stationId;
                for (Integer key: map.keySet()){
                    printLog("type = " + key + "nums = " +map.get(key).size());
                }
//                printLog(robotId);
//                printLog("hi");
                return true;
            }

            for (int i=0;i<100;i++){
                double x = i * 0.5 + 0.25;
                char c = line.charAt(i);
                if (c == '.') continue;
                if (c == 'A'){
                    robots[robotId] = new Robot(robotId,x,y,robotId);
                    robotId++;
                }else {
                    int type = Character.getNumericValue(c);
                    Station station = new Station(stationId,type,x,y);
                    stations[stationId] = station;
                    if (!map.containsKey(type)){
                        ArrayList<Station> list = new ArrayList<>();
                        list.add(station);
                        map.put(type,list);
                    }else {
                        map.get(type).add(station);
//                        stations[stationId];
                    }
                    stationId ++;
                }
            }
            // do something;
        }
        return false;
    }

    // 计算向量的点积
    public static double dotProduct(double x1,double y1, double x2,double y2) {
        return x1 * x2 + y1 * y2;
    }

    // 计算向量的点积
    public static double dotProduct(Point vector1,Point vector2) {
        return dotProduct(vector1.x,vector1.y,vector2.x,vector2.y);
    }

    // 计算向量的模长
    public static double norm(double x,double y) {
        return Math.sqrt(x*x + y*y);
    }
    public static double norm(Point vector) {
        return norm(vector.x, vector.y);
    }

}
