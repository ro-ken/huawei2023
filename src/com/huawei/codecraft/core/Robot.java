package com.huawei.codecraft.core;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.Line;
import com.huawei.codecraft.util.Path;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Astar;
import com.huawei.codecraft.way.Pos;

import java.util.ArrayList;
import java.util.HashSet;

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
    public static final double maxRotate = pi;//rad/s
    public static final int maxForce = 250;//N
    public static final int maxRotateForce = 50;//N*m

    public static  double canForwardRad = pi/2 ; // 行走最小角度偏移量
    public static  double maxForwardRad = pi/4 ; // 最大速度的最小角度
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
    public int StationId; // -1 无 ，从0 开始 表示第几个工作台
    public int carry;  // 携带物品 1-7
    public double timeValue;  // 时间价值系数 [0.8 - 1]
    public double bumpValue;       //碰撞价值系数 [0.8 - 1]
    public double angV; //角速度  弧度/s 正逆，负顺
    public double lineVx;    //线速度， m/s
    public double lineVy;    //线速度， m/s
    public double turn; //朝向 [-pi,pi] 0朝向右，pi/2  朝上
    public Point pos;
//    public double x, y; //坐标

    public Station nextStation;    // null : no target to go
    public Station srcStation;
    public Station destStation;

    public WaterFlow waterFlow; // 处于哪条流水线

    public Station lastStation; // 当前处于那个工作站，供决策使用，到达更新

    public Point start; // 出生的地点
    public Point midPoint = new Point(); // 记录相撞坐标的中点
    // 碰撞相关
    public boolean isTempPlace = false;   // 是否去往临时目的地，避免碰撞
    public Point tmpPos;
    public Line topLine;   // 轨迹上面的线
    public Line belowLine;     // 轨迹下面的线
    public Line midLine;     // 轨迹下面的线

    public boolean tmpSafeMode = false;    // 是否去临时安全点
    public boolean avoidBumpMode = false;   // 与其他机器人阻塞，临时避障模式
    public int avoidBumpModeFps;
    public boolean inSafePlace = false;    // 是否到达临时点

    public double lastDis = 100000; // 在临时点判断是否距离另外的机器人越来越远

    public int lastDisFps = 0; // 越来越远 经过了几帧


    public double blockFps = 0;    // 目前阻塞的帧数
    //下面参数可调
    public static final double minDis = 0.2; // 判定离临时点多近算到达
    public static final int minLastDisFps = 5; // 越来越远 经过了多少帧，小车开始走
    public static final double maxSpeedCoef = 1.5;
    public static final double stationSafeDisCoef = 2;    // 工作站的安全距离距离系数
    public static final int cacheFps = 50;     // 判断是否要送最后一个任务的临界时间 > 0
    public static final double blockJudgeSpeed = 0.5 ;    // 判断机器人是否阻塞的最小速度
    public static final int blockJudgeFps = 20 ;    // 则阻塞速度的fps超过多少判断为阻塞 ，上面speed调大了这个参数也要调大一点
    public static final int maxWaitBlockFps = 50 * 3 ;    // 等待超过多长时间目标机器人没有来，就自行解封  todo 重要参数

    public static final double robotInPointDis = 0.2 ;    // 判断机器人到达某个点的相隔距离
    public static final double detectWallWideCoef = 1.0 ;    // 半径乘子，判断从圆心多远的地方发出的射线会经过障碍物  todo 重要参数
    public static final double arriveBPDis = 1.0;     // 其他小车躲避的点

    public Robot winner;
    public HashSet<Robot> losers = new HashSet<>(); // 要避让我的点
    public Route route;

    static {
        emptyA = calcAcceleration(emptyRadius);
        fullA = calcAcceleration(fullRadius);

        emptyRotateA = calcRotateAcce(emptyRadius);
        fullRotateA = calcRotateAcce(fullRadius);

        emptyMinAngle = calcMinAngle(true);
        fullMinAngle = calcMinAngle(false);
    }



    public Robot(int stationId, double x, double y, int robotId) {
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

    @Override
    public String toString() {
        return "Robot{" +
                "id=" + id +
                ", pos=" + pos +
                '}';
    }

    private static double calcAcceleration(double radius) {
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
        Point[] src = getPoints(pos.x,pos.y,radius);
        Point[] dest = getPoints(route.next.x,route.next.y,radius);
        topLine.setValue(src[0],dest[0]);
        belowLine.setValue(src[1],dest[1]);
        midLine.setValue(pos,route.next);

    }

    private static double calcRotateAcce(double radius) {
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
//                path = nextStation.paths.getPath(isEmpty,lastStation.pos);
            }
            Main.printLog(path);
            Main.printLog(pos1);
            route = new Route(nextStation.pos,this,path,pos1);
        }
    }


    public void recoveryPath() {

        clearWinner();
        // 重新寻找新路径
        if (nextStation.paths == null) return;
        ArrayList<Point> path = nextStation.paths.getPath(carry == 0,pos);
        HashSet<Pos> pos1 = nextStation.paths.getResSet(carry==0,pos);
        path = Path.reversePath(path);
        route = new Route(nextStation.pos,this,path,pos1);
        route.calcParamEveryFrame();    // 通用参数
        calcMoveEquation();     //  运动方程
        Main.printLog(pos + ":recovery path"+path);
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
            calcRoute();
        }else {
            if (waterFlow != null) {
                useWaterFlowChangeMode();
//                lastStation = nextStation;
            }
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

    // 知道点，获取左右两坐标,输出点顺序为先上后下
    public Point[] getPoints(double x,double y,double distance){

        double[] direction = new double[]{route.vector.x,route.vector.y};   // 方向向量
        double[] points = new double[4];
        Point[] p = new Point[2];
        // 求解过程
        double tmp = direction[0];
        direction[0] = -direction[1];
        direction[1] = tmp;
        double d = Math.sqrt(direction[0] * direction[0] + direction[1] * direction[1]); // 直线的长度
        double dx = direction[0] / d; // 直线的单位向量在 x 方向上的分量
        double dy = direction[1] / d; // 直线的单位向量在 y 方向上的分量
        points[0] = x + dx * distance; // 求解 point2 在 x 方向上的坐标
        points[1] = y + dy * distance; // 求解 point2 在 y 方向上的坐标

        direction[0] = -direction[0];
        direction[1] = -direction[1];
        dx = direction[0] / d; // 直线的单位向量在 x 方向上的分量
        dy = direction[1] / d; // 直线的单位向量在 y 方向上的分量
        points[2] = x + dx * distance; // 求解 point2 在 x 方向上的坐标
        points[3] = y + dy * distance; // 求解 point2 在 y 方向上的坐标
        if (points[1]<points[3]){
            p[0] = new Point(points[2],points[3]);
            p[1] = new Point(points[0],points[1]);
        }else{
            p[0] = new Point(points[0],points[1]);
            p[1] = new Point(points[2],points[3]);
        }

        return p;
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

        if (avoidBumpMode){
            boolean far = true;
            for (Robot oth : zone.robots) {
                if (oth == this) continue;
                double dis = pos.calcDistance(oth.pos);
                if (dis < 3){
                    far = false;
                }
            }
            if (!far){
                goToEmptyPlace();
            }else {
                avoidBumpMode = false;
                Main.printLog(11111);
                recoveryPath();     // 恢复路径
            }
        }

        if (tmpSafeMode){

            if (!inSafePlace){
                // 如果未到安全点，要判断是否到达
//                if (route.pathIndex<2) return;
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


        route.rush2();

        route.deletePos();  // 以走过的点要删除，防止发生误判
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
        Point wall = Route.getNearBumpWall(line);
        return wall == null;
    }

    // 选一个最佳的工作站
    public void selectBestStation() {
        if (waterFlow == null){
            return; // 机器人没有流水线，暂停生成
        }else {
            waterFlow.scheduler(this);
//            taskIsOK();
        }

    }
    
    public void setSrcDest(Station src, Station dest) {
        nextStation = srcStation = src;
        destStation = dest;

        Main.printLog(this + " src, "+srcStation + " dest, " + destStation);
        srcStation.bookPro = true;      // 预定位置

        if (destStation.type <= 7)  {   // 8,9 不需要预定
            destStation.bookRow[srcStation.type] = true;
        }

        src.bookNum ++;
        if (dest.type <= 6){
            waterFlow.halfComp.put(dest.type,waterFlow.halfComp.get(dest.type) +1);    // 原料数 +1
        }

        calcRoute();
    }

    public boolean arriveBasePoint() {
        Point pre = baseLine.left;
        boolean arrive =  isArrivePoint(pre,basePoint);
        Main.printLog("arrive:" + arrive);
        return arrive;
    }

    public boolean isArrivePoint(Point pre, Point next) {
        double dis1 = pre.calcDistance(next);
        double dis2 = pre.calcDistance(pos);
//        Main.printLog("bp dis"+dis1);
//        Main.printLog("pre" + pre + "pos" + pos);
//        Main.printLog("pos dis"+dis2);
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
        lastStation = nextStation;
    }

    public boolean blockDetect() {
        // todo 后面判断是否周围有墙或者机器人
        // 阻塞检测，在某个点阻塞了多少帧，重新设置路径

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


//
//    public void setNewPath() {
//        // 重新寻找新路径
//        if (nextStation.paths == null) return;
//        tmpSafeMode = false;
//        winner = null;
//        ArrayList<Point> path = nextStation.paths.getPath(carry == 0,pos);
//        path = Path.reversePath(path);
//        route = new Route(nextStation.pos,this,path);
//        route.calcParamEveryFrame();    // 通用参数
//        calcMoveEquation();     //  运动方程
//        Main.printLog("blocked renew path"+path);
//    }


    public void setNewPath() {

        avoidBumpMode = false;

        for (Robot oth : zone.robots) {
            if (oth == this) continue;
            double dis = pos.calcDistance(oth.pos);
            if (dis < 1.5){
                avoidBumpMode = true;
                avoidBumpModeFps = 0;
            }
        }

        if (!avoidBumpMode){
            if (tmpSafeMode){
                calcTmpRoute(route.target,winner);
            }else {
                Main.printLog(444444);
                recoveryPath();
            }
//        recoveryPath();
        }
    }

    public Point selectTmpSafePoint(Point dest, HashSet<Pos> posSet, Point midPoint) {
        Point sp = Astar.getSafePoint(carry == 0, pos, dest, posSet,midPoint);

        HashSet<Point> ps = new HashSet<>();
        for (Pos pos1 : posSet) {
            ps.add(Astar.Pos2Point(pos1));
        }
        Main.printLog(this);
        Main.printLog(ps);

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

}


