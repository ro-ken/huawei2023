package com.huawei.codecraft.util;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.core.Station;
import com.huawei.codecraft.way.Astar;
import com.huawei.codecraft.way.Pos;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

// 可表示点，也可表示原点为0,0的向量
public class Point{
    public double x,y;

    // 用的多的两个点设为静态变量
    public static final Point vecX = new Point(1,0);
    public static final Point vecY = new Point(0,1);

    public Point() { }

    public Point(Point point) {
        x = point.x;
        y = point.y;
    }

    // 如果这个点上有工作站，记录一下


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return Double.compare(point.x, x) == 0 && Double.compare(point.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x +
                ", " + y +
                ')';
    }

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double calcDistance(Point oth){
        return calcDistance(oth.x,oth.y);
    }

    public double calcDistance(double ox, double oy) {
        return Math.sqrt(Math.pow(ox-x,2) + Math.pow(oy-y,2));
    }

    // 计算两点向量
    public Point calcVector(Point dest){
        return new Point(dest.x-x, dest.y-y);
    }

    // 计算两点的叉积
    public double calcDot(Point oth){
        return x*oth.y - oth.x*y;
    }
    
    // 当前坐标是否靠近墙体
    public boolean nearWall() {
        Pos pos = Astar.Point2Pos(this);
        // 周围2格有墙就算
        int[] off = new int[]{0,1,-1,2,-2};
        for (int i = 0; i < off.length; i++) {
            for (int j = 0; j < off.length; j++) {
                if (posIsWall(pos.x + off[i], pos.y + off[i])){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean posIsWall(int x, int y) {
        if (x <0 || y<0 || x>99 || y>99) return true;
        return Main.wallMap[x][y] == -2;
    }


    public void set(double x,double y){
        this.x = x;
        this.y = y;
    }

    public void set(Point p){
        set(p.x,p.y);
    }

    // 计算自己与其他的角度
    public double calcDeltaAngle(Point vec) {
        double cosTheta = calcDeltaCos(vec);
        double theta = Math.acos(cosTheta); // 将余弦值转化为弧度值
        return theta;
    }


    // 计算自己与其他的角度的cos
    public double calcDeltaCos(Point vec) {
        double dotProduct = dotProduct(vec); // 计算点积
        double normA = norm(); // 计算向量a的模长
        double normB = vec.norm(); // 计算向量b的模长

        double cosTheta = dotProduct / (normA * normB); // 计算余弦值
        return cosTheta;
    }

    public double norm() {
        return norm(x, y);
    }

    // 计算向量的模长
    public static double norm(double x,double y) {
        return Math.sqrt(x*x + y*y);
    }
    // 计算向量的点积
    public double dotProduct(Point vec) {
        return dotProduct(x,y,vec.x,vec.y);
    }
    // 计算向量的点积
    public static double dotProduct(double x1,double y1, double x2,double y2) {
        return x1 * x2 + y1 * y2;
    }


    // 获取从源到目的地的一些列路径
    public Queue<Point> getPath(Point target) {
        // todo 到时候算法进行替换
        Queue<Point> path = new LinkedList<>();
//        path.add(this);
        path.add(target);
        return path;
    }

    public double distanceToSecond(boolean isEmpty, Point other){
        //两种情况， 加速，匀速，减速  or  加速 ，减速
        double minDistance = isEmpty? Station.emptyMinDistance:Station.fullMinDistance;
        double a = isEmpty ? Robot.emptyA:Robot.fullA;
        double distance = calcDistance(other);
        double second ;
        if (distance <= minDistance){
            second = Math.sqrt(distance/a)*2;   // t = sqrt(2*d/2 /a) * 2
        }else {
            second = Math.sqrt(minDistance/a)*2 + (distance-minDistance)/a;
        }
        return second;
    }

    // 距离换算成时间, 0 -> v -> 0
    public int distanceToFps(boolean isEmpty, Point p){
        double second = distanceToSecond(isEmpty,p);
        int fps = (int) (second * 50);
        return fps;
    }

    public int[] fixPoint2Map() {
        // 将 坐标 修正为 map 的中心点的下标
        int[] res = new int[2];
        res[0] = (int) (x / 0.5);
        res[1] = (int) ((50-y) / 0.5);

        return res;
    }

    public Point fixPoint2Center() {
        // 将 坐标 修正为 map 的中心点的坐标
        return fixPoint2Center(x,y);
    }

    public static Point fixPoint2Center(double x,double y) {
        // 将 坐标 修正为 map 的中心点的坐标
        double x1 = fixAxis2Center(x);
        double y1 = fixAxis2Center(y);

        return new Point(x1,y1);
    }

    // 修正一个点到中点
    public static double fixAxis2Center(double t){
        return ((int) (t / 0.5)) * 0.5 + 0.25;
    }

    public boolean nearStation() {
        // 附近是否有工作台
        Pos pos = Astar.Point2Pos(this);
        // 周围2格有墙就算
        int dis = 3;
        for (int i = -dis; i <= dis; i++) {
            for (int j = -dis; j <= dis; j++) {
                if (posIsStation(pos.x + i, pos.y + j)){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean posIsStation(int x, int y) {
        if (x <0 || y<0 || x>99 || y>99) return false;

        return Main.wallMap[x][y] <=50 && Main.wallMap[x][y] >=0;
    }

    public void set(Pos pos) {
        Point p= Astar.Pos2Point(pos);
        set(p);
    }


    public static double epsion = 0.0001; // 半径偏差区间

    //若识别出来的点为机器人，则返回坐标+状态
    public static RadarPoint getCenterPos(double x1, double y1, double x2, double y2, double x3, double y3){
        double a = 2 * (x2 - x1);
        double b = 2 * (y2 - y1);
        double d = 2 * (x3 - x2);
        double e = 2 * (y3 - y2);
        double divisor = b * d - e * a;
        if (divisor == 0)
            return null;
        double c = x2 * x2 + y2 * y2 - x1 * x1 - y1 * y1;
        double f = x3 * x3 + y3 * y3 - x2 * x2 - y2 * y2;
        double x = (b * f - e * c) / divisor;
        double y = (d * c - a * f) / divisor;
        double r = Math.sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));// 半径
        if (Robot.emptyRadius - epsion < r && r < Robot.emptyRadius + epsion){
            double x_d = (double) Math.round(x*100) / 100;
            double y_d = (double) Math.round(y*100) / 100;
            return new RadarPoint(x_d, y_d, 0);
        }else if (Robot.fullRadius - epsion < r && r < Robot.fullRadius + epsion){
            double x_d = (double) Math.round(x*100) / 100;
            double y_d = (double) Math.round(y*100) / 100;
            return new RadarPoint(x_d, y_d, 1);
        }
        return null;
    }

    public ArrayList<Station> getNearStations() {
        //以自己为中心，获取周围的所有的工作站
        Pos pos = Astar.Point2Pos(this);
        ArrayList<Station> stations = new ArrayList<>();
        int dis = 1;
        for (int i = -dis; i <= dis; i++) {
            for (int j = -dis; j <= dis; j++) {
                if (posIsStation(pos.x + i, pos.y + j)){
                    stations.add(Main.pointStaMap.get(Astar.Pos2Point(new Pos(pos.x + i,pos.y + j))));
                }
            }
        }

        return stations;
    }

    public boolean inCorner() {
        // 判断工作站是否和墙靠得很近，
        // 一个机器人占了以后能否撞开
        boolean fx = false;
        boolean fy = false;
        int step = 2;   // 判断
        for (int i = -step; i <= step; i++) {
            Point nx = new Point(x+i,y);
            Point ny = new Point(x,y+i);
            if (nx.isWall()){
                fx = true;
            }
            if (ny.isWall()){
                fy = true;
            }
        }
        // 判断墙角是前后有墙 并且左右有墙
        return fx && fy;
    }

    public boolean closeTo(Point point) {
        // 两个点很接近
        double dis = calcDistance(point);
        return dis <= 0.3;
    }

    public boolean isWall() {
        return posIsWall(x,y);
    }

    public static boolean posIsWall(double x, double y) {
        if (notInMap(x,y)){
            return true;
        }
        // 找出 x,y 属于哪一个点的区域
        Pos pos = Astar.Point2Pos(new Point(x, y));
        // -2 对应的是墙
        return Main.wallMap[pos.x][pos.y] == -2;
    }

    public static boolean notInMap(double x, double y) {
        // 是否不在地图内
        boolean flag1 = x <= 0 || y <= 0;
        boolean flag2 = x >= 50 || y >= 50;

        return flag1 || flag2;
    }
}