package com.huawei.codecraft.util;

import java.util.Objects;

/**
 * ClassName: RadarPoint
 * Package: com.huawei.codecraft.util
 * Description:
 *
 * @Author WLY
 * @Create 2023/4/12 19:26
 * @Version 1.0
 */
public class RadarPoint {
    public double x,y;
    public int isFull; //0表示空载 1表示载物

    public RadarPoint() {
    }

    public RadarPoint(double x, double y, int isFull) {
        this.x = x;
        this.y = y;
        this.isFull = isFull;
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

    public Point getPoint(){
        return new Point(x,y);
    }

    @Override
    public String toString() {
        return "Radar{(" +
                x + "," +
                y +
                "), full=" + isFull +
                '}';
    }
}
