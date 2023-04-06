package com.huawei.codecraft.util;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.core.Station;
import com.huawei.codecraft.way.Astar;
import com.huawei.codecraft.way.Pos;

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

}