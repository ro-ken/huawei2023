package com.huawei.codecraft.way;

// 记录寻路路径的地图信息，每个点保存该点的F值和探索情况
public class Board {
    public static int row = 0;
    public static int col = 0;
    private static int[] dirX = {-1, 1, 0, 0, -1, -1, 1, 1};
    private static int[] dirY = {0, 0, -1, 1, -1, 1, -1, 1};
    public Msg[][] maps;                    // 记录此次寻路的地图，使用空间换取时间策略
    public Pos targetPostion;               // 终点位置的坐标，开始将 point 转为 pos传入
    public static int StraightCost = 10;    // 直边代价
    public static int HypotenuseCost = 14;  // 斜边代价


    public Board(int[][] mapInfo, Pos startPostion, Pos targetPostion) {
        row = mapInfo.length;
        col = mapInfo[0].length;
        maps = new Msg[row][col];
        this.targetPostion = targetPostion;
        initMsg(mapInfo);
        // openStartPos(startPostion);
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
    
    // 将起点周围一圈打开
    private void openStartPos(Pos startPostion) {
        for (int i = 0; i < dirX.length / 4; i++) {
            int x = startPostion.x + dirX[i];
            int y = startPostion.y + dirY[i];
            if (Mapinfo.isInMap(x, y)) {
                maps[x][y].isOK = 0; // 打开起点
            }
        }
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
