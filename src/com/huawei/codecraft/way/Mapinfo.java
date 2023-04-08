package com.huawei.codecraft.way;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.core.Station;
import com.huawei.codecraft.core.Zone;

import java.util.*;


// 用于机器人空载和满载的地图
public class Mapinfo {
    static int[] bits = {20, 18, 12, 10};   // 用于判断斜边是否可以通过，按照上下左右是否有障碍物进行位运算
    static boolean[] isVisit = {false, false, false, false};       // 判断机器人是否已经寻找过
    public static int[][] mapInfoEmpty;
    public static int[][] mapInfoFull;
    public static int[][] mapInfoOri;      // 记录最原始的地图 -2表示障碍物，-1 表示空地，0-50表示工作台
    public static int[][] mapInfoOriginal;   // 记录最原始的地图 -2表示障碍物，-1 表示空地，0-50表示工作台
    public static int row;
    public static int col;
    Map<Integer, ArrayList<Integer>> robotId;      // 记录哪些机器人是连通的
    Map<Integer, ArrayList<Integer>> stationId;    // 记录哪些工作台是连通的
    ArrayList<Integer> robot;
    ArrayList<Integer> station;

    public Mapinfo(int[][] mapinfo) {
        row = mapinfo.length;
        col = mapinfo[0].length;
        mapInfoEmpty = new int[row][col];
        mapInfoFull = new int[row][col];
        mapInfoOri = new int[row][col];
        mapInfoOriginal = new int[row][col];
        initMapFull(mapinfo);
        initMapEmpty(mapinfo);
        robotId = new HashMap<>();
        stationId = new HashMap<>();
    }

    @Override
    public String toString() {
        return "Mapinfo{" +
                "robotId=" + robotId +
                ", stationId=" + stationId +
                '}';
    }

    // 广度优先搜索最原始的地图,寻找连通区域 -3表示已探索 -2表示障碍物，-1 表示空地，0-50表示工作台
    public void Bfs(Pos startPostion,  Map<Integer, Pos> robotPos) {
        ArrayList<Pos> openList = new ArrayList<Pos>();
        openList.add(startPostion);

        // 第一个机器人已经加入了，无需重复加入
        mapInfoOri[startPostion.x][startPostion.y] = -3;

        while (openList.size() != 0) {
            Pos currentPosition = openList.get(0);
            openList.remove(currentPosition);
            // 开始从上下左右依次加入节点
            int[] rangeX = {currentPosition.x - 1, currentPosition.x + 1};
            int[] rangeY = {currentPosition.y - 1, currentPosition.y + 1};

            // 待优化，按照上下左右得bit位判断是否存在障碍物
            int flag = 0;
            int y = currentPosition.y;
            // 上下探索
            for (int x : rangeX) {
                if (isInMap(x, y)) {
                    flag = (flag | (mapInfoOri[x][y] == -2 ? 1 : 0)) << 1;
                    if (mapInfoOri[x][y] >= -1) {
                        // 节点加入探索列表
                        openList.add(new Pos(x, y));
                        if (mapInfoOri[x][y] >= 100) {
                            robot.add(mapInfoOri[x][y] - 100);
                            isVisit[mapInfoOri[x][y] - 100] = true; // 机器人已经探索果了
                        }
                        else if (mapInfoOri[x][y] >= 0) {
//                            if (Main.stations[mapInfoOri[x][y]].type<=3 || mapInfoFull[x][y] == 0){
                                station.add(mapInfoOri[x][y]);
//                            }
                        }
                        // 节点设置为已探索，障碍物不能改变，不然会影响后续的点判断
                        mapInfoOri[x][y] = -3;
                    }
                }
            }

            // 左右
            int x = currentPosition.x;
            for (int i = 0; i < rangeY.length; i++) {
                y = rangeY[i];
                if (isInMap(x, y)) {
                    flag = (flag | (mapInfoOri[x][y] == -2 ? 1 : 0)) << 1;
                    if (mapInfoOri[x][y] >= -1) {
                        // 节点加入探索列表
                        openList.add(new Pos(x, y));
                        if (mapInfoOri[x][y] >= 100) {
                            robot.add(mapInfoOri[x][y] - 100);
                            isVisit[mapInfoOri[x][y] - 100] = true; // 机器人已经探索果了
                        }
                        else if (mapInfoOri[x][y] >= 0) {
//                            if (Main.stations[mapInfoOri[x][y]].type<=3 || mapInfoFull[x][y] == 0){
                                station.add(mapInfoOri[x][y]);
//                            }
                        }
                        // 节点设置为已探索，障碍物不能改变，不然会影响后续的点判断
                        mapInfoOri[x][y] = -3;
                    }
                }
            }

            // 往斜边寻找
            int index = 0;
            for (int i = 0; i < rangeX.length; i++) {
                for (int j = 0; j < rangeY.length; j++) {
                    x = rangeX[i];
                    y = rangeY[j];
                    if (isInMap(x, y) && ((flag & bits[index]) == 0)) {
                        if (mapInfoOri[x][y] >= -1) {
                            // 节点加入探索列表
                            openList.add(new Pos(x, y));
                            if (mapInfoOri[x][y] >= 100) {
                                robot.add(mapInfoOri[x][y] - 100);
                                isVisit[mapInfoOri[x][y] - 100] = true; // 机器人已经探索果了
                            }
                            else if (mapInfoOri[x][y] >= 0) {
//                                if (Main.stations[mapInfoOri[x][y]].type<=3 || mapInfoFull[x][y] == 0){
                                    station.add(mapInfoOri[x][y]);
//                                }
                            }
                            // 节点设置为已探索，障碍物不能改变，不然会影响后续的点判断
                            mapInfoOri[x][y] = -3;
                        }
                    }
                    index++;
                }
            }

        }
    }


    // 开始直接处理斜角不能走的情况，方便空载封路
    public void checkSideWay(int[][] mapinfo, int x, int y) {
        if (isInMap(x + 1, y - 1) && mapinfo[x + 1][y - 1] == -1) {
            if (isInMap(x + 2, y - 2) && mapinfo[x + 2][y - 2] == -2) {
                mapInfoOri[x + 1][y - 1] = -2;
                mapInfoEmpty[x + 1][y - 1] = 1;
            }
        }
        if (isInMap(x + 1, y + 1) && mapinfo[x + 1][y + 1] == -1) {
            if (isInMap(x + 2, y + 2) && mapinfo[x + 2][y + 2] == -2) {
                mapInfoOri[x + 1][y + 1] = -2;
                mapInfoEmpty[x + 1][y + 1] = 1;
            }
        }
    }

    // 获取连通的机器人和工作台的Id
    public void getConnectedArea(Map<Integer, Pos> robotPos) {
        int connectedArea = 0;
        for (Map.Entry<Integer, Pos> entry : robotPos.entrySet()) {
            // 获取机器人的 位置 和 Id
            Integer key = entry.getKey() - 100;
            Pos position = entry.getValue();
            if (isVisit[key]) {
                continue;
            }
            robot = new ArrayList<Integer>();
            station = new ArrayList<Integer>();
            robot.add(key);

            Bfs(position, robotPos);   // 从机器人的位置做广度优先搜索

            // 结果加入hash表
            robotId.put(connectedArea, robot);
            stationId.put(connectedArea, station);
            connectedArea++;
        }
    }

    // 获取连通的机器人ID
    public  Map<Integer, ArrayList<Integer>> getConnectedRobotsId() {
        return robotId;
    }

    // 获取连通的工作台ID
    public  Map<Integer, ArrayList<Integer>> getConnectedStationsId() {
        return stationId;
    }

    public int[][] getFixMap(boolean isEmpty){
        return isEmpty? mapInfoEmpty:mapInfoFull;
    }
    
    // 初始化用于空载的地图
    public void initMapEmpty(int[][] mapinfo) {
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                mapInfoOri[i][j] =  mapInfoOri[i][j] < 0 ? mapInfoOri[i][j] : mapinfo[i][j];   // 已被初始化的话不再初始化
                mapInfoOriginal[i][j] = mapinfo[i][j];

                if (mapinfo[i][j] == -2) {
                    // 检查斜边是否能走，不能走需要封闭斜边节点
                    checkSideWay(mapinfo, i, j);
                    mapInfoEmpty[i][j] = 1;
                    // 包住下边
                    if (isInMap(i + 1, j)) {
                        mapInfoEmpty[i + 1][j] = 1;
                        // 原始地图用于探索连通时，将除工作台、机器人以外的点全部置为封闭区域
                        mapInfoOri[i + 1][j] = mapinfo[i + 1][j]  >= 0 ? mapinfo[i + 1][j]  : -2;
                    }
                    // 包住右边
                    if (isInMap(i, j + 1)) {
                        mapInfoEmpty[i][j + 1] = 1;
                        mapInfoOri[i][j + 1] =  mapinfo[i][j + 1] >= 0 ?  mapinfo[i][j + 1] : -2;
                    }

                    // 封住左边需要检查左边的左边
                    if (isInMap(i, j - 2) && isInMap(i - 1, j - 2) && isInMap(i + 1, j - 2)) {
                        // 三种同时不成立才能包住
                        if (mapInfoEmpty[i][j - 2] != 1 && mapInfoEmpty[i - 1][j - 2] != 1 && mapInfoEmpty[i + 1][j - 2] != 1) {
                            mapInfoEmpty[i][j - 1] = 1;
                            mapInfoOri[i][j - 1] =  mapInfoOri[i][j - 1] >= 0 ?  mapInfoOri[i][j - 1] : -2;
                        }
                    }
                    // 封住上方需要看上方的上方是不是空位
                    if (isInMap(i - 2, j) && isInMap(i - 2, j - 1) && isInMap(i - 2, j + 1)) {
                        // 三种同时不成立才能包住
                        if (mapInfoEmpty[i - 2][j] != 1 && mapInfoEmpty[i - 2][j - 1] != 1 && mapInfoEmpty[i - 2][j + 1] != 1) {
                            mapInfoEmpty[i - 1][j] = 1;
                            mapInfoOri[i - 1][j] =  mapInfoOri[i - 1][j] >= 0 ?  mapInfoOri[i - 1][j] : -2;
                        }
                    }
                }
            }
        }
    }

    // 初始化用于满载的地图
    public  void initMapFull(int[][] mapinfo) {
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                mapInfoOri[i][j] = mapinfo[i][j];   // 初始化原始的地图
                if (mapinfo[i][j] == -2) {   // 有障碍包住点周围的 8 个方向
                    mapInfoFull[i][j] = 1;
                    int[] rangeX = {i - 1, i + 1};
                    int[] rangeY = {j - 1, j + 1};
                    // 左右
                    for (int y : rangeY) {
                        if (isInMap(i, y) ) {
                            mapInfoFull[i][y] = 1;
                        }
                    }
                    // 上下
                    for (int x : rangeX) {
                        if (isInMap(x, j)) {
                            mapInfoFull[x][j] = 1;
                        }
                    }
                }
            }
        }
    }

    public static boolean isInMap(int x, int y) {
        if (x < 0 || y < 0 || x >= row || y >= col) {
            return false;
        }
        return true;
    }

    private void putStation(int sid, int x, int y) {
        if (Main.stations[sid].type<=3 || mapInfoFull[x][y] == 0){
            station.add(sid);
        }
    }

    public void printMapFull() {
        for (int i = 0; i < row; i++) {
            String data = new String();
            for (int j = 0; j < col; j++) {
                if (mapInfoFull[i][j] == 0) {
                    data += " ";
                }
                else {
                    data += String.valueOf(mapInfoFull[i][j]);
                }
            }
            System.out.println(data);
        }
    }

        // 给station 和 robot 划分区域
        public void setZone(Map<Integer, Zone> zoneMap){
            getConnectedArea(Main.robotPos);
            for (int zoneId : robotId.keySet()) {
                Zone zone = null;
                if (!zoneMap.containsKey(zoneId)){
                    // 没有先创建
                    zone = new Zone(zoneId);
                    zoneMap.put(zoneId,zone);
                }
                // 分别把，机器人和station 加到zone里面去
                for (int rid : robotId.get(zoneId)) {
                    zone.robots.add(Main.robots[rid]);
                    Main.robots[rid].zone = zone;
                }
    
                for (int sid : stationId.get(zoneId)) {
                    Station st = Main.stations[sid];
                    if (!zone.stationsMap.containsKey(st.type)){
                        ArrayList<Station> list = new ArrayList<>();
                        list.add(st);
                        zone.stationsMap.put(st.type,list);
                    }else {
                        zone.stationsMap.get(st.type).add(st);
                    }
                    st.zone = zone;
                }
            }
        }
}
