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

    public static ArrayList<Point> getPath(boolean isEmpty,Point src, Point dest){
        if (src.equals(dest)){
            ArrayList<Point> res = new ArrayList<>();
            res.add(dest);
            return res;
        }

        int[][] fixMap = Main.mapinfo.getFixMap(isEmpty);
        Astar ast = new Astar(fixMap,src,dest);
        ast.search();
        return ast.getResult(!isEmpty);
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
        if (mergeList.size() < 2){
            return;
        }
        int size = mergeList.size();
        // 从起点开始逐一校准路径
        Pos starPos = mergeList.get(0);
        // 起点加入结果
        result.add(Pos2Point(starPos));
        // fixList.add(starPos);  
        for (int i = 1; i < size - 1; i++) {
            // 两点相邻，直接优化掉后面的点，可能有些激进，但是目前就这样处理， 
            Pos curPos = mergeList.get(i);
            if (Math.abs(curPos.x - starPos.x) + Math.abs(curPos.y - starPos.y) <= 2) {
                continue;
            }
            // 空载情况下加入结果队列的点，让点尽可能在中间,如果偏移点的时候已经发生移到中心，空载计算路径无需再次偏移
            if (!carry) {
                Point p = Pos2Point(curPos);
                if (isCriticalPos(curPos)) {
                    p.x -= 0.25;
                    p.y += 0.25;
                }
                result.add(p);
            }
            else {
                result.add(Pos2Point(curPos));
            }
           
            // fixList.add(curPos);
            starPos = curPos;
        }
        // fixList.add(mergeList.get(size - 1)); // 终点加入fixList
        // 终点加入结果
        result.add(Pos2Point(mergeList.get(size - 1)));
    }

    public boolean isCriticalPos(Pos curPos) {
        int x = curPos.x;
        int y = curPos.y;
        // 判断右边是否是墙
        if (y == Mapinfo.col - 1 || Mapinfo.mapInfoOriginal[x][y + 1] == -2 || Mapinfo.mapInfoOriginal[x - 1][y + 1] == -2 || Mapinfo.mapInfoOriginal[x + 1][y + 1] == -2) {
            return true;
        }
        // 判断上边是否是墙
        if (x == Mapinfo.row - 1 || Mapinfo.mapInfoOriginal[x + 1][y] == -2 || Mapinfo.mapInfoOriginal[x + 1][y - 1] == -2 || Mapinfo.mapInfoOriginal[x + 1][y + 1] == -2) {
            return true;
        }
        return false;
    }

    // 合并结果，将相同的坐标合并在一起
    public void mergeResultList() {
        // 没有结果，返回
        if (resultList.size() < 2){
            return;
        }
        int preDiffX, preDiffY, curDiffX, curDiffY;
        Pos prePos = resultList.get(0);
        Pos nextPos = resultList.get(1);
        preDiffX = nextPos.x - prePos.x;
        preDiffY = nextPos.y - prePos.y;

        mergeList.add(prePos);    // 起点加入合并列表
        prePos = nextPos;
        for (int index = 2; index < resultList.size() - 1; index++) {
            nextPos = resultList.get(index);
            curDiffX = nextPos.x - prePos.x;
            curDiffY = nextPos.y - prePos.y;
            // 节点存在变向,节点加入合并列表
            if (curDiffX != preDiffX || curDiffY != preDiffY) {
                // mergeList.add(prePos);
                mergeList.add(nextPos);
            }
            preDiffX = curDiffX;
            preDiffY = curDiffY;
            prePos = nextPos;
        }
        mergeList.add(resultList.get(resultList.size() - 1));    // 终点加入合并列表
    }

    // 将得到的坐标转为Point
    public static Point Pos2Point(Pos Pos) {
        // 空载需要向右上便宜0.25
        double x = Pos.y * 0.5 + 0.25 ;
        double y = 50 - (Pos.x * 0.5 + 0.25) ;
        return new Point(x, y);
    }

    // 将Point转为Pos用于地图索引 0-50 对应 0-99
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
                // System.out.println("flag "+flag);
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
                // System.out.println("flag "+flag);
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
