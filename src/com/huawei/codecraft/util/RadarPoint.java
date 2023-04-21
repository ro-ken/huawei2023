package com.huawei.codecraft.util;

import com.huawei.codecraft.Main;

import java.util.HashSet;
import java.util.Objects;

/**
 * ClassName: RadarPoint
 * Package: com.huawei.codecraft.util
 * Description: 雷达扫描信息
 *
 * @Author WLY
 * @Create 2023/4/12 19:26
 * @Version 1.0
 */
public class RadarPoint {
    public double x, y;
    private final Point p;
    public int isFull; //0表示空载 1表示载物

    /**
     * 构造器
     *
     * @param x      x坐标
     * @param y      y坐标
     * @param isFull 是否满载
     */
    public RadarPoint(double x, double y, int isFull) {
        this.x = x;
        this.y = y;
        this.isFull = isFull;
        p = new Point(x, y);
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getIsFull() {
        return isFull;
    }

    public void setIsFull(int isFull) {
        this.isFull = isFull;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RadarPoint that = (RadarPoint) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0 && isFull == that.isFull;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, isFull);
    }

    public Point getPoint() {
        return p;
    }

    @Override
    public String toString() {
        return "Radar{(" +
                x + "," +
                y +
                "), full=" + isFull +
                '}';
    }

    /**
     * 分析敌人的运动轨迹，是否靠近我
     *
     * @param pos 点位置
     * @return 私人是否靠近我
     */
    public boolean isCloseToMe(Point pos) {
        LimitedQueue<HashSet<RadarPoint>>.Node currentNode = Main.enemysQueue.last;
//        Main.enemysQueue.reversePrint();
        Point last = getPoint();    // 获取上一个点
        while (currentNode != null) {
            HashSet<RadarPoint> set = currentNode.value;
            // 逆序遍历所有
            Point cur = getCloestPoint(set, last);

            if (cur == null) return false;  // 没找到上一帧数据，应该是刚出现
            if (cur.calcDistance(pos) < last.calcDistance(pos)) {
                return false;
            }
            last = cur;
            currentNode = currentNode.prev;
        }

        return true;    // 每一帧都在靠近，返回true
    }

    /**
     * 返回当前距离last最近的点
     *
     * @param set  集合
     * @param last last点
     * @return 当前距离last最近的点
     */
    private Point getCloestPoint(HashSet<RadarPoint> set, Point last) {
        for (RadarPoint radarPoint : set) {
            Point tp = radarPoint.getPoint();
            if (tp.calcDistance(last) < 0.3) {
                return tp;
            }
        }
        return null;
    }
}
