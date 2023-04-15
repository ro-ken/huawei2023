package com.huawei.codecraft.util;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.way.Astar;
import com.huawei.codecraft.way.Pos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

// 路径
public class Path {
    public Point src;
    public Map<Point,ArrayList<Point>> emptyPathMap = new HashMap<>();  // 空载到达Point 需要的路径点
    public Map<Point, HashSet<Pos>> emptyResSetMap = new HashMap<>();  // 空载到达Point 需要的路径点
    public Map<Point,ArrayList<Point>> fullPathMap = new HashMap<>();  // 满载到达Point 需要的路径点
    public Map<Point,HashSet<Pos>> fullResSetMap = new HashMap<>();  // 满载到达Point 需要的路径点
    public Map<Point,Integer> emptyFpsMap = new HashMap<>();  // 空载到达Point 需要的fps
    public Map<Point,Integer> fullFpsMap = new HashMap<>();  // 满载到达Point 需要的fps

    public Path(Point src) {
        this.src = src;
    }

    // 获取路径
//    public ArrayList<Point> getPath(boolean isEmpty, Point dest) {
//        // 1、如果存在路径，返回
//        ArrayList<Point> path = getInterPath(isEmpty,dest);
//        if (path!=null){
//            return path;
//        }else {
//            // 2、不存在，判断对方是否已计算过，直接逆序拿来用
//            if (Main.pointStaMap.containsKey(dest)){
//                ArrayList<Point> dest2src = Main.pointStaMap.get(dest).paths.getInterPath(isEmpty,src);
//                if (dest2src != null){
//                    // 要把此条路径翻转，存到自己的路径表中
//                    path = reversePath(dest2src);
//                }
//            }
//            if (path == null){
//                // 如果还为空，在调用A*算法计算路径
//                path = Astar.getPath(isEmpty, src, dest);
//            }
//            Map<Point,ArrayList<Point>> paths = getPathMap(isEmpty);
//            paths.put(dest,path);     // 保存路径，下次备用
//            return path;
//        }
//    }

    public ArrayList<Point> getPath(boolean isEmpty, Point dest) {
        // 1、如果存在路径，返回
        ArrayList<Point> path = getInterPath(isEmpty,dest);
        HashSet<Pos> set = new HashSet<>();
        if (path!=null){
            return path;
        }else {
            // 2、不存在，判断对方是否已计算过，直接逆序拿来用
            if (Main.pointStaMap.containsKey(dest)){
                ArrayList<Point> dest2src = Main.pointStaMap.get(dest).paths.getInterPath(isEmpty,src);
                if (dest2src != null){
                    // 要把此条路径翻转，存到自己的路径表中
                    path = reversePath(dest2src);
                    set = Main.pointStaMap.get(dest).paths.getInterSet(isEmpty,src);
                }
            }
            if (path == null){
                // 如果还为空，再调用A*算法计算路径
                path = Astar.getPathAndResult(isEmpty, src, dest,set);
            }
            Map<Point,ArrayList<Point>> paths = getPathMap(isEmpty);
            Map<Point,HashSet<Pos>> pos1 = getResSetMap(isEmpty);
            paths.put(dest,path);     // 保存路径，下次备用
            if (set != null){
                pos1.put(dest,set);     // 保存路径，下次备用
            }
            return path;
        }
    }
    // 翻转路径
    public static ArrayList<Point> reversePath(ArrayList<Point> path) {
        ArrayList<Point> res = new ArrayList<>();
        for (int i = path.size()-1; i >=0 ; i--) {
            res.add(path.get(i));
        }
        return res;
    }

    // 内部方法，获取有效路径
    private ArrayList<Point> getInterPath(boolean isEmpty, Point dest) {
        Map<Point,ArrayList<Point>> path = getPathMap(isEmpty);
        if (!path.containsKey(dest)){
            return null;    // 不包含此条路径，返回空
        }
        return path.get(dest);
    }

    public HashSet<Pos> getResSet(boolean isEmpty, Point dest) {
        Map<Point,HashSet<Pos>> posMap = getResSetMap(isEmpty);
        if (Main.pointStaMap.containsKey(dest)){
            HashSet<Pos> set = Main.pointStaMap.get(dest).paths.getInterSet(isEmpty, src);
            if (set != null && set.size()>1){
                set = Main.pointStaMap.get(dest).paths.getInterSet(isEmpty,src);
                posMap.put(dest,set);     // 保存路径，下次备用
                return set;
            }
        }
        if (!posMap.containsKey(dest) ||posMap.get(dest) == null ||posMap.get(dest).size()<3){

            Map<Point,ArrayList<Point>> paths = getPathMap(isEmpty);
            HashSet<Pos> pos1 = new HashSet<>();
            ArrayList<Point> path = Astar.getPathAndResult(isEmpty, src, dest,pos1);
            paths.put(dest,path);     // 保存路径，下次备用
            posMap.put(dest,pos1);     // 保存路径，下次备用
        }
        return posMap.get(dest);
    }

//
//    public HashSet<Pos> getResSet(boolean isEmpty, Point dest) {
//        Map<Point,HashSet<Pos>> path = getResSetMap(isEmpty);
//        if (!path.containsKey(dest)){
//            return null;    // 不包含此条路径，返回空
//        }
//        return path.get(dest);
//    }

    private HashSet<Pos> getInterSet(boolean isEmpty, Point dest) {
        Map<Point,HashSet<Pos>> path = getResSetMap(isEmpty);
        if (!path.containsKey(dest)){
            return null;    // 不包含此条路径，返回空
        }
        return path.get(dest);
    }


    // 获取路径所需的fps
    public int getPathFps(boolean isEmpty, Point dest) {
        // 1、如果存在路径，直接返回
        Map<Point,Integer> pathFps = getFpsMap(isEmpty);
        if (pathFps.containsKey(dest)){
            return pathFps.get(dest);
        }else {
            // 2、不存在，重新计算
            int fps = 0;
            if (Main.pointStaMap.containsKey(dest)){
                Map<Point,Integer> dest2srcFps = Main.pointStaMap.get(dest).paths.getFpsMap(isEmpty);
                if (dest2srcFps.containsKey(src)){
                    // 获取对方的时间，计算是相同的
                    fps = dest2srcFps.get(src);
                }
            }
            if (fps == 0){
                // 对方也没存，自己计算
                ArrayList<Point> path = getPath(isEmpty,dest);
                if (path.size() == 0){
                    fps += Main.unreachableCost;     // 路径不可达
                }else {
                    fps = calcPathFps(isEmpty,path);
                }
            }
            pathFps.put(dest,fps);      // 保存结果
            return fps;
        }
    }

    public static int calcPathFps(boolean isEmpty, ArrayList<Point> path) {
        int fps = 0;
        for (int i = 0; i < path.size()-1; i++) {
            fps += path.get(i).distanceToFps(isEmpty,path.get(i+1));
            fps += 20;  // 默认旋转时间20fps,尽量走直线
        }
        return fps;
    }

    public static int calcPathFps(boolean isEmpty, ArrayList<Point> path,int start) {
        int fps = 0;
        for (int i = start; i < path.size()-1; i++) {
            fps += path.get(i).distanceToFps(isEmpty,path.get(i+1));
            fps += 20;  // 默认旋转时间20fps,尽量走直线
        }
        return fps;
    }

    private Map<Point,Integer> getFpsMap(boolean isEmpty) {
        return isEmpty? emptyFpsMap:fullFpsMap;
    }

    private Map<Point,ArrayList<Point>> getPathMap(boolean isEmpty) {
        return isEmpty? emptyPathMap:fullPathMap;
    }
    public Map<Point,HashSet<Pos>> getResSetMap(boolean isEmpty) {
        return isEmpty? emptyResSetMap:fullResSetMap;
    }
}
