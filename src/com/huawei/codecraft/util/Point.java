package com.huawei.codecraft.util;

public class Point{
    public double x,y;

    public Point() {

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
        return Math.sqrt(Math.pow(oth.x-x,2) + Math.pow(oth.y-y,2));
    }
    public void set(double x,double y){
        this.x = x;
        this.y = y;
    }

    public void set(Point p){
        set(p.x,p.y);
    }


    public double calcDistance(double ox, double oy) {
        return Math.sqrt(Math.pow(ox-x,2) + Math.pow(oy-y,2));
    }

    // 计算两点向量
    public Point calcVector(Point dest){
        return new Point(dest.x-x, dest.y-y);
    }

    public double calcDot(Point oth){
        return x*oth.y - oth.x*y;
    }

}