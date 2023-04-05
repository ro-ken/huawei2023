package com.huawei.codecraft.util;

/**
 * @Author: ro_kin
 * @Data:2023/3/17 21:52
 * @Description: TODO
 */
public class Line {
    public Point left;
    public Point right;
    private double k = 0;
    private double b = 0;
    private Point vector;
    // y = kx + b
    private double dis = 0;

    public Line() {
    }

    public Line(Point left, Point right) {
        this.left = left;
        this.right = right;
    }

    // 计算两条直线交点
    public Point calcIntersectionPoint(Line other) {
        double k1 = getK();
        double b1 = getB();
        double k2 = other.getK();
        double b2 = other.getB();

        double x = (b2-b1)/(k2-k1);
        double y = k1 * x + b1;

        return new Point(x,y);
    }

    public double getY(double x){
        double k = getK();
        double b = getB();
        double y = k*x + b;
        return y;
    }

    private double getB() {
        if (b != 0) return b;
        b = left.y - getK() * left.x; //y = k(x-x1)+y1;
        return b;
    }

    // y = kx + b
    private double getK(){
        if (k!= 0) return k;
        k = (right.y - left.y)/(right.x - left.x);// (y2-y1)/(x2-x1);
        return k;
    }

    public void setValue(Point left, Point right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return
                "(src=" + left +
                ", dest=" + right +
                ')';
    }

    public Point getFixPoint(double x) {
        // 记录x对应点的中心坐标
        double y = getY(x);
        Point p = Point.fixPoint2Center(x,y);
        return p;
    }

    public Point getPointDis2src(double dis) {
        // 返回距离原点 多少距离的点
        // 如果线段不够长，返回右端点
        if (getDis()<dis){
            return right;
        }
        Point vec = vector();
        double d = vec.norm(); // 直线的长度
        double dx = vec.x / d; // 直线的单位向量在 x 方向上的分量
        double dy = vec.y / d; // 直线的单位向量在 y 方向上的分量
        Point res = new Point();
        res.x = left.x + dx * dis; // 求解 point2 在 x 方向上的坐标
        res.y = left.y + dy * dis; // 求解 point2 在 y 方向上的坐标

        return res;
    }

    private double getDis() {
        if (dis == 0){
            dis = left.calcDistance(right);
        }
        return dis;
    }

    public Point vector() {
        if (vector == null) {
            vector = left.calcVector(right);
        }
        return vector;
    }

    public Point getPointDis2dest(double dis) {
        // 距离选择终点dis距离的点
        Point vec = vector();
        double d = vec.norm(); // 直线的长度
        double dx = vec.x / d; // 直线的单位向量在 x 方向上的分量
        double dy = vec.y / d; // 直线的单位向量在 y 方向上的分量
        Point res = new Point();
        res.x = right.x + dx * dis; // 求解 point2 在 x 方向上的坐标
        res.y = right.y + dy * dis; // 求解 point2 在 y 方向上的坐标

        return res;
    }
}

