package com.huawei.codecraft.util;

/**
 * @Author: ro_kin
 * @Data:2023/3/17 21:52
 * @Description: TODO
 */
public class Line {
    public Point left;
    public Point right;

    public Line() {
    }

    public Line(Point left, Point right) {
        this.left = left;
        this.right = right;
    }

    // 计算两条直线交点
    public Point calcIntersectionPoint(Line other) {
        double k1 = getK();
        double b1 = getB(k1);
        double k2 = other.getK();
        double b2 = other.getB(k2);

        double x = (b2-b1)/(k2-k1);
        double y = k1 * x + b1;

        return new Point(x,y);
    }

    public double getY(double x){
        double k = getK();
        double b = getB(k);
        double y = k*x + b;
        return y;
    }

    private double getB(double k) {
        double b = left.y - k * left.x; //y = k(x-x1)+y1;
        return b;
    }

    // y = kx + b
    public double getK(){
        double k = (right.y - left.y)/(right.x - left.x);// (y2-y1)/(x2-x1);
        return k;
    }

    public void setValue(Point left, Point right) {
        this.left = left;
        this.right = right;
    }

    public Point getFixPoint(double x) {
        // 记录x对应点的中心坐标
        double y = getY(x);
        Point p = Point.fixPoint2Center(x,y);
        return p;
    }
}

