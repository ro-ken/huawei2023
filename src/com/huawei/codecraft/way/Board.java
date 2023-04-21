package com.huawei.codecraft.way;

/**
 * @Author: zhouzhiqi
 * @Data:2023/4/1 19:47
 * @Description: 地图封路实现，保证寻路算法实现
 */

// 记录寻路路径的地图信息，每个点保存该点的F值和探索情况
public class Board {
    public static int row = 0;
    public static int col = 0;
    public Msg[][] maps;                    // 记录此次寻路的地图，使用空间换取时间策略
    public Pos targetPostion;               // 终点位置的坐标，开始将 point 转为 pos传入
    public static int StraightCost = 10;    // 直边代价
    public static int HypotenuseCost = 14;  // 斜边代价


    public Board(int[][] mapInfo, Pos targetPostion) {
        row = mapInfo.length;
        col = mapInfo[0].length;
        maps = new Msg[row][col];
        this.targetPostion = targetPostion;
        initMsg(mapInfo);
        maps[targetPostion.x][targetPostion.y].isOK = 0; // 终点设置为可探索，防止出现错误
    }

    // 初始话每个点到目标点的 H 值
    public void initMsg(int[][] mapInfo) {
        for (int i = 0; i < row; ++i) {
            for (int j = 0; j < col; ++j) {
                maps[i][j] = new Msg();
                // 障碍物初始设置为2，用于区分斜穿条件
                maps[i][j].isOK = mapInfo[i][j] == 1 ? 2 : 0;
                maps[i][j].H = StraightCost * (Math.abs(targetPostion.x - i) + Math.abs(targetPostion.y - j));
                
            }
        } 
    }

    // 获取每个点的信息
    public Msg getMsg(Pos posotion) {
        return maps[posotion.x][posotion.y];
    }

    public void printBoard() {
        for (int i = 0; i < row; i++) {
            String data = new String();
            for (int j = 0; j < col; j++) {
                if (getMsg(new Pos(i, j)).isOK == 0) {
                    data += " ";
                }
                else {
                    data += String.valueOf(2);
                }
            }
            System.out.println(data);
        }
    }
}
