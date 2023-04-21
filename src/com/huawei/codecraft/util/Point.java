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


/**
 * ClassName: Point
 * Package: com.huawei.codecraft.util
 * Description: 可表示点，也可表示原点为0,0的向量
 */
public class Point {
    public double x, y;
    public static double epsion = 0.0001; // 半径偏差区间 用于判断是否为机器人

    // 用的多的两个点设为静态变量
    public static final Point vecX = new Point(1, 0);
    public static final Point vecY = new Point(0, 1);

    /**
     * 空参构造器
     */
    public Point() {
    }

    /**
     * 空参构造器
     *
     * @param point 点
     */
    public Point(Point point) {
        x = point.x;
        y = point.y;
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
        return "(" + x +
                ", " + y +
                ')';
    }

    /**
     * 构造器
     *
     * @param x x坐标
     * @param y y坐标
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 计算欧式距离
     *
     * @param oth 点
     * @return 距离
     */
    public double calcDistance(Point oth) {
        return calcDistance(oth.x, oth.y);
    }

    /**
     * 计算欧氏距离
     *
     * @param ox x位置
     * @param oy y位置
     * @return 距离
     */
    public double calcDistance(double ox, double oy) {
        return Math.sqrt(Math.pow(ox - x, 2) + Math.pow(oy - y, 2));
    }


    /**
     * 计算两点向量
     *
     * @param dest 点
     * @return 两点向量
     */
    public Point calcVector(Point dest) {
        return new Point(dest.x - x, dest.y - y);
    }


    /**
     * 计算两点的叉积
     *
     * @param oth 点
     * @return 两点的叉积
     */
    public double calcDot(Point oth) {
        return x * oth.y - oth.x * y;
    }


    /**
     * 判断当前坐标是否靠近墙体
     *
     * @return 当前坐标是否靠近墙体
     */
    public boolean nearWall() {
        Pos pos = Astar.Point2Pos(this);
        // 周围2格有墙就算
        int[] off = new int[]{0, 1, -1, 2, -2};
        for (int j : off) {
            for (int k : off) {
                if (posIsWall(pos.x + j, pos.y + k)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断当前坐标是否靠近墙体
     *
     * @return 当前坐标是否靠近墙体
     */
    public boolean nearWall2() {
        Pos pos = Astar.Point2Pos(this);
        // 周围2格有墙就算
        int[] off = new int[]{0, 1, -1};
        for (int j : off) {
            for (int k : off) {
                if (posIsWall(pos.x + j, pos.y + k)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断点是否为墙
     *
     * @param x x位置
     * @param y y位置
     * @return 点是否为墙
     */
    public boolean posIsWall(int x, int y) {
        if (x < 0 || y < 0 || x > 99 || y > 99) return true;
        return Main.wallMap[x][y] == -2;
    }

    /**
     * 获取真实墙的位置
     *
     * @param x x位置
     * @param y y位置
     * @return 真实墙的位置
     */
    public static Point getRealWall(int x, int y) {
        if (x < 0 || y < 0 || x > 99 || y > 99) return null;
        if (Main.wallMap[x][y] == -2) {
            return Astar.Pos2Point(new Pos(x, y));
        }
        return null;
    }


    /**
     * 设置位置
     *
     * @param x x位置
     * @param y y位置
     */
    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 设置位置
     *
     * @param p 点p
     */
    public void set(Point p) {
        set(p.x, p.y);
    }

    /**
     * 计算自己与其他的角度
     *
     * @param vec 角度
     * @return 自己与其他的角度
     */
    public double calcDeltaAngle(Point vec) {
        double cosTheta = calcDeltaCos(vec);
        double theta = Math.acos(cosTheta); // 将余弦值转化为弧度值
        return theta;
    }


    /**
     * 计算自己与其他的角度的cos
     *
     * @param vec 角度
     * @return 自己与其他的角度的cos
     */
    public double calcDeltaCos(Point vec) {
        double dotProduct = dotProduct(vec); // 计算点积
        double normA = norm(); // 计算向量a的模长
        double normB = vec.norm(); // 计算向量b的模长

        double cosTheta = dotProduct / (normA * normB); // 计算余弦值
        return cosTheta;
    }

    /**
     * 标准化
     *
     * @return 单位向量
     */
    public double norm() {
        return norm(x, y);
    }


    /**
     * 计算向量的模长
     *
     * @param x x坐标
     * @param y y坐标
     * @return 模长
     */
    public static double norm(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }


    /**
     * 计算向量的点积
     *
     * @param vec 向量
     * @return 向量的点积
     */
    public double dotProduct(Point vec) {
        return dotProduct(x, y, vec.x, vec.y);
    }


    /**
     * 计算向量的点积
     *
     * @param x1 x1
     * @param y1 y1
     * @param x2 x2
     * @param y2 y2
     * @return 向量的点积
     */
    public static double dotProduct(double x1, double y1, double x2, double y2) {
        return x1 * x2 + y1 * y2;
    }


    /**
     * 获取从源到目的地的一些列路径
     *
     * @param target 目的地点
     * @return 路径
     */
    public Queue<Point> getPath(Point target) {
        // todo 到时候算法进行替换
        Queue<Point> path = new LinkedList<>();
//        path.add(this);
        path.add(target);
        return path;
    }

    /**
     * 计算距离所用时间
     *
     * @param isEmpty 是否为空
     * @param other   点
     * @return 时间
     */
    public double distanceToSecond(boolean isEmpty, Point other) {
        //两种情况， 加速，匀速，减速  or  加速 ，减速
        double minDistance = isEmpty ? Station.emptyMinDistance : Station.fullMinDistance;
        double a = isEmpty ? Robot.emptyA : Robot.fullA;
        double distance = calcDistance(other);
        double second;
        if (distance <= minDistance) {
            second = Math.sqrt(distance / a) * 2;   // t = sqrt(2*d/2 /a) * 2
        } else {
            second = Math.sqrt(minDistance / a) * 2 + (distance - minDistance) / a;
        }
        return second;
    }

    /**
     * 距离换算成时间, 0 -> v -> 0
     *
     * @param isEmpty 是否为空
     * @param p       点
     * @return 时间
     */
    public int distanceToFps(boolean isEmpty, Point p) {
        double second = distanceToSecond(isEmpty, p);
        int fps = (int) (second * 50);
        return fps;
    }

    /**
     * 将 坐标 修正为 map 的中心点的下标
     *
     * @return map下标
     */
    public int[] fixPoint2Map() {
        int[] res = new int[2];
        res[0] = (int) (x / 0.5);
        res[1] = (int) ((50 - y) / 0.5);

        return res;
    }

    /**
     * 将 坐标 修正为 map 的中心点的坐标
     *
     * @return map下标
     */
    public Point fixPoint2Center() {
        return fixPoint2Center(x, y);
    }

    /**
     * 将 坐标 修正为 map 的中心点的坐标
     *
     * @param x x坐标
     * @param y y坐标
     * @return 坐标
     */
    public static Point fixPoint2Center(double x, double y) {
        double x1 = fixAxis2Center(x);
        double y1 = fixAxis2Center(y);

        return new Point(x1, y1);
    }

    /**
     * 修正一个点到中点
     *
     * @param t t
     * @return 修正点
     */
    public static double fixAxis2Center(double t) {
        return ((int) (t / 0.5)) * 0.5 + 0.25;
    }

    /**
     * 判断附近是否有工作台
     *
     * @return 附近是否有工作台
     */
    public boolean nearStation() {
        Pos pos = Astar.Point2Pos(this);
        // 周围2格有墙就算
        int dis = 3;
        for (int i = -dis; i <= dis; i++) {
            for (int j = -dis; j <= dis; j++) {
                if (posIsStation(pos.x + i, pos.y + j)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断点是否为工作台
     *
     * @param x x坐标
     * @param y y坐标
     * @return 点是否为工作台
     */
    private boolean posIsStation(int x, int y) {
        if (x < 0 || y < 0 || x > 99 || y > 99) return false;

        return Main.wallMap[x][y] <= 50 && Main.wallMap[x][y] >= 0;
    }

    public void set(Pos pos) {
        Point p = Astar.Pos2Point(pos);
        set(p);
    }


    /**
     * 若识别出来的点为机器人，则返回坐标+状态
     * 原理：三个不在同一条直线的点能唯一确定一个圆
     *
     * @param x1 x1
     * @param y1 y1
     * @param x2 x2
     * @param y2 y2
     * @param x3 x3
     * @param y3 y3
     * @return 敌方机器人位置
     */
    public static RadarPoint getCenterPos(double x1, double y1, double x2, double y2, double x3, double y3) {
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
        if (Robot.emptyRadius - epsion < r && r < Robot.emptyRadius + epsion) {
            double x_d = (double) Math.round(x * 100) / 100;
            double y_d = (double) Math.round(y * 100) / 100;
            return new RadarPoint(x_d, y_d, 0);
        } else if (Robot.fullRadius - epsion < r && r < Robot.fullRadius + epsion) {
            double x_d = (double) Math.round(x * 100) / 100;
            double y_d = (double) Math.round(y * 100) / 100;
            return new RadarPoint(x_d, y_d, 1);
        }
        return null;
    }

    /**
     * 以自己为中心，获取周围的所有的工作站
     *
     * @return 工作台列表
     */
    public ArrayList<Station> getNearStations() {
        Pos pos = Astar.Point2Pos(this);
        ArrayList<Station> stations = new ArrayList<>();
        int dis = 1;
        for (int i = -dis; i <= dis; i++) {
            for (int j = -dis; j <= dis; j++) {
                if (posIsStation(pos.x + i, pos.y + j)) {
                    stations.add(Main.pointStaMap.get(Astar.Pos2Point(new Pos(pos.x + i, pos.y + j))));
                }
            }
        }

        return stations;
    }

    /**
     * 获取点集
     *
     * @param vec      向量
     * @param pos      点
     * @param distance 距离
     * @return 点集
     */
    public static Point[] getPoints(Point vec, Point pos, double distance) {

        double x = pos.x;
        double y = pos.y;
        double[] direction = new double[]{vec.x, vec.y};   // 方向向量
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
        if (points[1] < points[3]) {
            p[0] = new Point(points[2], points[3]);
            p[1] = new Point(points[0], points[1]);
        } else {
            p[0] = new Point(points[0], points[1]);
            p[1] = new Point(points[2], points[3]);
        }

        return p;
    }


    /**
     * 判断工作站是否和墙靠得很近，
     * 一个机器人占了以后能否撞开
     *
     * @return 工作站是否和墙靠得很近
     */
    public boolean inCorner() {
        boolean fx = false;
        boolean fy = false;
        int step = 2;   // 判断
        for (int i = -step; i <= step; i++) {
            Point nx = new Point(x + i, y);
            Point ny = new Point(x, y + i);
            if (nx.isWall()) {
                fx = true;
            }
            if (ny.isWall()) {
                fy = true;
            }
        }
        // 判断墙角是前后有墙 并且左右有墙
        return fx && fy;
    }

    /**
     * 判断两个点是否接近，可用于去重
     *
     * @param point 点
     * @return 两个点是否接近
     */
    public boolean closeTo(Point point) {
        // 两个点很接近
        double dis = calcDistance(point);
        return dis <= 0.3;
    }

    /**
     * 判断是否为墙
     *
     * @return 是否为墙
     */
    public boolean isWall() {
        return posIsWall(x, y);
    }

    /**
     * 判断后面是否为墙
     *
     * @param vec   向量
     * @param point 点
     * @return 后面是否为墙
     */
    public static boolean isWallBehind(Point vec, Point point) {
        double[] direction = {vec.x, vec.y}; // 方向向量
        double[] unitDirection = normalize(direction); // 单位向量

        Point p = new Point(point.x - unitDirection[0] * 0.6, point.y - unitDirection[1] * 0.6);
        if (notInMap(p.x, p.y) || posIsWall(p.x, p.y)) {
            return true;
        }
        return false;
    }

    /**
     * 判断点是否为墙
     *
     * @param x x坐标
     * @param y y坐标
     * @return 是否为墙
     */
    public static boolean posIsWall(double x, double y) {
        if (notInMap(x, y)) {
            return true;
        }
        // 找出 x,y 属于哪一个点的区域
        Pos pos = Astar.Point2Pos(new Point(x, y));
        // -2 对应的是墙
        return Main.wallMap[pos.x][pos.y] == -2;
    }

    /**
     * 判断是否不在地图内
     *
     * @param x x坐标
     * @param y y坐标
     * @return 是否不在地图内
     */
    public static boolean notInMap(double x, double y) {
        boolean flag1 = x <= 0 || y <= 0;
        boolean flag2 = x >= 50 || y >= 50;

        return flag1 || flag2;
    }


    /**
     * 根据一个向量和夹角，算另外两个向量
     *
     * @param vec   夹角
     * @param angle 向量
     * @return 目标向量
     */
    public static Point[] calc2vec(Point vec, double angle) {

        double[] direction = {vec.x, vec.y}; // 方向向量

        double[] unitDirection = normalize(direction); // 单位向量
        double[] u = {-unitDirection[1], unitDirection[0]}; // 与单位向量垂直的向量

        double[] v1 = add(scale(Math.cos(angle), unitDirection), scale(Math.sin(angle), u));
        double[] v2 = add(scale(Math.cos(angle), unitDirection), scale(-Math.sin(angle), u));

        Point[] res = new Point[2];
        res[0] = new Point(v1[0], v1[1]);
        res[1] = new Point(v2[0], v2[1]);

        return res;
    }


    /**
     * 求向量长度
     *
     * @param v 向量
     * @return 向量长度
     */
    public static double magnitude(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1]);
    }

    //

    /**
     * 求向量的单位向量
     *
     * @param v 向量
     * @return 向量的单位向量
     */
    public static double[] normalize(double[] v) {
        double mag = magnitude(v);
        return new double[]{v[0] / mag, v[1] / mag};
    }

    //

    /**
     * 求向量的标量乘积
     *
     * @param c 系数
     * @param v 向量
     * @return 向量的标量乘积
     */
    public static double[] scale(double c, double[] v) {
        return new double[]{c * v[0], c * v[1]};
    }

    //

    /**
     * 求向量的加法
     *
     * @param v1 向量1
     * @param v2 向量2
     * @return 向量加法
     */
    public static double[] add(double[] v1, double[] v2) {
        return new double[]{v1[0] + v2[0], v1[1] + v2[1]};
    }


    /**
     * 得到靠近的墙体
     *
     * @return 靠近的墙体
     */
    public ArrayList<Point> getNearWall() {
        Pos pos = Astar.Point2Pos(this);

        ArrayList<Point> walls = new ArrayList<>();

        int[] steps = new int[]{1, -1, 2, -2};
        // 周围2格有墙就算
        for (int i : steps) {
            if (posIsWall(pos.x + i, pos.y)) {
                walls.add(Astar.Pos2Point(new Pos(pos.x + i, pos.y)));
                break;
            }
        }
        for (int i : steps) {
            if (posIsWall(pos.x, pos.y + i)) {
                walls.add(Astar.Pos2Point(new Pos(pos.x, pos.y + i)));
                break;
            }
        }
        for (int i : steps) {
            for (int j : steps) {
                if (posIsWall(pos.x + i, pos.y + j)) {
                    walls.add(Astar.Pos2Point(new Pos(pos.x + i, pos.y + j)));
                    break;
                }
            }
        }
        return walls;
    }
}