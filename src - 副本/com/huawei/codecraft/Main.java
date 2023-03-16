package com.huawei.codecraft;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;
import com.huawei.codecraft.*;

public class Main {

    private static final Scanner inStream = new Scanner(System.in);
    private static final PrintStream outStream = new PrintStream(new BufferedOutputStream(System.out));
    private static PrintStream log = null;

    static Robot[] robots = new Robot[4];
    static Station[] stations = new Station[50];
    public static Map<Integer, ArrayList<Station>> map = new HashMap<>(); // 类型，以及对应的工作站
    static int stationNum = 0;
    public static final int duration = 3 * 60;
    public static final int fps = 50;

    public static void main(String[] args) throws FileNotFoundException {

        log = new PrintStream("./log.txt");
        System.setOut(log);//把创建的打印输出流赋给系统。即系统下次向 ps输出
        System.out.println("这行语句将会被写到log.txt文件中");
        schedule();
    }

    // 核心代码，分析如何运动
    private static void analyse() {
        for (int i = 0; i < 4; i++) {
            System.out.println(robots[i]);
            if (robots[i].nextStation == null){
                robots[i].selectBestStation();
                robots[i].calcRoute();
                robots[i].rush();
            }else {
                if (robots[i].isArrive()){
                    if (robots[i].nextStation == robots[i].srcStation && robots[i].nextStation.proStatus == 1){
                        printBuy(i);
                        robots[i].srcStation.bookPro = false;       //解除预定
                        System.out.println("buy");
                        robots[i].changeTarget();
                    } else if (robots[i].nextStation == robots[i].destStation && !robots[i].nextStation.positionIsFull(robots[i].carry)){
                        printSell(i);
                        robots[i].destStation.bookRow[robots[i].srcStation.type] = false;   //解除预定
                        System.out.println("sell");
                        robots[i].changeTarget();
                    }
                }else{
                    robots[i].rush();
                }
            }
        }
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

    private static void schedule() {
        initMap();
        initialization();
        outStream.println("OK");
        outStream.flush();

        int frameID;
        while (inStream.hasNextLine()) {
            String line = inStream.nextLine();
            String[] parts = line.split(" ");
            frameID = Integer.parseInt(parts[0]);
            System.out.println(frameID);
            readUtilOK();

            printFrame(frameID);
            analyse();
            printOk();
        }
    }

    private static void initialization() {
        for (int i = 0; i < stationNum; i++) {
            stations[i].initialization();
//            System.out.println(stations[i].type + " -> "+ stations[i].canSellStations.size());
//            if (stations[i].canSellStations.size() > 0)
//            System.out.println(stations[i].type + " -> "+ stations[i].canSellStations.size() + " top money =" + stations[i].canSellStations.peek().getValue());
        }
    }


    private static boolean readUtilOK() {
        String line;
        line = inStream.nextLine(); // stationNum
//        System.out.println(line);
        assert stationNum == Integer.parseInt(line);
        int stationId = -1;
        while (inStream.hasNextLine()) {
            stationId ++;
            line = inStream.nextLine();
//            System.out.println(line);
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
                robot.x = Double.parseDouble(parts[8]);
                robot.y = Double.parseDouble(parts[9]);
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
//            System.out.println(line);
            if ("OK".equals(line)) {
                stationNum = stationId;
                for (Integer key: map.keySet()){
                    System.out.println("type = " + key + "nums = " +map.get(key).size());
                }
//                System.out.println(robotId);
//                System.out.println("hi");
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

}
