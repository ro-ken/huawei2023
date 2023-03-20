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
    public static final double maxRotate = pi;//rad/s
    public static final int maxForce = 250;//N
    public static final int maxRotateForce = 50;//N*m

    public static final double canForwardRad = pi/2 ; // 行走最小角度偏移量
    public static final double maxForwardRad = pi/4 ; // 最大速度的最小角度
//    public static final double angleSpeedOffset = 0.1 ; //(rad)最大误差 0.003
    public static double emptyA;     //加速度
    public static double fullA;     //加速度

    public static double emptyRotateA;     //角加速度
    public static double fullRotateA;     //角加速度
    public static double emptyMinAngle; // 加速减速临界值 ,空载
    public static double fullMinAngle; // 加速减速临界值 ，满载
    public static Line rotateSpeedEquation; // 转向速度方程


    int id;
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

    // 碰撞相关
    public boolean isTempPlace = false;   // 是否去往临时目的地，避免碰撞
    public Point tmpPos;
    public Line topLine;   // 轨迹上面的线
    public Line belowLine;     // 轨迹下面的线
    public double vectorNearOffset = 0.1; // 小于这个角度认为直线重合
    public double tmpPlaceOffset = 1.2; // 半径乘子 偏移系数，单位 个，临时距离向右偏移多少
    public double minDistanceForWall = 4; // 半径乘子，偏移系数，单位 个，
    public double arriveMinDistance = 2;//半径乘子，和目的地的最小判定距离
    public static final boolean judgeWidth = true;


    Route route;

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
        id=robotId;
        topLine = new Line();
        belowLine = new Line();
        rotateSpeedEquation = new Line(new Point(maxForwardRad,maxSpeed),new Point(pi/2,0));
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
            double t2 = 1-Math.sqrt(t1);
            return t2 * (1-minRate) + minRate;
        }
    }

    // 选一个最佳的工作站
    public void selectBestStation() {
//        Station station = selectClosestStation();
        Station station = selectTimeShortestStation();
//        Station maxStation = selectBestValueStation();
        if (station == null){
            Main.printLog("no available station ! wait...");
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
            double dis = station.pos.calcDistance(pos);
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
            double dis = station.pos.calcDistance(pos);
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
        double v0 = Main.norm(lineVx,lineVy);
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

    // 计算路线
    public void calcRoute() {
        if (nextStation != null){
            route = new Route(nextStation.pos.x,nextStation.pos.y,this);
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

        // 临时目的地判断是否到达
        if (isTempPlace){
            if (route.isArriveTarget()){
                isTempPlace = false;
                if (nextStation == null) return;
                route.target.set(nextStation.pos);// 重新设置目的地
            }
        }

//        route.rush();
        route.rush2();
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


    // 判断目标点是否到达工作台
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


    public double getMinAngleDistanceByCurSpeed() {
        double a = getAngleAcceleration();
        double angle = angV * angV / (2*a);
        return angle;
    }

    // 买入的商品是否有时间售出
    public boolean canBugJudge() {
        int needFPS = calcFpsToPlace(srcStation.pos.calcDistance(srcStation.availNextStation.pos));
        int leftFps = Main.duration - Main.frameID - 100;   // 两s误差,后期可调整
        return leftFps > needFPS;
    }


    //计算机器人的轨迹方程，也就两条平行线
    public void calcMoveEquation() {
        if (route.vector.x == 0) return;// 不能是垂直的情况，在调用此函数之前事先要做出判断 todo
        double radius = getRadius();
        Point[] src = getPoints(pos.x,pos.y,radius);
        Point[] dest = getPoints(route.target.x,route.target.y,radius);
        topLine.setValue(src[0],dest[0]);
        belowLine.setValue(src[1],dest[1]);

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

    // 碰撞计算
    public void calcBump(Robot other) {
        if (route.vector.x == 0 || other.route.vector.x == 0) return;    //应该不会是垂直情况



        // 检测两个机器人轨迹是否有交集
        if (routeBumpDetect(other)){
            double deltaVector = route.calcDeltaAngle(other.route.vector);
            Main.printLog("deta "+deltaVector);
            if (deltaVector < vectorNearOffset){
                // 方向同向，后车饶
                Point vec = new Point(other.pos.x - pos.x,other.pos.y - pos.y);
                Main.printLog("vec det" + route.calcDeltaAngle(vec));
                if (route.calcDeltaAngle(vec) < pi/2){
                    //锐角，说明自己在后面
                    if (!setTmpPlace()){
                        setTmpPlace(false);//向右不能转，改为向左转
                    }
                }else {
                    if (!other.setTmpPlace()){
                        other.setTmpPlace(false);//改为向左转
                    }
                }

            }else if(deltaVector>pi-vectorNearOffset){
                // 方向相反，双车都绕
                setTmpPlace();
                other.setTmpPlace();

            }else if (ifRangeConflict(other)){

                setTmpPlace();
                other.setTmpPlace();
            }
        }
    }

    private boolean setTmpPlace() {
        return setTmpPlace(true);
    }

    //  设置临时目的地，默认设置中点向右偏移
    private boolean setTmpPlace(boolean right) {
        double x = (route.target.x + pos.x)/2;
        double y = (route.target.y + pos.y)/2;
        Point[] points = getPoints(x, y, getRadius() * tmpPlaceOffset);
        // 默认往右转
        if (route.vector.x>0){
            tmpPos = right?points[1]:points[0];
        }else {
            tmpPos =  right?points[0]:points[1];
        }
        if (!posIsAllow(tmpPos)){
            return false;
        }
        // 重新设置轨迹
        route.target.set(tmpPos);
        isTempPlace = true;

        return true;
    }

    private boolean posIsAllow(Point tmpPos) {
        // 距离边界太近，则不合法
        double dis = minDistanceForWall * getRadius();
        if (tmpPos.x <= dis || tmpPos.x >= 50 - dis){
            return false;
        }
        return !(tmpPos.y <= dis) && !(tmpPos.y >= 50 - dis);
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



    // 检测路线是否有重叠区域
    private boolean routeBumpDetect(Robot other) {
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

}


