package com.huawei.codecraft.util;

import com.huawei.codecraft.Main;

import java.util.Objects;

// 可表示点，也可表示原点为0,0的向量
public class Point{
    public double x,y;

    public Point() {

    }

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
        return "Point{" +
                "x=" + x +
                ", y=" + y +
                '}';
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
        return x < 1 || x > 49 || y < 1 || y > 49;
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


}