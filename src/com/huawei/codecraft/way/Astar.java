package com.huawei.codecraft.way;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.Point;

import java.util.*;

/**
 * @Author: zhouzhiqi
 * @Data:2023/4/1 19:47
 * @Description: 寻路算法主要实现
 */

// 寻路算法主要实现
public class Astar {
    public static int narrowPathCount = 0;
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
    public ArrayList<Point> mergeList;    // 存储合并的结果节点
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
                if (posNotInTwoPos(startPosition,targetPosition,i,j)){
                    maps[i][j] = 2;
                }
            }
        }
    }

    // 将机器人的位置当作障碍物进行锁定
    // todo：暂时直接封禁一圈，因为前面的东西拿东西突然变大，也会对路径产生影响
    public void blockPoints(HashSet<Point> pointSet) {
        if (pointSet == null){
            return;
        }

        for (Point p : pointSet) {
            // 直接将机器人周围一圈封禁掉
            Pos position = Point2Pos(p);
            if (Mapinfo.isInMap(position.x, position.y))  {
                board.getMsg(position).isOK = 2;
            }
            for (int i = 0; i < dirX.length; i++) {
                int x = position.x + dirX[i];
                int y = position.y + dirY[i];
                if (Mapinfo.isInMap(x, y)) {
                    board.getMsg(new Pos(x, y)).isOK = 2;
                }
            }
        }
    }

    private boolean posNotInTwoPos(Pos s, Pos t, int i, int j) {
        if (i == t.x && j == t.y){
            return false;   //中点放出来
        }
        //
        if (s.x == t.x && s.y == t.y){
            return true;
        }

        if (s.x != t.x && s.y != t.y){
            //
            boolean flag1 = (t.x - s.x) * (t.x-i) >= 0; //同侧
            boolean flag2 = (t.y - s.y) * (t.y-j) >= 0; //同侧
            if (flag1 && flag2){
                return false;
            }

        }else {
            if (s.x == t.x){
                boolean flag2 = (t.y - s.y) * (t.y-j) >= 0; //同侧
                boolean flag3 = Math.abs(t.x - i) <= 1; //
                if (flag2 && flag3){
                    return false;
                }
            }else {
                boolean flag1 = (t.x - s.x) * (t.x-i) >= 0; //同侧
                boolean flag3 = Math.abs(t.y - j) <= 1; //
                if (flag1 && flag3){
                    return false;
                }
            }
        }
        return true;
    }

    // 创建两点之间的直线方程
    public Pair calLineExpression(Point startPoint,  Point endPoint) {
        double k = (endPoint.y - startPoint.y) / (endPoint.x - startPoint.x);
        double b = startPoint.y - k * startPoint.x;
        return new Pair(k, b);
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
        if (ast.resultList.size() == 0){
            mid.set(src);
            return 100000;  // 未找到
        }
        mid.set(ast.resultList.get(ast.resultList.size()/2));
        return ast.resultList.size();
    }

    // 判断是否是狭窄路径
    private boolean isNarrowWay(Pos curPos) {
        // 路超过6就算宽
        int lenX = 0, lenY = 0;
        int x = curPos.x , y = curPos.y;
        // 计算横向的宽度
        while (lenX < 6 && Mapinfo.isInMap(x, y) && Mapinfo.mapInfoOriginal[x][y] != -2) {
            lenX++;
            y--;
        }
        y = curPos.y + 1;
        while (lenX < 6 && Mapinfo.isInMap(x, y) && Mapinfo.mapInfoOriginal[x][y] != -2) {
            lenX++;
            y++;
        }
        y = curPos.y;
        // 计算纵向宽度
        while (lenY < 6 && Mapinfo.isInMap(x, y) && Mapinfo.mapInfoOriginal[x][y] != -2) {
            lenY++;
            x--;
        }
        x = curPos.x + 1;
        while (lenY < 6 && Mapinfo.isInMap(x, y) && Mapinfo.mapInfoOriginal[x][y] != -2) {
            lenY++;
            x++;
        }
        int len = Math.min(lenX, lenY);
        if (len <= 5) {
            return true;
        }
        return false;    
    }

    private void countNarrowPath() {
        // 连续15格路径宽度低于5，判定为狭窄路径
        int total = 0;
        for (Pos pathPos : resultList) {
            if (isNarrowWay(pathPos)) {
                total++;
            }
            else {
                total = 0;
            }
            if (total >= 15) {
                narrowPathCount++;
            }
        }
    }

    public static Point getSafePoint2(boolean isEmpty,Point src, Point dest,HashSet<Pos> pos1,Point midPoint){

        int[][] fixMap = Main.mapinfo.getFixMap(isEmpty);
        Main.printLog("midPoint" + midPoint);
        Astar ast = new Astar(fixMap,src,midPoint);

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
        if (Main.mapSeq == 0) {
            ast.countNarrowPath();
        }
        resultToPosSet(ast.resultList,posSet);
//        posSet.addAll(ast.resultList);
        return ast.getResult(!isEmpty);
    }

    public static ArrayList<Point> getPathBlockRobots(boolean isEmpty,Point src, Point dest,Set<Pos> posSet,HashSet<Point> robots){
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
        ast.blockPoints(robots);    // 先封锁机器人
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

    // 空载需要找 2 * 2 的网格，从该点进行寻找 -1 没找到，1 代表左上方 2 代表右上方 3 代表左下方 4 代表右下方
    public int getEmptyGrid(Pos curPos, HashSet<Pos> set) {
        if (set.contains(curPos)) {
            return -1;
        }
        // 15 = 1111(2) 代表上下左右
        int ret = 15;
        // 上下左右四个方向
        for (int i = 0; i < dirX.length / 2; i++) {
            int x = curPos.x + dirX[i];
            int y = curPos.y + dirY[i];
            if (!Mapinfo.isInMap(x, y) || set.contains(new Pos(x, y)) || Mapinfo.mapInfoOriginal[x][y] == -2) {
                ret &= ~(1 << 3 - i) & 0xFF;
            }
            if (ret <= 3 || (ret & 3) == 0) {
                return -1;
            }
        }

        // 斜左上 左右 左下 右下
        // 斜左上
        if ((ret & 10) == 10 && Mapinfo.isInMap(curPos.x - 1, curPos.y - 1)  && !set.contains(new Pos(curPos.x - 1, curPos.y - 1)) && Mapinfo.mapInfoOriginal[curPos.x - 1][ curPos.y - 1] != -2) {
            return 0;
        }
        // 斜右上
        if ((ret & 9) == 9 && Mapinfo.isInMap(curPos.x - 1, curPos.y + 1) && !set.contains(new Pos(curPos.x - 1, curPos.y + 1)) && Mapinfo.mapInfoOriginal[curPos.x - 1][ curPos.y + 1] != -2) {
            return 1;
        }
        // 斜左下
        if ((ret & 6) == 6 && Mapinfo.isInMap(curPos.x + 1, curPos.y - 1) && !set.contains(new Pos(curPos.x + 1, curPos.y - 1)) && Mapinfo.mapInfoOriginal[curPos.x + 1][ curPos.y - 1] != -2) {
            return 2;
        }
        // 斜右下
        if ((ret & 5) == 5 && Mapinfo.isInMap(curPos.x + 1, curPos.y + 1) && !set.contains(new Pos(curPos.x + 1, curPos.y + 1)) && Mapinfo.mapInfoOriginal[curPos.x + 1][ curPos.y + 1] != -2) {
            return 3;
        }
        return -1;
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

    public Point getPoint(int[][] maps, HashSet<Pos> pos1, boolean carry) {
        if (pos1.size() == 0) {
            return null;
        }
        ArrayList<Pos> openList = new ArrayList<>(); // 存储带拓展节点
        openList.add(targetPosition);
        maps[targetPosition.x][targetPosition.y] = 1; // 起点已探索
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
        return getPoint(maps, pos1, carry);
    }

    // 将得到的路径左边合并并返回结果坐标
    public  ArrayList<Point> getResult(boolean carry) {
        // 将结果返回，空载需要右移坐标，满载无需移动
        mergeResultList();
        fixRoute(carry);    // 修正得到的结果
        return result;
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
        result.add(mergeList.get(0));
        Point prePoint = mergeList.get(0);
        Pos prePos = Point2Pos(prePoint);
        for (int i = 1; i < size - 1; i++) {
            // 两点相邻，直接优化掉后面的点，可能有些激进，但是目前就这样处理，
            Point curPoint = mergeList.get(i);
            Pos curPos = Point2Pos(curPoint);
            if (prePos.y == curPos.y || prePos.x == curPos.x) {
                offsetPoint(prePoint, curPoint, prePos, curPos, carry);
                result.add(prePoint);
                result.add(curPoint);
            }
            else {
                result.add(curPoint);
            }
            prePoint = curPoint;
            prePos = curPos;
        }
        result.add(mergeList.get(mergeList.size() - 1));
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

    public void offsetPoint(Point startPoint, Point endPoint, Pos startPos, Pos endPos, boolean carry) {
        int step = 1;
        if (carry == true) {
            return;
        }
        // 垂直
        if (startPos.y == endPos.y) {
            if (!Mapinfo.isInMap(startPos.x, startPos.y - 1)) {  // 左边是墙，直接右移
                startPoint.x += 0.25;
                endPoint.x += 0.25;
                return;
            }
            else if (!Mapinfo.isInMap(startPos.x, startPos.y + 1)) {  // 右边是墙，直接左移
                startPoint.x -= 0.25;
                endPoint.x -= 0.25;
                return;
            }
            else {
                int turn = startPos.x < endPos.x ? 1 : -1;
                for (int x = startPos.x; (x - endPos.x) * turn <= 0; x += turn * step) {
                    if (Mapinfo.mapInfoOriginal[x][startPos.y - 1] == -2) {
                        startPoint.x += carry ? 0.5 : 0.25;
                        endPoint.x += carry ? 0.5 : 0.25;
                        return;
                    }
                    if (Mapinfo.mapInfoOriginal[x][startPos.y + 1] == -2) {
                        startPoint.x -= carry ? 0.5 : 0.25;
                        endPoint.x -= carry ? 0.5 : 0.25;
                        return;
                    }
                }
            }
        }
        else {  // 水平
            if (!Mapinfo.isInMap(startPos.x - 1, startPos.y)) {  // 上边是墙，直接下移
                startPoint.y -= 0.25;
                endPoint.y -= 0.25;
                return;
            }
            else if (!Mapinfo.isInMap(startPos.x + 1, startPos.y)) {  // 下边是墙，直接上移
                startPoint.y += 0.25;
                endPoint.y += 0.25;
                return;
            }
            else {
                int turn = startPos.y < endPos.y ? 1 : -1;
                for (int y = startPos.y; (y - endPos.y) * turn <= 0; y += turn * step) {
                    if (Mapinfo.mapInfoOriginal[startPos.x - 1][y] == -2) {
                        startPoint.y -= carry ? 0.5 : 0.25;
                        endPoint.y -= carry ? 0.5 : 0.25;
                        return;
                    }
                    if (Mapinfo.mapInfoOriginal[startPos.x + 1][y] == -2) {
                        startPoint.y += carry ? 0.5 : 0.25;
                        endPoint.y += carry ? 0.5 : 0.25;
                        return;
                    }
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
        mergeList.add(Pos2Point(startPos));    // 起点加入合并列表

        for (int index = 2; index < resultList.size(); index++) {
            Pos curPos = resultList.get(index);
            if (curPos.x == startPos.x || curPos.y == startPos.y) {
                prePos = curPos;
                continue;
            }
            Pair lineParam = calLineExpression(Pos2Point(startPos), Pos2Point(curPos));
            if (isObstacle(lineParam, startPos, curPos)) {   // 两点之间有障碍，这个点的前一个点就必须加入节点
                mergeList.add(Pos2Point(prePos));
                startPos = prePos;
            }
            prePos = curPos;
        }
        mergeList.add(Pos2Point(resultList.get(resultList.size() - 1)));    // 终点加入合并列表
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
