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
    public Line(Line line) {
        left = line.left;
        right = line.right;
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
    private double getDis() {

        return left.calcDistance(right);
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

    public Point vector() {
        return left.calcVector(right);
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

    @Override
    public String toString() {
        return
                "(left=" + left +
                        ", right=" + right +
                        ')';
    }

    public Point getFixPoint(double x) {
        // 记录x对应点的中心坐标
        double y = getY(x);
        Point p = Point.fixPoint2Center(x,y);
        return p;
    }

    public Point getNearBumpWall() {
        // 查看此条线段最近的墙体
        if (posIsWall(left)){
            return left.fixPoint2Center();
        }
        double offset = left.x < right.x ? 0.26 : -0.26;  // 刚好是下一个点
        double x = left.x;

        int times = 0;
        while (Math.abs(x - right.x) >= 0.5){
            if (times > 10){
                return null;
            }else {
                times ++;
            }

            Point wall = getWallByX(x);
            if (wall != null) return wall;
            x =Point.fixAxis2Center(x)+offset;

        }
        return null;
    }

    public static boolean posIsWall(Point point) {
        return point.isWall();//posIsWall(point.x,point.y);
    }

    // 找出直线在x方格内所有的最近的墙
    public Point getWallByX(double x) {
        double offset = left.x < right.x ? 0.24 : -0.24;
        Point start = getFixPoint(x);
        Point end = getFixPoint(start.x + offset);
        Point wall = getWallBy2Point(start,end);
        return wall;
    }

    public static Point getWallBy2Point(Point start, Point end) {
        // 两个点的x都是相同的，判断夹住的point
        if (start.equals(end) && posIsWall(start)) return start;

        double offset = start.y < end.y ? 0.5 : -0.5;
        Point tmp = new Point(start);
        int times = 0;

        while (Math.abs(tmp.y - end.y)>=0.5){
            if (posIsWall(tmp)){
                return tmp;
            }

            if (times > 10){
                return null;
            }else {
                times ++;
            }

            tmp.y +=offset;
        }
        // 前面没判断结尾
        if (posIsWall(end)) return end;
        return null;
    }

}

