package com.huawei.codecraft.way;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.Point;

import java.util.*;

// 寻路算法主要实现
public class Astar {
    static int[] bits = {20, 18, 12, 10};   // 用于判断斜边是否可以通过，按照上下左右是否有障碍物进行位运算
    static int[] dirX = {-1, 1, 0, 0, -1, -1, 1, 1};
    static int[] dirY = {0, 0, -1, 1, -1, 1, -1, 1};
    static int findTimes = 101;
    static int maxTimes = 10001;
    public Pos startPosition;
    public Pos targetPosition;
    public Board board;
    public ArrayList<Pos> openList ;     // 存储待扩展节点
    // 后续使用优先队列进行优化
    // PriorityQueue<Pos> openList;
    public ArrayList<Pos> resultList;    // 存储结果节点
    public ArrayList<Pos> mergeList;    // 存储合并的结果节点
    public ArrayList<Pos> fixList;
    public ArrayList<Point> result;     // 存储结果节点

    public Astar(int[][] mapinfo, Point startPoint, Point endPoint) {
        this.startPosition = Point2Pos(startPoint);
        this.targetPosition = Point2Pos(endPoint);
        board = new Board(mapinfo, targetPosition);
        openList = new ArrayList<Pos>();
        // openList = new PriorityQueue<>((pos1, pos2)->Integer.compare(board.getMsg(pos1).getF(), board.getMsg(pos2).getF()));
        resultList = new ArrayList<Pos>();
        mergeList = new ArrayList<>();
        fixList = new ArrayList<>();
        result = new ArrayList<Point>();
    }

    public void blockAvoidMaps(int[][] maps) {
        int x = targetPosition.x, y = targetPosition.y;
        int startI = Math.max(x - 2, 0);
        int startJ = Math.max(y - 2, 0);
        int endI = x + 2 < Mapinfo.row ? x + 2 : Mapinfo.row - 1;
        int endJ = y + 2 < Mapinfo.col ? y + 2 : Mapinfo.col - 1;

        for (int i = startI; i <= endI; i++) {
            for (int j = startJ; j <= endJ; j++) {
                maps[i][j] = 2;
            }
        }
    }

    // 创建两点之间的直线方程
    public Pair calLineExpression(Pos starPos, Pos endPos) {
        Point startPoint = Pos2Point(starPos);
        Point endPoint = Pos2Point(endPos);
        double k = (endPoint.y - startPoint.y) / (endPoint.x - startPoint.x);
        double b = startPoint.y - k * startPoint.x;
        return new Pair(k, b);
    }

    public static ArrayList<Point> getPath(boolean isEmpty,Point src, Point dest){
        double dis = src.calcDistance(dest);
        if (dis < 1.0){
            if (src.equals(dest) || src.fixPoint2Center().equals(dest.fixPoint2Center())){
                ArrayList<Point> res = new ArrayList<>();
                res.add(src);
                res.add(dest);
                return res;
            }
        }

        int[][] fixMap = Main.mapinfo.getFixMap(isEmpty);
        Astar ast = new Astar(fixMap,src,dest);
        ast.search();
        return ast.getResult(!isEmpty);
    }

    // 计算两点多长，返回点的个数
    public static int calcDis(boolean isEmpty,Point src, Point dest){
        double dis = src.calcDistance(dest);
        if (dis < 1.0){
            if (src.equals(dest) || src.fixPoint2Center().equals(dest.fixPoint2Center())){
                return 2;
            }
        }

        int[][] fixMap = Main.mapinfo.getFixMap(isEmpty);
        Astar ast = new Astar(fixMap,src,dest);
        ast.search();

        return ast.resultList.size();
    }

    // 计算两点多长，返回点的个数
    public static int calcDisAndMidPoint(boolean isEmpty,Point src, Point dest,Point mid){
        double dis = src.calcDistance(dest);
        if (dis < 1.0){
            if (src.equals(dest) || src.fixPoint2Center().equals(dest.fixPoint2Center())){
                mid.set(dest);
                return 2;
            }
        }

        int[][] fixMap = Main.mapinfo.getFixMap(isEmpty);
        Astar ast = new Astar(fixMap,src,dest);
        ast.search();
        mid.set(ast.resultList.get(ast.resultList.size()/2));
        return ast.resultList.size();
    }




    public static Point getSafePoint(boolean isEmpty,Point src, Point dest,HashSet<Pos> pos1){

        int[][] fixMap = Main.mapinfo.getFixMap(isEmpty);
        Astar ast = new Astar(fixMap,src,dest);

        Point sp = ast.getTmpAvoidPoint(!isEmpty, pos1);
        return sp;
    }

    public static Point getSafePoint(boolean isEmpty,Point src, Point dest,HashSet<Pos> pos1,Point midPoint){

        int[][] fixMap = Main.mapinfo.getFixMap(isEmpty);
        Main.printLog("midPoint" + midPoint);
        Astar ast = new Astar(fixMap,src,midPoint);

        Point sp = ast.getTmpAvoidPoint(!isEmpty, pos1);
        return sp;
    }

    public static Point getSafePointAndBasePoint(boolean isEmpty,Point src, Point dest,HashSet<Pos> pos1){

        int[][] fixMap = Main.mapinfo.getFixMap(isEmpty);
        Astar ast = new Astar(fixMap,src,dest);

        Point sp = ast.getTmpAvoidPoint(!isEmpty, pos1);
        return sp;
    }

    public static ArrayList<Point> getPathAndResult(boolean isEmpty,Point src, Point dest,Set<Pos> posSet){
        double dis = src.calcDistance(dest);
        if (dis < 1.0){
            if (src.equals(dest) || src.fixPoint2Center().equals(dest.fixPoint2Center())){
                ArrayList<Point> res = new ArrayList<>();
                res.add(src);
                res.add(dest);
                return res;
            }
        }

        int[][] fixMap = Main.mapinfo.getFixMap(isEmpty);
        Astar ast = new Astar(fixMap,src,dest);
        ast.search();
        resultToPosSet(ast.resultList,posSet);
//        posSet.addAll(ast.resultList);
        return ast.getResult(!isEmpty);
    }

    private static void resultToPosSet(ArrayList<Pos> resultList, Set<Pos> posSet) {
        // 把结果保存到posSet里面，加上下左右点
        for (Pos pos : resultList) {
            posSet.add(pos);
            posSet.add(new Pos(pos.x-1, pos.y));
            posSet.add(new Pos(pos.x+1, pos.y));
            posSet.add(new Pos(pos.x, pos.y-1));
            posSet.add(new Pos(pos.x, pos.y+1));
        }
    }

    public static double getPosDistance(Pos pos1, Pos pos2) {
            Point point1 = Pos2Point(pos1);
            Point point2 = Pos2Point(pos2);
            return Math.sqrt(Math.pow((point1.x - point2.x), 2) + Math.pow((point1.y - point2.y), 2));
    }
    public static Point getClosestPoint(Point sp, HashSet<Pos> pos1) {
        if (pos1.size() == 0) {
            return null;
        }
        Pos pos = Point2Pos(sp);
        // 按照上下左右 斜上左 斜上右 斜下左 斜下右 循环
        int step = 1;
        boolean flag = false;
        Pos minPos = new Pos();
        double mindis = 50;
        int times = 0;
        while(!flag && times != findTimes) {   // 检查是否检测到了
              for (int i = 0; i < dirX.length; i++) {
                    int x = pos.x + dirX[i] * step;
                    int y = pos.y + dirY[i] * step;
                    Pos curPos = new Pos(x, y);
                    if (Mapinfo.isInMap(x, y) && pos1.contains(curPos)) {
                          flag = true;
                          double curDis = getPosDistance(pos, curPos);
                          if (mindis > curDis) {
                                mindis = curDis;
                                minPos.set(curPos);;
                          }
                    }
              }
              step++;
              times++;
        }
        if (times == findTimes) {
            return null;
        }
        return Pos2Point(minPos);
    }
    
    // 0 代表找到，-1 没找到
    public boolean getFullGrid(Pos curPos, HashSet<Pos> set) {
        // 从当前点寻找 3 * 3 网格
        if (set.contains(curPos)) {
            return false;
        }
        // 判断周围 8个方向是否符合条件
        for (int i = 0; i <  dirX.length; i++) {
            int x = curPos.x + dirX[i];
            int y = curPos.y + dirY[i];
            if (!Mapinfo.isInMap(x, y) || set.contains(new Pos(x, y)) || Mapinfo.mapInfoOriginal[x][y] == -2) {
                return false;
            }
        }

        return true;
    }

    public Point getPoint(int[][] maps, HashSet<Pos> pos1) {
        if (pos1.size() == 0) {
            return null;
        }
        ArrayList<Pos> openList = new ArrayList<Pos>();; // 存储带拓展节点
        openList.add(startPosition);
        maps[startPosition.x][startPosition.y] = 1; // 起点已探索
        int time = 0;
        while (openList.size() != 0 && time != maxTimes) {
            time++;
            Pos curPos = openList.get(0);
            openList.remove(curPos);
            // 上下左右四个方向探索
            // 开始从上下左右依次加入节点
            if (getFullGrid(curPos, pos1)) {
                return Pos2Point(curPos);
            }
            for (int i = 0; i < dirX.length / 2; i++) {
                int x = curPos.x + dirX[i];
                int y = curPos.y + dirY[i];
                if (Mapinfo.isInMap(x, y) && maps[x][y] == 0) {
                        maps[x][y] = 1; // 节点设置已探索
                        Pos explorePos = new Pos(x, y);
                        openList.add(explorePos);
                }
            }
        }
        return null;
    }

    public Point getTmpAvoidPoint(boolean carry,HashSet<Pos> pos1) {

        int[][] maps = new int[Mapinfo.row][Mapinfo.col];
        initAvoidMaps(maps);    // 用于找避让点的地图，0 未探索 1 已探索 2 障碍物
        blockAvoidMaps(maps);       // 阻塞地图，减小计算量
        return getPoint(maps, pos1);
    }

    // 将得到的路径左边合并并返回结果坐标
    public  ArrayList<Point> getResult(boolean carry) {
        // 将结果返回，空载需要右移坐标，满载无需移动
        mergeResultList();
        fixRoute();    // 修正得到的结果
        return result;
    }

    // 将得到的路径左边合并并返回结果
    public  ArrayList<Pos> getResultList() {
        return resultList;
    }

    public void fixRoute() {
        // 没有结果，返回
        int size = mergeList.size();
        if (size < 2){
            return;
        }

        for (int i = 0; i < size; i++) {
            // 两点相邻，直接优化掉后面的点，可能有些激进，但是目前就这样处理，
            Pos curPos = mergeList.get(i);
            result.add(Pos2Point(curPos));
        }
    }

    public int isCriticalPos(Pos curPos) {
        int x = curPos.x;
        int y = curPos.y;
        // 判断左边是否是墙
        if ((Mapinfo.isInMap(x, y - 1) && Mapinfo.isInMap(x - 1, y - 1) && Mapinfo.isInMap(x - 1, y + 1)) &&  (Mapinfo.mapInfoOriginal[x][y - 1] == -2 || Mapinfo.mapInfoOriginal[x - 1][y - 1] == -2 || Mapinfo.mapInfoOriginal[x + 1][y - 1] == -2)) {
            return 1;
        }
        // 判断上边是否是墙
        if ((Mapinfo.isInMap(x - 1, y) && Mapinfo.isInMap(x - 1, y - 1) && Mapinfo.isInMap(x - 1, y + 1)) && (Mapinfo.mapInfoOriginal[x - 1][y] == -2 || Mapinfo.mapInfoOriginal[x - 1][y - 1] == -2 || Mapinfo.mapInfoOriginal[x - 1][y + 1] == -2)) {
            return 2;
        }
        return 0;
    }

    // TODO：边界问题暂时没有考虑，需要优化
    public boolean isObstacle(Pair param, Pos startPos, Pos endPos) {
        double step = 0.5;  // 下标 x 的增加对应世界地图值增加 0.5
        double x1 = Pos2Point(startPos).x;
        double y1 = Pos2Point(startPos).y;
        double x2 = Pos2Point(endPos).x;
        double y2 = Pos2Point(endPos).y;

        int times = 0;
        // 斜率大于1 得以 y 为增量，看 x 轴方向是否出现障碍
        if (Math.abs(param.k) > 1) {
            int turn = y1 < y2 ? 1 : -1;
            for (double y = y1 + step * turn; (y - y2) * turn - 0.01 < 0 && times != 100; y += step * turn) {
                times++;
                double x = (y - param.b) / param.k;
                Pos nexPos = Point2Pos(new Point(x, y));
                // 连接线上的点，上下不能有障碍物

                if ( (nexPos.y < Mapinfo.col - 1 && nexPos.y > 0) &&  ( Mapinfo.mapInfoOriginal[nexPos.x][nexPos.y] == -2 || Mapinfo.mapInfoOriginal[nexPos.x][nexPos.y - 1] == -2 || Mapinfo.mapInfoOriginal[nexPos.x][nexPos.y + 1] == -2)) {
                    return true;
                }
                else if (nexPos.y == Mapinfo.col - 1 && (Mapinfo.mapInfoOriginal[nexPos.x][nexPos.y] == -2 || Mapinfo.mapInfoOriginal[nexPos.x][nexPos.y - 1] == -2)) {
                    return true;
                }
                else if (nexPos.y == 0 && (Mapinfo.mapInfoOriginal[nexPos.x][nexPos.y] == -2 || Mapinfo.mapInfoOriginal[nexPos.x][nexPos.y + 1] == -2)) {
                    return true;
                }
            }
        }
        else {
            int turn = x1 < x2 ? 1 : -1;
            // 从起点和终点开始判断是否能够舍弃该点,相邻点直接舍弃
            for (double x = x1 + step * turn; (x - x2) * turn - 0.01 < 0 && times != 100; x += step * turn) {
                times++;
                double y =  param.k * x + param.b;
                Pos nexPos = Point2Pos(new Point(x, y));
                // 连接线上的点，上下不能有障碍物
                if ((nexPos.x > 0 && nexPos.x < Mapinfo.row - 1) && (Mapinfo.mapInfoOriginal[nexPos.x][nexPos.y] == -2 || Mapinfo.mapInfoOriginal[nexPos.x - 1][nexPos.y] == -2 || Mapinfo.mapInfoOriginal[nexPos.x + 1][nexPos.y] == -2)) {
                    return true;
                }
                else if (nexPos.x == 0 && (Mapinfo.mapInfoOriginal[nexPos.x][nexPos.y] == -2 || Mapinfo.mapInfoOriginal[nexPos.x + 1][nexPos.y] == -2)) {
                    return true;
                }
                else if (nexPos.x == Mapinfo.row - 1 && (Mapinfo.mapInfoOriginal[nexPos.x][nexPos.y] == -2 || Mapinfo.mapInfoOriginal[nexPos.x - 1][nexPos.y] == -2)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void initAvoidMaps(int[][] maps) {
        for (int i = 0; i < Mapinfo.row; i++) {
            for (int j = 0; j < Mapinfo.col; j++) {
                if (Mapinfo.mapInfoOriginal[i][j] == -2) {
                    maps[i][j] = 2;
                }
                else {
                    maps[i][j] = 0;
                }
            }
        }
    }

    // 合并结果，将相同的坐标合并在一起
    public void mergeResultList() {
        // 没有结果，返回
        if (resultList.size() < 2){
            return;
        }
        Pos startPos = resultList.get(0);
        Pos prePos = resultList.get(1);
        mergeList.add(startPos);    // 起点加入合并列表

        for (int index = 2; index < resultList.size() - 1; index++) {
            Pos curPos = resultList.get(index);
            if (curPos.x == startPos.x || curPos.y == startPos.y) {
                prePos = curPos;
                continue;
            }
            Pair lineParam = calLineExpression(startPos, curPos);
            if (isObstacle(lineParam, startPos, curPos)) {   // 两点之间有障碍，这个点的前一个点就必须加入节点
                mergeList.add(prePos);
                startPos = prePos;
            }
            prePos = curPos;
        }
        mergeList.add(resultList.get(resultList.size() - 1));    // 终点加入合并列表
    }

    // 将得到的坐标转为Point
    public static Point Pos2Point(Pos Pos) {
        double x = Pos.y * 0.5 + 0.25 ;
        double y = 50 - (Pos.x * 0.5 + 0.25) ;
        return new Point(x, y);
    }

    // 将Point转为Pos用于地图索引 世界地图0-50 对应 数组下标0-99
    public static Pos Point2Pos(Point point) {
        int x = 99 -  (int)(point.y / 0.5);
        int y = (int)(point.x / 0.5);
        return new Pos(x, y);
    }

    // A* 搜索算法
    // 判断是否找到路径
    public void search() {
        board.getMsg(startPosition).isOK = 1;    // 起点设为已探索
        openList.add(startPosition);                // 从起点开始探索
        int times = 0;
        while (openList.size() != 0 && times != maxTimes) {
            // Pos currentPosition = openList.poll();  // 获取堆顶元素并移除
            times++;
            Pos currentPosition = openList.get(0);
            openList.remove(currentPosition);

            // 找到了终点
            if (currentPosition.equals(targetPosition)) {
                int times1 = 0;
                while (currentPosition != null && times1 != maxTimes) {
                    times1++;
                    resultList.add(currentPosition);
                    currentPosition = board.getMsg(currentPosition).parent; // 获取当前节点的父节点
                }
                Collections.reverse(resultList);
                return;
            }


            // 待优化，按照 上下左右的 bit位判断是否存在障碍物
            int flag = 0;
            for (int i = 0; i < dirX.length / 2; i++) {
                int x = currentPosition.x + dirX[i];
                int y = currentPosition.y + dirY[i];
                if (Mapinfo.isInMap(x, y)) {
                    flag = (flag | (board.getMsg(new Pos(x, y)).isOK == 2 ? 1 : 0)) << 1;
                    if (board.getMsg(new Pos(x, y)).isOK != 2) {
                        int newG = board.getMsg(currentPosition).G + Board.StraightCost;
                        updateG(x, y, currentPosition, newG);
                    }
                }
            }
            // 往斜边寻找
            // 开始寻找下一个最佳点，按照F值进行寻找,检查并记录G是否需要更新
            int index = 0;
            int[] rangeX = {currentPosition.x - 1, currentPosition.x + 1};
            int[] rangeY = {currentPosition.y - 1, currentPosition.y + 1};
            for (int x : rangeX) {
                for (int y : rangeY) {
                    if (Mapinfo.isInMap(x, y) && ((flag & bits[index]) == 0)) {
                        // 计算当前点的G
                        int newG = board.getMsg(currentPosition).G + Board.HypotenuseCost;
                        updateG(x, y, currentPosition, newG);
                    }
                    index++;
                }
            }


            // 每次重新排序，选择最小的 F 作为下一次的探索节点,使用lambda进行重排序，加负号就是从大到小排序
            Comparator<Pos> comparator = Comparator.comparingInt(openList -> board.getMsg(openList).getF());
            openList.sort(comparator);

        }
    }

    public void updateG(int x, int y, Pos currentPosition, int newG) {
        // 维护最小的G值
        if (board.maps[x][y].isOK == 0) {
            board.maps[x][y].isOK = 1;               // 设为以探索
            openList.add(new Pos(x, y));
            board.maps[x][y].parent = currentPosition;  // 设置父节点
            board.maps[x][y].G = newG;                  // 更新节点G
        }
        // 如果未更新G值，更新G值
        if (board.maps[x][y].G > newG) {
            board.maps[x][y].parent = currentPosition;
            board.maps[x][y].G = newG;
        }
    }
}
