package com.huawei.codecraft.way;
import com.huawei.codecraft.util.Point;

import java.util.*;;

// 记录点的坐标
public class Pos {
    public int x = 0;
    public int y = 0;    // 坐标x，y

    public Pos() {

    }

    public Pos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Pos pos = (Pos) o;
    return x == pos.x && y == pos.y;
    }
    
    @Override
    public int hashCode() {
    return Objects.hash(x, y);
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "(" + x +
                ", " + y +
                ')';
    }

    public void set(Point p) {
        Pos pos = Astar.Point2Pos(p);
        x = pos.x;
        y = pos.y;
    }

    public void set(Pos pos) {
        x = pos.x;
        y = pos.y;
    }
}
