package com.huawei.codecraft.way;

// 棋盘信息类
public class Board {
    public static int row = 0;
    public static int col = 0;
    public Msg[][] maps;
    public Pos targetPostion;
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

    // 判断寻找的点是否处于地图之中
    public  boolean isInboard(int x, int y) {
        if (x < 0 || y < 0 || x >= row || y >= col) {
            return false;
        }
        return true;
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
