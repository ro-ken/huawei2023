package com.huawei.codecraft.core;

import java.util.*;
import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Astar;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.way.Pos;

// 攻击类
public class Attack {
    private static double attackRange = 3;               // 设定攻击距离，机器人在这个点攻击多远的距离
    public static Point[] attackPoint = new Point[4];   // 记录需要攻击的点,最多 4 个
    ArrayList<Robot> robots ;   // 负责攻击的机器人
    Map<Pos, Integer> posCnt;   // 记录路径pos点出现的次数
    

    public Attack(ArrayList<Robot> robots) {
        this.robots = robots;
        posCnt = new HashMap<>();
        initCntMap();
        initAttackPoint();
    }

    public void add2Posrange(Pos posAttack, HashSet<Pos> posRange, int range) {
        // 后续点不能出现在前一个点的范围内
        int startI = posAttack.x - range;
        int endI = posAttack.x + range;
        int startJ = posAttack.y - range;
        int endJ = posAttack.y + range;
        for (int i = startI; i <= endI; i++) {
            for (int j = startJ; j <= endJ; j++) {
                Pos cuPos = new Pos(i, j);
                if (!posRange.contains(cuPos)) {
                    posRange.add(cuPos);
                }
            }
        }
    }

    // 初始化对方路径路径点中的次数
    private void initCntMap() {
        // 获取敌方的工作台
        int length = Main.fighterStationNum;
        Station[] stations = Main.fighterStations;
        // 将station路径中的pos全部记录到hash表中
        for (int i = 0; i < length; i++) {
            Map<Point,HashSet<Pos>> emptyPos = stations[i].paths.getResSetMap(false);
            Map<Point,HashSet<Pos>> fullPos = stations[i].paths.getResSetMap(true);
            // 获取 Pos下 路径的所有点
            for (Point key : emptyPos.keySet()) {
                HashSet<Pos> posSet = emptyPos.get(key);
                for (Pos pos : posSet) {
                    // 对每个 Point 对应的 Pos 进行相关操作
                    if (posCnt.containsKey(pos)) {
                        posCnt.put(pos, posCnt.get(pos) + 1); // 自增
                    } else {
                        posCnt.put(pos, 1); // 初始化值为1
                    }
                }
            }
            for (Point key : fullPos.keySet()) {
                HashSet<Pos> posSet = emptyPos.get(key);
                for (Pos pos : posSet) {
                    // 对每个 Point 对应的 Pos 进行相关操作
                    if (posCnt.containsKey(pos)) {
                        posCnt.put(pos, posCnt.get(pos) + 1); // 自增
                    } else {
                        posCnt.put(pos, 1); // 初始化值为1
                    }
                }
            }
            
        }
    }

    // 初始化攻击的点
    private void initAttackPoint() {
        // 将 Map 转化为 List
        List<Map.Entry<Pos, Integer>> list = new ArrayList<>(posCnt.entrySet());

        // 按照值从大到小排序，使用 lambda 和流式 API
        list.sort((a, b) -> b.getValue() - a.getValue());

        // 将排序后的 Pos 存到 ArrayList 中
        ArrayList<Pos> sortedList = new ArrayList<>();
        for (Map.Entry<Pos, Integer> entry : list) {
            sortedList.add(entry.getKey());
        }

        int range = (int)(attackRange / 0.5);
        int index = 0;
        Pos posAttack = null;
        HashSet<Pos> posRange = new HashSet<>(); // 记录该点的范围，后续点不在这个范围才能加入到新的点
        for (int i = 0; i < attackPoint.length; i++) {
            // 确保得到的点在地图内，而且不会出现数组越界错误
            while (posAttack == null && index < sortedList.size()) {
                posAttack = sortedList.get(index++);
                if (!posRange.contains(posAttack) && Mapinfo.isInMap(posAttack.x, posAttack.y)) {
                    attackPoint[i] = Astar.Pos2Point(posAttack);
                    add2Posrange(posAttack, posRange, range);
                    break;
                }
                else {
                    posAttack = null; // 不在地图内的点，重新获取
                }  
            }
        }
    }
}
