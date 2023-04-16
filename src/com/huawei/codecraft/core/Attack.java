package com.huawei.codecraft.core;

import java.util.*;
import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.AttackType;
import com.huawei.codecraft.util.Path;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.Goods;
import com.huawei.codecraft.way.Astar;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.way.Pos;

// 攻击类
public class Attack {
    // 下面是全局静态属性
    private static double attackRange = 2;               // 设定攻击距离，机器人在这个点攻击多远的距离
    public static Point[] attackPoint = new Point[4];   // 记录需要攻击的点,最多 4 个
    public static HashSet<Robot> robots = new HashSet<>();   // 负责攻击的机器人
    public static Map<Pos, Double> posCnt = new HashMap<>();   // 记录路径pos点的价值

    //下面是对象属性
    public Point target;        // 目标点
    public AttackType attackType;       // 攻击类型
    public Path paths;
    public Robot robot;

    public static void init(){
        initCntMap();
        initAttackPoint();
    }

    public Attack(Robot robot ,Point target,AttackType tp) {
        this.robot = robot;
        robot.attack = this;
        this.target = target;
        this.attackType = tp;
        paths = new Path(target);    //建立一条路径
    }

    public static void addRobot(Robot robot){
        addRobot(robot,attackPoint[0]);     // 默认进攻第一个点，后期可调整
    }

    public static void addRobot(Robot robot, Point target){
        addRobot(robot,target,AttackType.BLOCK);    // 默认调用阻塞模式
    }

    public static void addRobot(Robot robot, Point target,AttackType tp){
        robot.earn = false; // 不是赚钱模式
        Attack attack = new Attack(robot,target,tp);
        Attack.robots.add(robot);
        // 计算路线
        attack.calcRouteFromNow();
    }

    public static void add2Posrange(Pos posAttack, HashSet<Pos> posRange, int range) {
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

    public void calcRouteFromNow() {
        // 计算从自身到目的地位置的路由

        ArrayList<Point> path = paths.getPath(robot.carry == 0,robot.pos);   // 第一次，计算初始化的路径
        HashSet<Pos> pos1 = paths.getResSet(robot.carry == 0,robot.pos);
        path = Path.reversePath(path);
        robot.route = new Route(target,robot,path,pos1);
    }

    // 路径权重是根据路径利润算出来的
    private static double calcPathMoney(int type, int psoCount) {
        int baseMoney = Goods.item[type].earn;
        double speed = Main.isBlue ? 7 : 6;
        int fps = (int)(50 * psoCount * 0.5 / speed);
        double theoryMoney = (baseMoney * Robot.calcTimeValue(fps));   // 不算碰撞
        return theoryMoney;
    }

   // 初始化对方路径路径点中的次数
   private static void initCntMap() {
    // 获取敌方的工作台
    int length = Main.fighterStationNum;
    Station[] stations = Main.fighterStations;
    // 将station路径中的pos全部记录到hash表中
    for (int i = 0; i < length; i++) {
        // 计算满载路径下的点
        int type = stations[i].type;
        Map<Point,HashSet<Pos>> fullPos = stations[i].paths.getResSetMap(false);
        for (Point key : fullPos.keySet()) {
            HashSet<Pos> posSet = fullPos.get(key);
            double pathWeight = calcPathMoney(type, posSet.size() / 3);
            for (Pos pos : posSet) {
                // 对每个 Point 对应的 Pos 进行相关操作
                if (posCnt.containsKey(pos)) {
                    posCnt.put(pos, posCnt.get(pos) + pathWeight); // 自增
                } else {
                    posCnt.put(pos, pathWeight); // 初始化值为1
                }
            }
        }
    }
}

    // 初始化攻击的点
    private static void initAttackPoint() {
        // 将Map<Pos, Double> posCnt按照value的从大到小排序
         List<Map.Entry<Pos, Double>> entryList = new ArrayList<>(posCnt.entrySet());
         Comparator<Map.Entry<Pos, Double>> comparator = (o1, o2) -> Double.compare(o2.getValue(), o1.getValue());
         Collections.sort(entryList, comparator);
 
         // 将排序后的 Pos 存到 ArrayList 中
         ArrayList<Pos> sortedList = new ArrayList<>();
         for (Map.Entry<Pos, Double> entry : entryList) {
             sortedList.add(entry.getKey());
         }
 
 
         int range = (int)(attackRange / 0.5);
         int index = 0;
         Pos posAttack = null;
         HashSet<Pos> posRange = new HashSet<>(); // 记录该点的范围，后续点不在这个范围才能加入到新的点
         for (int i = 0; i < attackPoint.length; i++) {
             // 确保得到的点在地图内，而且不会出现数组越界错误
             while (index < sortedList.size()) {
                 posAttack = sortedList.get(index++);
                 if (!posRange.contains(posAttack) &&  Mapinfo.isInMap(posAttack.x, posAttack.y) && Mapinfo.mapInfoOriginal[posAttack.x][posAttack.y] != -2) {
                     attackPoint[i] = Astar.Pos2Point(posAttack);
                     add2Posrange(posAttack, posRange, range);
                     break;
                 } 
             }
         }
     }
}
