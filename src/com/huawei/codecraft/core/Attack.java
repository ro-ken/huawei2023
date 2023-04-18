package com.huawei.codecraft.core;

import java.util.*;
import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.*;
import com.huawei.codecraft.way.Astar;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.way.Pos;

// 攻击类
public class Attack {
    // 下面是全局静态属性
    public static double attackRange = 2.5;               // 设定攻击距离，机器人在这个点攻击多远的距离
    public static double smallRange = 3;               // 小圈无差别撞击
    public static double bigRange = 6;               // 大圈撞击迎面的敌人
    public static double maxFarToTarget = 10;               // 最多跟踪多远的距离，超过就回到目标点,图4可调大
    public static int maxWaitFps = 50 * 5;     // 最多等多久，就换地方
    static int[] dirX = {-1, 1, 0, 0, -1, -1, 1, 1};
    static int[] dirY = {0, 0, -1, 1, -1, 1, -1, 1};
    public static Point[] attackPoint = new Point[4];   // 记录需要攻击的点,最多 4 个
    public static HashSet<Robot> robots = new HashSet<>();   // 负责攻击的机器人
    public static Map<Pos, Double> posCnt = new HashMap<>();   // 记录路径pos点的价值

    //下面是对象属性
    public Point target;        // 目标点
    public AttackType attackType;       // 攻击类型
    public AttackStatus status = AttackStatus.ROAD;     // 机器人的状态
    public int arriveFrame;     // 到达这个点的 帧序号


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
    public static void addRobot(Robot robot,int i){
        addRobot(robot,attackPoint[i]);     // 默认进攻第一个点，后期可调整
    }

    public static void addRobot(Robot robot, Point target){
        addRobot(robot,target,AttackType.RUSH);    // 默认调用通用攻击模式
    }

    public static void addRobot(Robot robot, Point target,AttackType tp){
        if (target == null){
            Main.printLog("target is null");
            return;
        }
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

    public Point getAttackEnemy() {
        // 返回一个 最需要攻击的机器人位置，如果没有，返回null
        // 有两个圈，只要进了小圈就撞击
        // 如果介于小圈和大圈之间，那么只有对面迎面走来是才撞击
        // 敌人范围在大圈以外，忽视
        if (robot.enemy.size() == 0){
            return null;
        }

        double minDis = 10;
        Point res = null;
        for (RadarPoint radarPoint : robot.enemy) {
            Point tp = radarPoint.getPoint();
            double dis = robot.pos.calcDistance(tp);
            if (dis>bigRange){
                continue;
            }
            if (dis>smallRange){
                Main.printLog("in big range");
                if (!radarPoint.isCloseToMe(robot.pos)){
                    continue;   // 远离我的不考虑
                }
            }
            // 选择最近的点攻击
            if (dis < minDis){
                minDis = dis;
                res = tp;
            }
        }
        return res;
    }

    public void calcRouteFromNow() {
        // 计算从自身到目的地位置的路由

        ArrayList<Point> path = paths.getPath(robot.carry == 0,robot.pos);   // 获取路径
        HashSet<Pos> pos1 = paths.getResSet(robot.carry == 0,robot.pos);     // 获取结果
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

    private static void fixPos(Pos curPos) {
        // 将 Pos 点修正到路径中间，从而影响更多的地方
        // 上下有墙 往中间移动
        int flag = 15;
        for (int i = 0; i < dirX.length / 2; i++) {
            int x = curPos.x + dirX[i];
            int y = curPos.y + dirY[i];
            if (Mapinfo.isInMap(x, y) && Mapinfo.mapInfoOriginal[x][y] == -2) {
                flag &= ~(1 << 3 - i) & 0xFF;
            }
        }
        // 上有墙，往下移动，下有墙，往上移动
        if ((flag & 8) == 0 && Mapinfo.isInMap(curPos.x + 1, curPos.y) && Mapinfo.mapInfoOriginal[curPos.x + 1][curPos.y] != -2) {
            curPos.x = curPos.x + 1;
        }
        else if ((flag & 4) == 0 && Mapinfo.isInMap(curPos.x - 1, curPos.y) && Mapinfo.mapInfoOriginal[curPos.x - 1][curPos.y] != -2) {
            curPos.x = curPos.x - 1;
        }
        // 左边有墙，往右移动，右边有墙，往左移动
        if ((flag & 2) == 0 && Mapinfo.isInMap(curPos.x, curPos.y + 1) && Mapinfo.mapInfoOriginal[curPos.x][curPos.y + 1] != -2) {
            curPos.y = curPos.y + 1;
        }
        else if ((flag & 1) == 0 && Mapinfo.isInMap(curPos.x, curPos.y - 1) && Mapinfo.mapInfoOriginal[curPos.x][curPos.y - 1] != -2) {
            curPos.y = curPos.x + 1;
        }
    }

    // 初始化对方路径每个路径点的价值 价值 = 路径利润 / 路径时间损耗
    private static void initCntMap() {
        // 获取敌方的工作台信息
        int length = Main.fighterStationNum;
        Station[] stations = Main.fighterStations;

        // 将station路径中的pos全部记录到hash表中
        for (int i = 0; i < length; i++) {
            // 计算满载路径下的点,获取路径价值
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
                    fixPos(posAttack);
                    attackPoint[i] = Astar.Pos2Point(posAttack);
                    add2Posrange(posAttack, posRange, range);
                    break;
                 } 
             }
         }
     }  
}
