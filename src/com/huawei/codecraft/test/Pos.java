package com.huawei.codecraft.test;

// 记录点的坐标
public class Pos {
    public int x = 0;
    public int y = 0;    // 坐标x，y

    public Pos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Pos pos = (Pos) o;
        return x == pos.x && y == pos.y;
    }
}
