package com.huawei.codecraft.test;

// 记录每个点的具体信息
public class Msg {
    public  int G = 0;  // Ｇ：起点到终点已付出的代价
    public  int H = 0;  // Ｈ：当前到终点的预计代价，只计算直线距离并且无视障碍（欧拉距离或者曼哈顿距离）
    public  int isOK = 0;  // 判断节点是否访问过 0 未访问，1访问过
    public Pos parent = null;  // 记录该点的父节点

    public Msg() {

    }

    // 获取该点到终点的代价
    public int getF() { 
        return G + H;
    }
}
