package com.huawei.codecraft.util;

import com.huawei.codecraft.Main;

/**
 * @Author: ro_kin
 * @Data:2023/3/13 19:47
 * @Description: TODO
 */
public class Robot {

    public static final double pi  = 3.1415926;
    public static final double emptyRadius = 0.45;
    public static final double fullRadius = 0.53;
    public static final int density = 20;
    public static final int maxSpeed = 6;//m/s
//    public static final int minSpeed = -2;//m/s
    public static final double maxRotate = pi/2;//rad/s
    public static final int maxForce = 250;//N
    public static final int maxRotateForce = 50;//N*m

    public static final double canForwardRad = pi/3 ; // 行走最小角度偏移量，0.4=23度
//    public static final double angleSpeedOffset = 0.1 ; //(rad)最大误差 0.003
    public static double emptyA;     //加速度
    public static double fullA;     //加速度

    public static double emptyRotateA;     //角加速度
    public static double fullRotateA;     //角加速度
    public static double emptyMinAngle; // 加速减速临界值 ,空载
    public static double fullMinAngle; // 加速减速临界值 ，满载


    int id;
    public int StationId; // -1 无 ，从0 开始 表示第几个工作台
    public int carry;  // 携带物品 1-7
    public double timeValue;  // 时间价值系数 [0.8 - 1]
    public double bumpValue;       //碰撞价值系数 [0.8 - 1]
    public double angV; //角速度  弧度/s 正逆，负顺
    public double lineVx;    //线速度， m/s
    public double lineVy;    //线速度， m/s
    public double turn; //朝向 [-pi,pi] 0朝向右，pi/2  朝上
    public double x;
    public double y;

    public Station nextStation;    // null : no target to go
    public Station srcStation;
    public Station destStation;

    Route route;

    static {
        emptyA = calcAcceleration(emptyRadius);
        fullA = calcAcceleration(fullRadius);

        emptyRotateA = calcRotateAcce(emptyRadius);
        fullRotateA = calcRotateAcce(fullRadius);

        emptyMinAngle = calcMinAngle(true);
        fullMinAngle = calcMinAngle(false);
    }


    private static double calcMinAngle(boolean isEmpty) {
        double a = isEmpty ? emptyRotateA:fullRotateA;
        return Math.pow(maxRotate,2)/(a);
    }

    private static double calcRotateAcce(double radius) {
        double s = pi * radius * radius;
        double m = s * density;
        double I = m * radius * radius * 0.5;
        double rotateA = maxRotateForce/I;
        return rotateA;
    }

    private static double calcAcceleration(double radius) {
        double s = pi * radius * radius;
        double m = s * density;
        double a = maxForce / m;
        return a;
    }


    public Robot(int stationId, double x, double y, int robotId) {
        StationId = stationId;
        this.x = x;
        this.y = y;
        this.nextStation = null;
        this.srcStation = null;
        this.destStation = null;
        id=robotId;
    }

    public static double calcTimeValue(int fps) {
        return f(fps,9000,0.8);
    }

    public static double calcBumpValue(int impulse) {
        return f(impulse,1000,0.8);
    }

    public static double f(int x, double maxX, double minRate){
        if (x>=maxX){
            return minRate;
        }
        else {
            double t1 = 1-Math.pow(1-x/maxX,2);
            double t2 = 1-Math.pow(t1,0.5);
            return t2 * (1-minRate) + minRate;
        }
    }

    // 选一个最佳的工作站
    public void selectBestStation() {
//        Station station = selectClosestStation();
        Station station = selectTimeShortestStation();
//        Station maxStation = selectBestValueStation();
        if (station == null){
            Main.printLog("no station can use...");
            return;
        }

        nextStation = srcStation = station;
        Main.printLog("src"+srcStation);
        destStation = srcStation.availNextStation;
        Main.printLog("dest" + destStation);
        srcStation.bookPro = true;      // 预定位置
        destStation.bookRow[srcStation.type] = true;
    }

    private Station selectClosestStation() {
        Station closestStation = null;
        double minDistance = 10000;
        for(int i=0;i<Main.stationNum;i++){

            Station station = Main.stations[i];
            if (station.leftTime == -1 || station.bookPro) continue;
            double dis = station.calcDistance(x,y);
            if (dis < minDistance){
                // 卖方有货，卖方有位置
//                if (station.type>3 && station.leftTime ==-1) continue;  // 未生产产品
                Station oth = station.chooseAvailableNextStation();
                if (oth != null){
                    closestStation = station;
                    minDistance = dis;
                }
            }
        }
        return closestStation;
    }

    //选择取货时间最短的，取货时间 = max {走路时间，生成时间}
    private Station selectTimeShortestStation() {
        Station shortestStation = null;
        double shortest = 10000;
        for(int i=0;i<Main.stationNum;i++){

            Station station = Main.stations[i];
            if (station.leftTime == -1 || station.bookPro) continue;
            double dis = station.calcDistance(x,y);
            double time1 = calcFpsToPlace(dis);
            double time = Math.max(time1,station.leftTime);
            if (time < shortest){
                // 卖方有货，卖方有位置
                Station oth = station.chooseAvailableNextStation();
                if (oth != null){
                    shortestStation = station;
                    shortest = time;
                }
            }
        }
        return shortestStation;
    }

    // 计算最快到达需要多久
    private int calcFpsToPlace(double dis) {
        double time = 0;
        double a = getAcceleration();
        double minDistance = getMinDistance();
        if (dis < minDistance/2){
            time = Math.pow(2*dis/a,0.5);
        }else {
            double time1 = Math.pow(minDistance/a,0.5);
            double time2 = (dis - minDistance/2)/maxSpeed;
            time = time1 + time2;
        }
        return (int) (time*50);
    }

    // 计算路线
    public void calcRoute() {
        if (nextStation != null){
            route = new Route(nextStation.x,nextStation.y,this);
//            route.calcParam();
        }
    }

    public double getMinDistance() {
        return carry > 0? Station.fullMinDistance : Station.emptyMinDistance;
    }

    public double getMinAngle() {
        return carry > 0? fullMinAngle:emptyMinAngle;
    }
    public double getAcceleration() {
        return carry > 0? fullA:emptyA;
    }
    public double getAngleAcceleration() {
        return carry > 0? fullRotateA:emptyRotateA;
    }

    public void rush() {
        route.rush();
    }

    @Override
    public String toString() {
//        String s = route == null ? " ": ", tarturn=" + route.theoryTurn +", set=" + route.setMinAngle ;
        return "Robot{" +
                "id=" + id +
                ", carry=" + carry +
                ", Vr=" + angV +
//                ", Vx=" + lineVx +
//                ", Vy=" + lineVy +
                ", turn=" + turn +
                route +
                '}';
    }


    public boolean isArrive() {
        if (StationId == nextStation.Id){
            Main.printLog("robot arrived id ="+StationId);
            return true;
        }else{
            return false;
        }
    }

    public void changeTarget() {
        if (nextStation == srcStation){
            nextStation = destStation;
            calcRoute();
        }else {
            nextStation = srcStation = destStation = null;
        }
        Main.printLog("state change");
        Main.printLog("next station" + nextStation);
    }

    // 通过当前速度减速到0 的最小距离
    public double getMinDistanceByCurSpeed() {
        double a = getAcceleration();
        double v2 = Math.pow(lineVx,2) + Math.pow(lineVy,2);
        double x = v2/(2*a);
        return x;
    }

    private double getCurLineSpeed() {
        double t = Math.pow(lineVx,2) + Math.pow(lineVy,2);
        return Math.pow(t,0.5);
    }

    public double getMinAngleDistanceByCurSpeed() {
        double a = getAngleAcceleration();
        double angle = angV * angV / (2*a);
        return angle;
    }

    // 买入的商品是否有时间售出
    public boolean canBugJudge() {
        double ox = srcStation.availNextStation.x;
        double oy = srcStation.availNextStation.y;
        int needFPS = calcFpsToPlace(srcStation.calcDistance(ox,oy));
        int leftFps = Main.duration - Main.frameID - 100;   // 两s误差,后期可调整
        return leftFps > needFPS;
    }
}

