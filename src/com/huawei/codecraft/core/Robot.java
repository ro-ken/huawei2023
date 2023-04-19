package com.huawei.codecraft.core;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.*;
import com.huawei.codecraft.way.Astar;
import com.huawei.codecraft.way.Pos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

/**
 * @Author: ro_kin
 * @Data:2023/3/13 19:47
 * @Description: TODO
 */
public class Robot {
//    public static final double angleSpeedOffset = 0.1 ; //(rad)最大误差 0.003
    public static double emptyA;     //加速度,19.6487587,刹车距离 = 36/2a = 0.916, 刹车时间 = 6/a = 0.30536s  fps = 15.26
    public static double fullA;     //加速度,14.164733,刹车距离 = 36/2a = 1.27,刹车时间 = 6/a = 0.4236     fps =  21.18

    public static double emptyRotateA;     //角加速度,  38.8  刹车角度 = pi * pi / 2a = 0.127 7.276° 刹车时间 = pi/a = 0.08  fps = 4
    public static double fullRotateA;     //角加速度,20.17  刹车角度 = pi * pi / 2a = 0.245 14° 刹车时间 = pi/a = 0.156  fps = 7.79
    public static double emptyMinAngle; // 加速减速临界值 ,空载
    public static double fullMinAngle; // 加速减速临界值 ，满载
    public static Line rotateSpeedEquation; // 转向速度方程
    public Point basePoint;     // 其他小车躲避的点
    public Line baseLine;       // 其他小车躲避的线


    int id;
    public Zone zone;   //所属的区域
    public Attack attack;
    public int StationId; // -1 无 ，从0 开始 表示第几个工作台
    public int carry;  // 携带物品 1-7
    public double timeValue;  // 时间价值系数 [0.8 - 1]
    public double bumpValue;       //碰撞价值系数 [0.8 - 1]
    public double angV; //角速度  弧度/s 正逆，负顺
    public double lineVx;    //线速度， m/s
    public double lineVy;    //线速度， m/s
    public double turn; //朝向 [-pi,pi] 0朝向右，pi/2  朝上
    public Point pos;
    public double[] radar = new double[360]; //雷达信息
    HashSet<RadarPoint> enemy;
//    public double x, y; //坐标

    public Station nextStation;    // null : no target to go
    public Station srcStation;
    public Station destStation;

    public WaterFlow waterFlow; // 处于哪条流水线

    public Station lastStation; // 当前处于那个工作站，供决策使用，到达更新

    public Point start; // 出生的地点
    public Point midPoint = new Point(); // 记录相撞坐标的中点
    // 碰撞相关

    public Line topLine;   // 轨迹上面的线
    public Line belowLine;     // 轨迹下面的线
    public Line midLine;     // 轨迹下面的线

    public boolean tmpSafeMode = false;    // 是否去临时安全点
    public boolean waitStationMode = false;    // 是否去临时安全点
    public boolean avoidBumpMode = false;   // 与其他机器人阻塞，临时避障模式
    public RadarPoint blockEnemy = null;    // 被哪个敌人困住了
    public boolean blockByWall = false;     // 被墙困住了
    public boolean inSafePlace = false;    // 是否到达临时点

    public double lastDis = 100000; // 在临时点判断是否距离另外的机器人越来越远

    public int lastDisFps = 0; // 越来越远 经过了几帧

    public double blockFps = 0;    // 目前阻塞的帧数
    
    //下面参数可调
    public static double minDis = 0.1; // 判定离临时点多近算到达
    public static int minLastDisFps = 5; // 越来越远 经过了多少帧，小车开始走
    public static double maxSpeedCoef = 1.5;
    public static double stationSafeDisCoef = 2;    // 工作站的安全距离距离系数
    public static int cacheFps = 50 * 4;     // 判断是否要送最后一个任务的临界时间 > 0
    public static double blockJudgeSpeed = 0.5 ;    // 判断机器人是否阻塞的最小速度
    public static int blockJudgeFps = 20 ;    // 则阻塞速度的fps超过多少判断为阻塞 ，上面speed调大了这个参数也要调大一点,
    public static int maxWaitBlockFps = 50 ;    // 等待超过多长时间目标机器人没有来，就自行解封  todo 重要参数

    public static double robotInPointDis = 0.2 ;    // 判断机器人到达某个点的相隔距离
    public static double detectWallWideCoef = 0.8 ;    // 半径乘子，判断从圆心多远的地方发出的射线会经过障碍物  todo 重要参数

    public static final double pi  = 3.1415926;
    public static final double emptyRadius = 0.45;
    public static final double fullRadius = 0.53;
    public int density = 20;
    public int maxSpeed = 6;//m/s
    public static final double maxRotate = pi;//rad/s
    public double maxForce = 250;//N
    public double maxRotateForce = 50;//N*m

    public static  double canForwardRad = pi/2 ; // 行走最小角度偏移量
    public static  double maxForwardRad = pi/4 ; // 最大速度的最小角度

    public Robot winner;
    public HashSet<Robot> losers = new HashSet<>(); // 要避让我的点
    public Route route;
    public static int step = 1;//计算雷达扫描出360//step个点的坐标

    public static int srcBumpCost = 50 * 3;  // 寻找卖家，有敌方机器人计算时间要加上代价 fps
    public static int destBumpCost = 50 * 6;  // 终点是789 寻找买家，有敌方机器人计算时间要加上代价 fps
    public static int dest456BumpCost = 50 * 5;  // 终点是456 寻找买家，有敌方机器人计算时间要加上代价 fps
    public static int redEnemyCost = 50 * 2;  // 红方作为障碍物的代价
    public static int blueEnemyCost = 50 * 4;  // 蓝方作为障碍物的代价



    public Robot(int stationId, double x, double y, int robotId) {

        if (!Main.isBlue){
            // 红方的参数
            maxSpeed = 7;
            density = 15;
            maxForce = 187.5;
            maxRotateForce = 37.5;
        }

        // 先计算基本参数
        emptyA = calcAcceleration(emptyRadius);
        fullA = calcAcceleration(fullRadius);

        emptyRotateA = calcRotateAcce(emptyRadius);
        fullRotateA = calcRotateAcce(fullRadius);

        emptyMinAngle = calcMinAngle(true);
        fullMinAngle = calcMinAngle(false);

        StationId = stationId;
        this.nextStation = null;
        this.srcStation = null;
        this.destStation = null;
        pos = new Point(x,y);
        start = new Point(x,y);
        id=robotId;
        topLine = new Line();
        belowLine = new Line();
        midLine = new Line();
        rotateSpeedEquation = new Line(new Point(maxForwardRad,maxSpeed/maxSpeedCoef),new Point(pi/2,0));

    }


    public static double getMaxSpeed() {
        if (Main.isBlue){
            return 6;
        }else {
            return 7;
        }
    }

    @Override
    public String toString() {
        return "Robot{" +
                "id=" + id +
                ", pos=" + pos +
                '}';
    }

    private double calcAcceleration(double radius) {
        double s = pi * radius * radius;
        double m = s * density;
        double a = maxForce / m;
        return a;
    }

    // 根据两点，计算当前小车到达两点的时间
    private double[] calcBumpTimeRange(Point p1, Point p2) {
        double dis1 = pos.calcDistance(p1);
        double dis2 = pos.calcDistance(p2);
        double fps1 = calcFpsToPlaceInCurSpeed(dis1);
        double fps2 = calcFpsToPlaceInCurSpeed(dis2);
        double[] res;
        if (fps1<fps2){
            res = new double[]{fps1,fps2};
        }else {
            res = new double[]{fps2,fps1};
        }
        return res;
    }
    
    // 计算最快到达需要多久
    public int calcFpsToPlace(double dis) {
        double time = 0;
        double a = getAcceleration();
        double minDistance = getMinDistance();
        if (dis < minDistance/2){
            time = Math.sqrt(2*dis/a);
        }else {
            double time1 = Math.sqrt(minDistance/a);
            double time2 = (dis - minDistance/2)/maxSpeed;
            time = time1 + time2;
        }
        return (int) (time*50);
    }


    // 若以当前速度加速到达，最快要多久
    private int calcFpsToPlaceInCurSpeed(double dis) {
        double time = 0;
        double a = getAcceleration();
        double v0 = Point.norm(lineVx,lineVy);
        double x1 = (maxSpeed*maxSpeed - v0*v0)/(2*a);
        if (dis<x1){
            // x = v0t + 0.5at^2;   算不了，简单替代下v0= 0
            time = (-v0 + Math.sqrt(v0 * v0 + 2 * a * dis)) / a;
        }else{
            time = (-v0 + Math.sqrt(v0 * v0 + 2 * a * x1)) / a;
            time += (dis-x1)/maxSpeed;
        }

        return (int) (time*50);
    }

    private static double calcMinAngle(boolean isEmpty) {
        double a = isEmpty ? emptyRotateA:fullRotateA;
        return Math.pow(maxRotate,2)/(a);
    }

    //计算机器人的轨迹方程，也就两条平行线
    public void calcMoveEquation() {

        if (route.vector.x == 0) return;// 不能是垂直的情况，在调用此函数之前事先要做出判断
        double radius = getRadius() * detectWallWideCoef;
//        Point[] src = getPoints(pos.x,pos.y,radius);
//        Point[] dest = getPoints(route.next.x,route.next.y,radius);
        Point[] src = Point.getPoints(route.vector,pos,radius);
        Point[] dest = Point.getPoints(route.vector,route.next,radius);
        topLine.setValue(src[0],dest[0]);
        belowLine.setValue(src[1],dest[1]);
        midLine.setValue(pos,route.next);

    }

    private double calcRotateAcce(double radius) {
        double s = pi * radius * radius;
        double m = s * density;
        double I = m * radius * radius * 0.5;
        double rotateA = maxRotateForce/I;
        return rotateA;
    }

    // 计算路线
    public void calcRoute() {

        if (nextStation != null){

            ArrayList<Point> path = null;
            HashSet<Pos> pos1 = null;
            if (lastStation == null){
                path = nextStation.paths.getPath(true,start);   // 第一次，计算初始化的路径
                pos1 = nextStation.paths.getResSet(true,start);
                path = Path.reversePath(path);
            }else {
                boolean isEmpty = nextStation == srcStation;
                path = lastStation.paths.getPath(isEmpty,nextStation.pos);
                pos1 = lastStation.paths.getResSet(isEmpty,nextStation.pos);
            }
            route = new Route(nextStation.pos,this,path,pos1);
        }
    }


    public Route calcRouteFromNowBlockRobot(HashSet<Point> robots) {
        // 计算从自身到目的地位置的路由，封住敌人位置
        if (nextStation != null){
            HashSet<Pos> posSet = new HashSet<>();
            ArrayList<Point> path = nextStation.paths.getPathBlockRobot(carry == 0,pos,posSet,robots);   // 第一次，计算初始化的路径
            if (path.size() == 0){
                return null;
            }
            path = Path.reversePath(path);

            return new Route(nextStation.pos,this,path,posSet);
        }
        return null;
    }


    public void recoveryPath() {

        if (attack != null){
            // 如果是攻击模式发生阻塞
            attack.calcRouteFromNow();  // 重新计算路线
            Main.printLog(pos + ":attack robot recovery path");
            return;
        }
        clearWinner();
        // 重新寻找新路径
        calcRouteFromNow();
        route.calcParamEveryFrame();    // 通用参数
        calcMoveEquation();     //  运动方程
        Main.printLog(pos + ":recovery path"+route.path);
    }
    public void calcRouteFromNow() {
        // 计算从自身到目的地位置的路由

        if (nextStation == null || nextStation.paths == null) return;
        ArrayList<Point> path = nextStation.paths.getPath(carry == 0,pos);   // 第一次，计算初始化的路径
        HashSet<Pos> pos1 = nextStation.paths.getResSet(carry == 0,pos);
        path = Path.reversePath(path);

        route = new Route(nextStation.pos,this,path,pos1);

    }

    public void calcTmpRoute(Point sp, Robot winRobot) {

        tmpSafeMode = true;
        winner = winRobot;      // 设置是给谁避让，后期需要定期探测这个机器人是否到达目标点
        inSafePlace = false;
        HashSet<Pos> pos1 = new HashSet<>();
//        ArrayList<Point> path = Astar.getPath(carry==0,pos,sp);
        ArrayList<Point> path = Astar.getPathAndResult(carry==0,pos,sp,pos1);
        if (path.size() >0){
            path.remove(path.size()-1);
        }
        path.add(sp);
        route = new Route(sp,this,path,pos1);
        route.calcParamEveryFrame();    // 通用参数
        calcMoveEquation();     //  运动方程

        Main.printLog(pos + ":sp" + sp);
        Main.printLog("set tmp route"+path);
    }


    public static double calcTimeValue(int fps) {
        return f(fps,9000,0.8);
    }

    // 买入的商品是否有时间售出
    public boolean canBugJudge() {
        int needFPS = calcFpsToPlace(srcStation.pos.calcDistance(destStation.pos));
        int leftFps = Main.duration - Main.frameID - cacheFps;   // 1s误差,后期可调整
        return leftFps > needFPS;
    }

    // 到达一个目的地，更换下一个
    public void changeTarget() {
        if (nextStation == srcStation){
            nextStation = destStation;
            lastStation = srcStation;
            destStation.bookNum ++;
            calcRoute();
        }else {
            if (waterFlow != null) {
                useWaterFlowChangeMode();
//                lastStation = nextStation;
            }
            lastStation = nextStation;
            nextStation = srcStation = destStation = null;

        }
        blockFps = 0;     // 阻塞帧数重新计算

        // 到达目的地，接除所有关系
        resetStatus();
        Main.printLog("state change");
        Main.printLog("next station" + nextStation);
    }

    public static double f(int x, double maxX, double minRate){
        if (x>=maxX){
            return minRate;
        }
        else {
            double t1 = 1-Math.pow(1-x/maxX,2);
            double t2 = 1-Math.sqrt(t1);
            return t2 * (1-minRate) + minRate;
        }
    }

    public double getAcceleration() {
        return carry > 0? fullA:emptyA;
    }

    public double getAngleAcceleration() {
        return carry > 0? fullRotateA:emptyRotateA;
    }

    public double getMinDistance() {
        return carry > 0? Station.fullMinDistance : Station.emptyMinDistance;
    }

    public double getMinAngle() {
        return carry > 0? fullMinAngle:emptyMinAngle;
    }

     // 通过当前速度减速到0 的最小距离
     public double getMinDistanceByCurSpeed() {
        double a = getAcceleration();
        double v2 = Math.pow(lineVx,2) + Math.pow(lineVy,2);
        double x = v2/(2*a);
        return x;
    }

    public void setBase(Line line, Point point) {
        baseLine = new Line(new Point(line.left),new Point(line.right));
        basePoint = new Point(point);
        Main.printLog("this"+this + "line" +line+"bp" + point);
    }


    public double getMinAngleDistanceByCurSpeed() {
        double a = getAngleAcceleration();
        double angle = angV * angV / (2*a);
        return angle;
    }

    public double getRadius() {
        return carry > 0?fullRadius:emptyRadius;
    }

    // 判断目标点是否到达工作台
    public boolean isArrive() {
        if (StationId == nextStation.id){
//            Main.printLog("robot arrived id ="+StationId);
            return true;
        }else{
            return false;
        }
    }

    // 判断范围是否有冲突
    private boolean ifRangeConflict(Robot other) {
        Point p1 = topLine.calcIntersectionPoint(other.belowLine);
        Point p2 = belowLine.calcIntersectionPoint(other.topLine);
        // 3、预测机器人经过交集区域的时间帧区间
        double[] t1 = calcBumpTimeRange(p1,p2);
        double[] t2 = other.calcBumpTimeRange(p1,p2);
        if (t1[0] > t2[1] || t2[0] > t1[1]){
            return false;
        }
        return true;
    }


    // 检测路线是否有重叠区域
    public boolean routeBumpDetect(Robot other) {
        if (topLine.left == null||other.topLine.left == null) return true;
        double left = Math.max(topLine.left.x,other.topLine.left.x);
        double right = Math.min(topLine.right.x,other.topLine.right.x);
        double m_l_min = belowLine.getY(left);
        double m_l_max = topLine.getY(left);
        double m_r_min = belowLine.getY(right);
        double m_r_max = topLine.getY(right);

        double o_l_min = other.belowLine.getY(left);
        double o_l_max = other.topLine.getY(left);
        double o_r_min = other.belowLine.getY(right);
        double o_r_max = other.topLine.getY(right);

        // 最小 > 最大 ，无交集
        if (m_l_min > o_l_max && m_r_min > o_r_max || o_l_min > m_l_max && o_r_min > m_r_max){
            return false;
        }
        return true;
    }

    public void rush() {

        if (nextStation == null) {
            Main.printLog("nextStation is null");
            return;
        }

        if (nextStation.place == StationStatus.BLOCK){
            // 家被占了，考虑是换工作站还是撞开
            Main.printLog("blockStations " + Main.blockStations);
            //
            fixStationBlocked();
        }

        if (losers.size()>0){
            // 自己是winner，需要判断是否到达了basePoint 若是，则释放相应的节点
            for (Robot loser : losers) {
                if (basePoint != null){
                    double dis = pos.calcDistance(loser.basePoint);
                    if (dis < 1.0){
                        loser.inSafePlace = true;   // 到达了目标点，让机器人做时间判断
                    }
                }
            }
        }

        if (blockEnemy!=null){
            // 被其他机器人堵死了，要逃离
            Main.printLog("blocked ");
            route.handleBlockByEnemy();
        }

        if (avoidBumpMode){
            boolean ret = handleAvoidBumpMode();
            if (ret) return;
        }

        if (tmpSafeMode){
            handleTmpSafeMode();
        }
        route.rush();
        route.deletePos();  // 以走过的点要删除，防止发生误判
    }

    private void handleTmpSafeMode() {
        if (!inSafePlace){
            // 如果未到安全点，要判断是否到达
            double dis = pos.calcDistance(route.target);
            if (dis < minDis) {
                inSafePlace = true;
                blockFps = 0;   // 重新计数
            }
        }else {
            // 到达了安全点，要判断是否能走
            if (roadIsSafe()){
                Main.printLog(22222);
                recoveryPath();   // 对方通过狭窄路段，重新寻路
            }
        }
    }

    private boolean handleAvoidBumpMode() {
        boolean ret = false;
        Point fp = null ;
        Robot fr = null;
        double minDis = 10000;
        for (Robot oth : zone.robots) {
            if (oth == this) continue;
            double dis = pos.calcDistance(oth.pos);
            if (dis < getRadius() + oth.getRadius() + 2 && dis<minDis){
                minDis = dis;
                fp = oth.pos;   //选择距离自己最近的机器人
                fr = oth;
            }
        }

        if (fp != null){
            // 多个机器人卡住了，找一个安全点
//            route.willBumpRobot = fr;
//            route.setTmpSafeMode2();
//            avoidBumpMode = false;
            randomBack(fp);
            ret = true;
        }else {
            avoidBumpMode = false;
            recoveryPath();     // 恢复路径
        }
        return ret;
    }

    public void fixStationBlocked() {

        Main.printLog("Occupied" + nextStation);

        if (nextStation == srcStation){
            // 若是源被占
            // 先释放资源
            releaseSrc();
            lastStation = nextStation = srcStation = destStation = null;
//            zone.scheduler(this);     //todo 可尝试放开
            // 解除关系，等待调度
        }else {
            // 查看是否有其他可用的next，有就送过去
            Station dest = null;
            int minFps = 10000000;
            for (Station st : zone.stationsMap.get(nextStation.type)) {
                if (st.place == StationStatus.EMPTY && st.canBuy(srcStation.type)){
                    // todo
                    int fps = st.pathToFps(false,pos);  // 计算到自己的距离
                    if(fps < minFps){
                        minFps = fps;
                        dest = st;
                    }
                }
            }
            Main.printLog("dest" + dest);
            if (dest != null){
                changeNextStation(dest);
            }else {
                // todo 没有其他源了，如果可以撞，直接撞
                // 考虑是否丢弃，会亏钱
                Main.printLog("no available next station");
            }
        }
    }

    private void changeNextStation(Station dest) {
        Main.printLog("change next station" + dest);
        // 先释放资源，
        releaseDest();
        // 更改目的地
        nextStation = destStation = dest;
        // 占用资源
        if (destStation.type <= 7)  {   // 8,9 不需要预定
            destStation.bookRow[srcStation.type] = true;
            destStation.bookRawNum[srcStation.type] ++;
        }
        destStation.bookNum++;       //解除预定
        calcRouteFromNow();
    }

    // 随机后退
    private void randomBack(Point fp) {
        // 要远离目标位置
        // 随机选择后退
        // 在与目标角度为 pi/4 ,- pi/4，前进
        Point vec=pos.calcVector(fp);
        // 面朝目标，然后后退
        double theoryTurn = Math.atan2(vec.y, vec.x);
        boolean right = false;  // 方向是否对，在一定的夹角内
        if (theoryTurn*turn>0){
            // 在同一个方向，
            if (Math.abs(theoryTurn-turn) < pi/4){
                right = true;
            }
        }else {
            double abs1 = Math.abs(theoryTurn);
            double abs2 = Math.abs(turn);
            if (Math.max(abs1,abs2)<pi/8 || Math.min(abs1,abs2)>pi*7/8){
                right = true;
            }
        }
        int clockwise = 1;
        if (right){
            // 方向对了，加一个随机参数，晃出去
            Random random = new Random();
            if (random.nextInt(2) == 0){
                clockwise = -1;
            }
        }else {
            if (theoryTurn>turn && theoryTurn - turn < Robot.pi || theoryTurn<turn && turn - theoryTurn > Robot.pi){
                clockwise = 1;
            }else {
                clockwise = -1;
            }
        }

        Main.Forward(id,-2);
        Main.Rotate(id,maxRotate * clockwise);
    }

    private boolean roadIsSafe() {
        // 判断winner已经走过去了
        // 连续 一段时间两车越来越远，说明过了

        double dis = 0;
        if (basePoint !=null){
            dis = winner.pos.calcDistance(basePoint);

            if (dis <= 1.5 && lastDis < dis){
                lastDisFps ++;
            }else {
                lastDisFps = 0;
            }
        }else {
            // baseP ==0 ，换一种判断策略
            dis = winner.pos.calcDistance(pos);

            if (dis <= 2.5 && lastDis < dis && lineNoWall(pos,winner.pos)){
                lastDisFps ++;
            }else {
                lastDisFps = 0;
            }
        }

        lastDis = dis;

        // 超过界限，能动了
        if (lastDisFps > minLastDisFps){
            return true;
        }

        return false;
    }

    private boolean lineNoWall(Point src, Point dest) {
        // 判断两点连线没有墙体
        Line line = new Line(src,dest);
        Point wall = line.getNearBumpWall();
        return wall == null;
    }

    // 选一个最佳的工作站
    public void selectBestStation() {
        if (waterFlow == null){
            zone.scheduler(this);
        }else {
            waterFlow.scheduler(this);
        }

    }
    
    public void setSrcDest(Station src, Station dest) {
        if (src == null || dest == null) return;

        nextStation = srcStation = src;
        destStation = dest;

        Main.printLog(this + " src, "+srcStation + " dest, " + destStation);
        srcStation.bookPro = true;      // 预定位置

        if (destStation.type <= 7)  {   // 8,9 不需要预定
            destStation.bookRow[srcStation.type] = true;
            destStation.bookRawNum[srcStation.type] ++;
        }

        src.bookNum ++;
//        if (dest.type <= 6 && waterFlow != null){
//            waterFlow.halfComp.put(dest.type,waterFlow.halfComp.get(dest.type) +1);    // 原料数 +1
//        }

        calcRoute();
    }

    public boolean isArrivePoint(Point pre, Point next) {
        double dis1 = pre.calcDistance(next);
        double dis2 = pre.calcDistance(pos);

        if (dis2>dis1){
            return true;
        }

        double dis = pos.calcDistance(next);
        return dis <= robotInPointDis;
    }



    private void useWaterFlowChangeMode() {
        // 流水线模式，加一些控制
        Main.printLog("nextStation"+ nextStation);
        // 如果两个物品格满了算作完成一个任务
        if (nextStation.positionFull() && nextStation.type <=6){
            waterFlow.completed.put(nextStation.type,waterFlow.completed.get(nextStation.type) + 1);    // 完成数 + 1
            waterFlow.halfComp.put(nextStation.type,waterFlow.halfComp.get(nextStation.type) - 2);    // 原料数 -2
        }
    }

    public boolean blockDetect() {
        // todo 后面判断是否周围有墙或者机器人
        // 阻塞检测，在某个点阻塞了多少帧，重新设置路径
        // todo 要加上和敌方机器人相遇阻塞的情况

        if (tmpSafeMode && inSafePlace){
            blockFps ++;
            if (blockFps > maxWaitBlockFps){
                Main.printLog(333333);
                recoveryPath();
                blockFps = 0;
            }
            return false;   // 如果是等待模式不判定阻塞
        }


        if (route.speed.norm() > blockJudgeSpeed){
            blockFps = 0;
        }else {
            blockFps ++;
        }

        if (blockFps >= blockJudgeFps){
            blockFps = 0;   //后面需要重新寻路  不属于临时安全模式
            return true;
        }
        return false;
    }

    public void setNewPath() {

        avoidBumpMode = false;
        blockEnemy = null;
        blockByWall = false;

        // 先判断附近是否有敌人
        for (RadarPoint curEnemy : Main.curEnemys) {
            Point point = curEnemy.getPoint();
            double dis = pos.calcDistance(point);
            double radius = curEnemy.isFull == 0?0.45:0.53;
            if (dis < getRadius() + radius + 0.2){
                blockEnemy = curEnemy;
                return;     // 被敌人阻塞，直接返回
            }
        }


        // 判断阻塞是否是和机器人堵住了
        for (Robot oth : zone.robots) {
            if (oth == this) continue;
            double dis = pos.calcDistance(oth.pos);
            if (dis < getRadius() + oth.getRadius() + 0.1){
                // 若是和机器人堵住了，要远离
                avoidBumpMode = true;
            }
        }

        if (!avoidBumpMode){
            if (tmpSafeMode){
                calcTmpRoute(route.target,winner);
            }else {
                Main.printLog(444444);
                recoveryPath();
            }
        }else {
            // 周围没有自己军队，也没有敌人，被墙困住
            blockByWall = true;
        }

    }

    public Point selectTmpSafePoint(Point dest, HashSet<Pos> posSet, Point midPoint) {
//        Main.printLog(midPoint);
        Point sp = Astar.getSafePoint2(carry == 0, pos, dest, posSet,midPoint);
        Main.printLog("sp:" + sp);
        HashSet<Point> ps = new HashSet<>();
        for (Pos pos1 : posSet) {
            ps.add(Astar.Pos2Point(pos1));
        }
//        Main.printLog(this);
//        Main.printLog(ps);

        return sp;
    }

    public void goToEmptyPlace() {
        // 如果机器人没事干，不要待在工作台边上
        Pos p = Astar.Point2Pos(pos);
        int status = Main.wallMap[p.x][p.y];
        int printSpeed = 0;
        int printRotate = 0;
        if (pos.nearStation()){
            printSpeed = 1;
        }

        if (route != null){
            // 判断是否与其他人靠近，若是要远离
            for (Robot oth : zone.robots) {
                if (oth == this) continue;
                double dis = pos.calcDistance(oth.pos);
                if (dis<2){
                    // 太靠近，要远离
                    // 先算线速度，夹角小于pi/2 刹车，大于pi/2 全速
                    Point vec = oth.pos.calcVector(pos);
                    Point v = new Point(lineVx,lineVy);
                    if (v.norm() == 0){
                        printSpeed = 1;
                    }else {
                        double angle = vec.calcDeltaAngle(v);
                        if (angle < Robot.pi){
                            printSpeed = 1;
                        }else {
                            printSpeed = -1;
                        }
                    }
                    break;
                }
            }
        }

        Main.Forward(id,printSpeed);
        Main.Rotate(id,printRotate);
    }

    public void setWeakRobot(Robot weakRobot) {
        // 设置对方为我的loser机器人
        if (tmpSafeMode){
            // 若我是loser，先解除封印
            Main.printLog(55555);
            recoveryPath();
        }
        weakRobot.resetStatus();
        losers.add(weakRobot);

    }

    // 自己是loser 恢复自由
    private void resetStatus() {
        if (tmpSafeMode){
            clearWinner();
        }

        if (!losers.isEmpty()){
            HashSet<Robot> tmp = new HashSet<>(losers);
            for (Robot loser : tmp) {
                Main.printLog(66666);
                loser.recoveryPath();
            }
            losers.clear();
        }
    }

    // 自己是loser 恢复自由
    private void clearWinner() {
        if (winner != null){
            winner.losers.remove(this);
            winner = null;
            // 解除主从关系
        }
        tmpSafeMode = false;
        basePoint = null;
    }

    public void goToNearStation() {
        // 工作台有满了，到旁边等待
        Pos p = Astar.Point2Pos(pos);

        boolean flag1 = nextStation == srcStation && nextStation.proStatus == 1;
        boolean flag2 = nextStation == destStation && !nextStation.positionIsFull(carry);
        if (flag1 || flag2){
            waitStationMode = false;
            return;
        }

        double printSpeed = 0;
        double printRotate = 0;
        if (pos.nearStation()){
            printSpeed = -2;
            Random random = new Random();
            int clockwise = random.nextInt(2) == 1? 1:-1;
            printRotate = maxRotate/8 * clockwise;
        }

        if (route != null){
            // 判断是否与其他人靠近，若是要远离
            for (Robot oth : zone.robots) {
                if (oth == this) continue;
                double dis = pos.calcDistance(oth.pos);
                if (dis<2){
                    // 太靠近，要远离
                    // 先算线速度，夹角小于pi/2 刹车，大于pi/2 全速
                    Point vec = oth.pos.calcVector(pos);
                    Point v = new Point(lineVx,lineVy);
                    if (v.norm() == 0){
                        printSpeed = 1;
                    }else {
                        double angle = vec.calcDeltaAngle(v);
                        if (angle < Robot.pi){
                            printSpeed = 1;
                        }else {
                            printSpeed = -1;
                        }
                    }
                    break;
                }
            }
        }

        Main.Forward(id,printSpeed);
        Main.Rotate(id,printRotate);
    }

    public void attack(){
        if (attack.attackType == AttackType.BLOCK){
            // 直接去目标点就可以了
            route.gotoTarget();    // 这里要判断与机器人的碰撞情况
            return;
        }

        // 判断周围有无敌人，获取需要攻击的敌人位置
        Point tar = attack.getAttackEnemy();
        if (tar == null){
            // 没有敌人，去到目标点
            if (attack.status == AttackStatus.ATTACK){
                // 上一次是攻击模式，可能走了很远了，需要重新寻路
                attack.calcRouteFromNow();
                // 切换状态
                attack.status = AttackStatus.ROAD;
                route.calcParamEveryFrame();    // 通用参数
                calcMoveEquation();     //  运动方程
            }
            if (attack.status == AttackStatus.ROAD){
                // 如果是在路上，要判断是否到达
                if (pos.calcDistance(attack.target)<0.3){
                    // 切换为等待姿态
                    attack.status = AttackStatus.WAIT;
                    attack.arriveFrame = Main.frameID;
                }else {
                    // 没有达到，就继续前进
                    route.gotoTarget();
                }
            }
            if (attack.status == AttackStatus.WAIT){
                if (Main.frameID - attack.arriveFrame > Attack.maxWaitFps){
                    attack.changeTarget();
                }
                route.gotoTarget();     // 等待模式，一直堵在目标点
            }
        }else {
            Main.printLog("find enemy" + tar);

            if (attack.attackType == AttackType.RUSH){
                double dis = route.target.calcDistance(pos);
                if (dis>Attack.maxFarToTarget){
                    // 这种模式超过一定距离就回去
                    route.gotoTarget();
                    return;
                }
            }

            // 有敌人，攻击敌人
            attack.status = AttackStatus.ATTACK;    // 切换为攻击模式
            route.bumpEnemy(tar);
        }
    }

    public HashSet<RadarPoint> handleEnemy() {
        HashSet<Station> resets = new HashSet<>();
        for(Station st:Main.blockStations){
            // 先遍历每个不正常的工作台，如果该机器人能看到，先恢复正常
            double dis = pos.calcDistance(st.pos);
            if (dis > 15) continue;  // 距离太长，中间可能有阻挡
            Line line = new Line(pos,st.pos);
            if (line.roadNoWall()){
                // 视野通畅，不会有机器人卡着
                // 都没有墙，那么也不会挡着机器人
                resets.add(st);
                st.place = StationStatus.EMPTY;
            }
        }
        Main.blockStations.removeAll(resets);   // 先恢复
        enemy = getTrueEnemy();
        Main.printLog("blue = " + Main.isBlue + " pos = "+pos);
        Main.printLog("enemy" + enemy);
        return enemy;
    }

    private HashSet<RadarPoint> getTrueEnemy() {
        // 可能某些点发生误判，要进行去重
        HashSet<RadarPoint> rps = getRadarPoint();
        HashSet<RadarPoint> enemy = new HashSet<>();
        if (rps.size()>0){
            for (RadarPoint rp : rps) {
                Point point = rp.getPoint();
                if (isFriend(point) || point.isWall()){
                    continue;
                }
                enemy.add(rp);  // 既不是友军，也不是墙，那就是敌人
            }
        }
        if (enemy.size() >0){
            Main.printLog(enemy);

            for (Robot robot : zone.robots) {
                Main.printLog(robot);
            }
//            for (int i = 0; i < 360; i++) {
//                System.out.print(radar[i]);
//                System.out.print(",");
//            }
        }

        return enemy;
    }

    private boolean isFriend(Point point) {
        // 该点是否是自己人

        for (Robot robot : zone.robots) {
            if (robot.pos.closeTo(point)){
                return true;    //是友军
            }
        }
        return false;
    }

    public void releaseSrc() {
        // 释放 src 资源
        srcStation.bookPro = false;       //解除预定
        srcStation.bookNum--;       //
    }

    public void releaseDest() {
        // 释放 dest 资源
        destStation.bookRawNum[srcStation.type] --;
        if (destStation.bookRawNum[srcStation.type] == 0){
            // 没人占用在释放锁
            destStation.bookRow[srcStation.type] = false;   //解除预定
        }

        destStation.bookNum--;       //解除预定
    }
    public HashSet<RadarPoint> getRadarPoint() {
        ArrayList<Point> points = new ArrayList<>();
        ArrayList<Double> radarList = new ArrayList<>();
        double degree = pi * 2 / 360;
        double azimuthAngle; //方位角
        for (int i = 0; i < 360 / step; i++) {

            double angle = turn + degree * i * step;
            if (angle > 2 * pi) {
                azimuthAngle = angle % pi;
            } else if (angle > pi) {
                azimuthAngle = angle % pi - pi;
            } else {
                azimuthAngle = angle;
            }
            points.add(new Point(pos.x + Math.cos(azimuthAngle) * radar[step * i],
                    pos.y + Math.sin(azimuthAngle) * radar[step * i]));
            radarList.add(radar[step * i]);
        }

        points.add(0, points.get(points.size()-1));
        points.add(points.get(1));
        radarList.add(0, radarList.get(radarList.size()-1));
        radarList.add(radarList.get(1));

        HashSet<RadarPoint> radarPoints = new HashSet<>();


        for (int i = 1; i < points.size() - 1; i++) {
            if(radarList.get(i) * 2 > radarList.get(i-1) + radarList.get(i+1)){
                continue;//如果外凸 忽略
            }
            RadarPoint centerPos = Point.getCenterPos(points.get(i - 1).x, points.get(i - 1).y, points.get(i).x,
                    points.get(i).y, points.get(i + 1).x, points.get(i + 1).y);
            if (centerPos != null){
                radarPoints.add(centerPos);
            }
        }
        return radarPoints;
    }
    public void printRoute() {
        Main.printLog("src:"+srcStation +" -> dest:"+destStation);
    }

    public void leaveWall() {
        // 被墙阻塞，离开墙体
        Point wall = getNearWall();
        if (wall == null){
            blockByWall = false;
            recoveryPath(); // 离开了墙体，恢复路径
        }else {
            Line line = new Line(wall,pos);
            Point tp = line.getPointDis2dest(2);    // 获取一个远离墙的临时点
            route.gotoTmpPlace(tp);
            Main.Forward(id,route.printLineSpeed);
            Main.Rotate(id,route.printTurnSpeed);
        }

    }

    private Point getNearWall() {
        Pos pos1 = Astar.Point2Pos(pos);
        // 周围2格有墙就算
        for (int i = -1; i <=1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i != 0 && j != 0) continue;     // 不考虑对角情况
                Point p = Point.getRealWall(pos1.x + i, pos1.y + j);
                if (p!= null){
                    return p;
                }
            }
        }
        return null;
    }


}


