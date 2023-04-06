package com.huawei.codecraft.way;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.Point;

import java.util.*;

public class Astar {
    static int[] bits = {20, 18, 12, 10};   // 用于判断斜边是否可以通过，按照上下左右是否有障碍物进行位运算
    public Pos startPosition;
    public Pos targetPosition;
    public Board board;

    public ArrayList<Pos> openList ;     // 存储待扩展节点
    // ArrayList<Msg> closeList;         // 存储已探索节点，被优化
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
//        // 使用曼哈顿距离按照远离目标点的地方进行搜索
//        int dis = Math.abs(startPosition.x - targetPosition.x) + Math.abs(startPosition.y - targetPosition.y);
        // x 的变化小于 y的变化，按照 x 进行封路
        if (Math.abs(startPosition.x - targetPosition.x) <= Math.abs(startPosition.y - targetPosition.y)) {
            while (x > 0 && maps[x][y] != 2) {
                maps[x][y] = 2;
                x--;
            }
            while (x < Mapinfo.row && maps[x][y] != 2) {
                maps[x][y] = 2;
                x++;
            }
        }
        else {  // x 的变化大于 y的变化，按照 y 进行封路
            while (y > 0 && maps[x][y] != 2) {
                maps[x][y] = 2;
                y--;
            }
            while (y < Mapinfo.col && maps[x][y] != 2) {
                maps[x][y] = 2;
                y++;
            }
        }
//        // 该点是否是远离目标点的位置
//        if (Math.abs(x - targetPosition.x) + Math.abs(y - targetPosition.y) > dis) {
//            return new Pos(x, y);
//        }
//        else {
//            // 变化的是 y
//            if (x == startPosition.x) {
//                return new Pos(x, startPosition.y + 1);
//            }
//            else {
//                return new Pos(startPosition.x + 1, y);
//            }
//        }
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


    public static Point getSafePoint(boolean isEmpty,Point src, Point dest,HashSet<Pos> pos1){

        int[][] fixMap = Main.mapinfo.getFixMap(isEmpty);
        Astar ast = new Astar(fixMap,src,dest);
        ast.search();
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

    // 空载需要找 2 * 2 的网格，从该点进行寻找 -1 没找到，1 代表左上方 2 代表右上方 3 代表左下方 4 代表右下方
    public int getEmptyGrid(Pos curPos, HashSet<Pos> set) {
        if (set.contains(curPos)) {
            return -1;
        }
        // 15 = 1111(2) 代表上下左右
        int ret = 15;
        // 上
        if (!Mapinfo.isInMap(curPos.x - 1, curPos.y) || set.contains(new Pos(curPos.x - 1, curPos.y)) || Mapinfo.mapInfoOriginal[curPos.x - 1][ curPos.y] == -2) {
            ret &= 7;
        }
        // 下
        if (!Mapinfo.isInMap(curPos.x + 1, curPos.y) || set.contains(new Pos(curPos.x + 1, curPos.y)) || Mapinfo.mapInfoOriginal[curPos.x + 1][ curPos.y] == -2) {
            ret &= 11;
        }
        if (ret  == 3) {
            return -1;
        }
        // 左
        if (!Mapinfo.isInMap(curPos.x, curPos.y - 1) || set.contains(new Pos(curPos.x, curPos.y - 1)) || Mapinfo.mapInfoOriginal[curPos.x][ curPos.y - 1] == -2) {
            ret &= 13;
        }
        // 右
        if (!Mapinfo.isInMap(curPos.x, curPos.y + 1) || set.contains(new Pos(curPos.x, curPos.y + 1)) || Mapinfo.mapInfoOriginal[curPos.x][ curPos.y + 1] == -2) {
            ret &= 14;
        }
        if ((ret & 3) == 0) {
            return -1;
        }
        // 斜左上
        if ((ret & 10) != 0 && !set.contains(new Pos(curPos.x - 1, curPos.y - 1)) && Mapinfo.mapInfoOriginal[curPos.x - 1][ curPos.y - 1] != -2) {
            return 1;
        }
        // 斜右上
        if ((ret & 9) != 0 && !set.contains(new Pos(curPos.x - 1, curPos.y + 1)) && Mapinfo.mapInfoOriginal[curPos.x - 1][ curPos.y + 1] != -2) {
            return 2;
        }
        // 斜左下
        if ((ret & 6) != 0 && !set.contains(new Pos(curPos.x + 1, curPos.y - 1)) && Mapinfo.mapInfoOriginal[curPos.x + 1][ curPos.y - 1] != -2) {
            return 3;
        }
        // 斜右下
        if ((ret & 5) != 0 && !set.contains(new Pos(curPos.x + 1, curPos.y + 1)) && Mapinfo.mapInfoOriginal[curPos.x + 1][ curPos.y + 1] != -2) {
            return 4;
        }
        return -1;
    }
    // 0 代表找到，-1 没找到
    public int getFullGrid(Pos curPos, HashSet<Pos> set) {
        // 从当前点寻找 3 * 3 网格
        if (set.contains(curPos)) {
            return -1;
        }
        // 判断周围 8个方向是否符合条件
        int[] rangeX = {curPos.x - 1, curPos.x + 1};
        int[] rangeY = {curPos.y - 1, curPos.y + 1};
        if (!Mapinfo.isInMap(curPos.x - 1, curPos.y - 1) || !Mapinfo.isInMap(curPos.x + 1, curPos.y + 1)) {
            return -1;
        }
        // 左右
        int y = curPos.y;
        for (int x : rangeX) {
            if (set.contains(new Pos(x, y)) || Mapinfo.mapInfoOriginal[x][y] == -2) {
                return -1;
            }
        }
        // 上下
        int x = curPos.x;
        for (int i = 0; i < rangeY.length; i++) {
            y = rangeY[i];
            if (set.contains(new Pos(x, y)) || Mapinfo.mapInfoOriginal[x][y] == -2) {
                return -1;
            }
        }
        // 斜向
        for (int i = 0; i < rangeX.length; i++) {
            for (int j = 0; j < rangeY.length; j++) {
                x = rangeX[i];
                y = rangeY[j];
            }
            if (set.contains(new Pos(x, y)) || Mapinfo.mapInfoOriginal[x][y] == -2) {
                return -1;
            }
        }

        return 0;
    }

    public Point getPoint(int[][] maps, boolean carry, HashSet<Pos> pos1) {
        ArrayList<Pos> openList = new ArrayList<Pos>();; // 存储带拓展节点
        openList.add(startPosition);
        maps[startPosition.x][startPosition.y] = 1; // 起点已探索

        while (openList.size() != 0) {
            Pos curPos = openList.get(0);
            openList.remove(curPos);

            // 上下左右四个方向探索
            // 开始从上下左右依次加入节点
            int[] rangeX = {curPos.x - 1, curPos.x + 1};
            int[] rangeY = {curPos.y - 1, curPos.y + 1};

            // 上下探索
            int y = curPos.y;
            for (int x : rangeX) {
                if (Mapinfo.isInMap(x, y) && maps[x][y] == 0) {
                    maps[x][y] = 1; // 节点设置已探索
                    Pos explorePos = new Pos(x, y);
                    openList.add(explorePos);
                    // 判断该节点是否能找到 2*2 的网格
                    int flag = 0;
                    if (carry) {
                        flag = getFullGrid(explorePos, pos1);
                    }
                    else {
                        flag = getEmptyGrid(explorePos, pos1);
                    }
                    if (flag >= 0) {
                        Point ret = Pos2Point(explorePos);
                        if (flag == 0) {
                            return ret;
                        }
                        else if (flag == 1) {
                            ret.x -= 0.25;
                            ret.y += 0.25;
                        }
                        else if (flag == 2) {
                            ret.x += 0.25;
                            ret.y += 0.25;
                        }
                        else if (flag == 3) {
                            ret.x -= 0.25;
                            ret.y -= 0.25;
                        }
                        else {
                            ret.x += 0.25;
                            ret.y -= 0.25;
                        }
                        return ret;
                    }
                }
            }
            int x = curPos.x;
            for (int i = 0; i < rangeY.length; i++) {
                y = rangeY[i];
                if (Mapinfo.isInMap(x, y) && maps[x][y] == 0) {
                    maps[x][y] = 1; // 节点设置已探索
                    Pos explorePos = new Pos(x, y);
                    openList.add(explorePos);
                    // 判断该节点是否能找到 2*2 的网格
                    int flag = 0;
                    if (carry) {
                        flag = getFullGrid(explorePos, pos1);
                    }
                    else {
                        flag = getEmptyGrid(explorePos, pos1);
                    }
                    if (flag >= 0) {
                        Point ret = Pos2Point(explorePos);
                        if (flag == 0) {
                            return ret;
                        }
                        else if (flag == 1) {
                            ret.x -= 0.25;
                            ret.y += 0.25;
                        }
                        else if (flag == 2) {
                            ret.x += 0.25;
                            ret.y += 0.25;
                        }
                        else if (flag == 3) {
                            ret.x -= 0.25;
                            ret.y -= 0.25;
                        }
                        else {
                            ret.x += 0.25;
                            ret.y -= 0.25;
                        }
                        return ret;
                    }
                }
            }
        }
        return Pos2Point(startPosition);
    }

    public Point getTmpAvoidPoint(boolean carry,HashSet<Pos> pos1) {

        int[][] maps = new int[Mapinfo.row][Mapinfo.col];
        initAvoidMaps(maps);    // 用于找避让点的地图，0 未探索 1 已探索 2 障碍物
       blockAvoidMaps(maps);       // 阻塞地图，减小计算量
        //        targetPosition = Point2Pos(newTargePoint);

        return getPoint(maps, carry,pos1);
    }

    // 将得到的路径左边合并并返回结果坐标
    public  ArrayList<Point> getResult(boolean carry) {
        // 将结果返回，空载需要右移坐标，满载无需移动
        mergeResultList();
        fixRoute(carry);    // 修正得到的结果
        return result;
        // mergeResult(carry);
        // return result;
    }

    // 将得到的路径左边合并并返回结果
    public  ArrayList<Pos> getResultList() {
        return resultList;
    }

    public void fixRoute(boolean carry) {
        // 没有结果，返回
        int size = mergeList.size();
        if (size < 2){
            return;
        }

        result.add(Pos2Point(mergeList.get(0)));
        for (int i = 1; i < size - 1; i++) {
            // 两点相邻，直接优化掉后面的点，可能有些激进，但是目前就这样处理，
            Pos curPos = mergeList.get(i);
            // 空载情况下加入结果队列的点，让点尽可能在中间,如果偏移点的时候已经发生移到中心，空载计算路径无需再次偏移
            if (!carry) {
                Point p = Pos2Point(curPos);
                // 左偏
                if (isCriticalPos(curPos) == 1) {
                    p.x -= 0.25;
                }
                else if (isCriticalPos(curPos) == 2) { // 上偏
                    p.y += 0.25;
                }
                result.add(p);
            }
            else {
                result.add(Pos2Point(curPos));
            }

        }
        result.add(Pos2Point(mergeList.get(size - 1)));
    }

    public int isCriticalPos(Pos curPos) {
        int x = curPos.x;
        int y = curPos.y;
        // 判断右边是否是墙
        if (y == Mapinfo.col - 1 || Mapinfo.mapInfoOriginal[x][y + 1] == -2 || Mapinfo.mapInfoOriginal[x - 1][y + 1] == -2 || Mapinfo.mapInfoOriginal[x + 1][y + 1] == -2) {
            return 1;
        }
        // 判断上边是否是墙
        if (x == Mapinfo.row - 1 || Mapinfo.mapInfoOriginal[x + 1][y] == -2 || Mapinfo.mapInfoOriginal[x + 1][y - 1] == -2 || Mapinfo.mapInfoOriginal[x + 1][y + 1] == -2) {
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

        // 斜率大于1 得以 y 为增量，看 x 轴方向是否出现障碍
        if (Math.abs(param.k) > 1) {
            int turn = y1 < y2 ? 1 : -1;
            for (double y = y1 + step * turn; (y - y2) * turn - 0.01 < 0 ; y += step * turn) {
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
            for (double x = x1 + step * turn; (x - x2) * turn - 0.01 < 0 ; x += step * turn) {
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
        while (openList.size() != 0) {
            // Pos currentPosition = openList.poll();  // 获取堆顶元素并移除
            Pos currentPosition = openList.get(0);
            openList.remove(currentPosition);

            // 找到了终点
            if (currentPosition.equals(targetPosition)) {
//                System.out.println("成功找到路径解");

                while (currentPosition != null) {
                    resultList.add(currentPosition);
                    currentPosition = board.getMsg(currentPosition).parent; // 获取当前节点的父节点
                }
                Collections.reverse(resultList);
                return;
            }

            int[] rangeX = {currentPosition.x - 1, currentPosition.x + 1};
            int[] rangeY = {currentPosition.y - 1, currentPosition.y + 1};
            // 待优化，按照上下左右得bit位判断是否存在障碍物
            int flag = 0;
            // 上下
            for (int x : rangeX) {
                if (board.isInboard(x, currentPosition.y)) {
                    flag = (flag | (board.getMsg(new Pos(x, currentPosition.y)).isOK == 2 ? 1 : 0)) << 1;
                    if (board.getMsg(new Pos(x, currentPosition.y)).isOK != 2) {
                        int newG = board.getMsg(currentPosition).G + Board.StraightCost;
                        updateG(x, currentPosition.y, currentPosition, newG);
                    }
                }
            }

            // 左右
            for (int y : rangeY) {
                if (board.isInboard(currentPosition.x, y)) {
                    flag = (flag | (board.getMsg(new Pos(currentPosition.x, y)).isOK == 2 ? 1 : 0)) << 1;
                    if (board.getMsg(new Pos(currentPosition.x, y)).isOK != 2) {
                        int newG = board.getMsg(currentPosition).G + Board.StraightCost;
                        updateG(currentPosition.x, y, currentPosition, newG);
                    }
                }
            }

            // 往斜边寻找
            // 开始寻找下一个最佳点，按照F值进行寻找,检查并记录G是否需要更新
            int index = 0;

            for (int x : rangeX) {
                for (int y : rangeY) {
                    if (board.isInboard(x, y) && ((flag & bits[index]) == 0)) {
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
