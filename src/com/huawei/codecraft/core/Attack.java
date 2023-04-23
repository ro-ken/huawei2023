package com.huawei.codecraft.core;

import java.util.*;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.*;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.way.Pos;


/**
 * ClassName: Attack
 * Package: com.huawei.codecraft.core
 * Description: 攻击类
 */
public class Attack {
    // 下面是全局静态属性
    public static double attackRange = 2.5;  // 设定攻击距离，机器人在这个点攻击多远的距离
    public static double smallRange = 3;  // 小圈无差别撞击
    public static double bigRange = 6;  // 大圈撞击迎面的敌人
    public static double maxFarToTarget = 25;  // 最多跟踪多远的距离，超过就回到目标点
    public static int maxWaitFps = 50 * 5;  // 最多等多久，就换地方
    static int[] dirX = {-1, 1, 0, 0, -1, -1, 1, 1};
    static int[] dirY = {0, 0, -1, 1, -1, 1, -1, 1};
    public static Point[] attackPoint = new Point[7];  // 记录需要攻击的点,最多 4 个


    public static HashSet<Robot> robots = new HashSet<>();  // 负责攻击的机器人
    public static Map<Pos, Double> posCnt = new HashMap<>();  // 记录路径pos点的价值

    //下面是对象属性
    public Point target;  // 目标点
    public Point curtar;  // 正在追的目标
    public static Map<Point, Integer> curTargets = new HashMap<>();  // 当前追踪的目标,以及其被占用的个数
    public AttackType attackType;  // 攻击类型
    public AttackStatus status = AttackStatus.ROAD;  // 机器人的状态
    public int arriveFrame;  // 到达这个点的 帧序号
    public int pointSeq = 0;

    public Path paths;
    public Robot robot;

    /**
     * 初始化
     */
    public static void init() {
        initAttackPoint();
    }

    /**
     * 构造器
     * @param robot 机器人
     * @param target 目标
     * @param tp 攻击类型
     */
    public Attack(Robot robot, Point target, AttackType tp) {
        this.robot = robot;
        robot.attack = this;
        this.target = target;
        this.attackType = tp;
        paths = new Path(target);  //建立一条路径
    }

    /**
     * 添加攻击机器人
     * @param robot 指定机器人
     */
    public static void addRobot(Robot robot) {
        addRobot(robot, attackPoint[0]);  // 默认进攻第一个点，后期可调整
    }

    /**
     * 添加攻击机器人
     * @param robot 指定机器人
     * @param i 进攻几号点
     */
    public static void addRobot(Robot robot, int i) {
        addRobot(robot, attackPoint[i]);  // 默认进攻第一个点，后期可调整

    }

    /**
     *  添加攻击机器人
     * @param robot 指定机器人
     * @param i 进攻几号点
     * @param ty 攻击类型
     */
    public static void addRobot(Robot robot, int i, AttackType ty) {
        addRobot(robot, attackPoint[i], ty);  // 默认进攻第一个点，后期可调整
    }

    /**
     *  添加攻击机器人
     * @param robot 指定机器人
     * @param target 目标
     */
    public static void addRobot(Robot robot, Point target) {
        addRobot(robot, target, AttackType.RUSH);  // 默认调用通用攻击模式
    }

    /**
     *  添加攻击机器人
     * @param robot 指定机器人
     * @param target 目标
     * @param tp 攻击类型
     */
    public static void addRobot(Robot robot, Point target, AttackType tp) {
        if (target == null) {
            Main.printLog("target is null");
            return;
        }
        Attack attack = new Attack(robot, target, tp);
        Attack.robots.add(robot);
        // 计算路线
        attack.calcRouteFromNow();
    }

    /**
     * 攻击范围指定
     * @param posAttack 攻击点
     * @param posRange 攻击点范围
     * @param range 范围设定
     */
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

    /**
     * 返回一个 最需要攻击的机器人位置，如果没有，返回null
     * 有两个圈，只要进了小圈就撞击
     * 如果介于小圈和大圈之间，那么只有对面迎面走来是才撞击
     * 敌人范围在大圈以外，忽视
     * @return 要攻击的敌人目标
     */
    public Point getAttackEnemy() {
        if (robot.enemy.size() == 0) {
            return null;
        }

        double minDis = 1000;
        Point res = null;
        for (RadarPoint radarPoint : robot.enemy) {
            Point tp = radarPoint.getPoint();
            double dis = robot.pos.calcDistance(tp);
            if (attackType == AttackType.RUSH) {
                if (dis > bigRange) {
                    continue;
                }
                if (dis > smallRange) {
                    Main.printLog("in big range");
                    if (!radarPoint.isCloseToMe(robot.pos)) {
                        continue;   // 远离我的不考虑
                    }
                }
            }

            if (Attack.curTargets.containsKey(tp)) {
                continue;   // 超过1个就不追了，划不来
            }

            // 选择最近的点攻击
            if (dis < minDis) {
                minDis = dis;
                res = tp;
            }
        }


        if (res != null) {
            if (!Attack.curTargets.containsKey(res)) {
                // 不包含,创建
                Attack.curTargets.put(res, 1);
            } else {
                Attack.curTargets.put(res, 2);
            }
        }

        return res;
    }

    /**
     * 图4 单独判断
     * @return 要攻击的敌人目标
     */
    public Point getAttackEnemy2() {
        if (robot.enemy.size() == 0) {
            return null;
        }

        double minDis = 1000;
        Point res = null;
        for (RadarPoint radarPoint : Main.curEnemys) {
            Point tp = radarPoint.getPoint();
            if (Attack.curTargets.containsKey(tp)) {
                if (Attack.curTargets.get(tp) >= 1) {
                    continue;  // 超过2个就不追了，划不来
                }
            }

            double dis = robot.pos.calcDistance(tp);
            // 选择最近的点攻击
            if (dis < minDis) {
                minDis = dis;
                res = tp;
            }
        }

        if (res != null) {
            if (!Attack.curTargets.containsKey(res)) {
                // 不包含,创建
                Attack.curTargets.put(res, 1);
            } else {
                Attack.curTargets.put(res, 2);
            }
        }

        return res;
    }

    /**
     * 计算从自身到目的地位置的路由
     * @return 路由信息
     */
    public boolean calcRouteFromNow() {
        ArrayList<Point> path = paths.getPath(robot.carry == 0, robot.pos);  // 获取路径
        HashSet<Pos> pos1 = paths.getResSet(robot.carry == 0, robot.pos);  // 获取结果
        path = Path.reversePath(path);
        robot.route = new Route(target, robot, path, pos1);
        if (path.size() == 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 路径权重是根据路径利润算出来的，路径越长，时间收益越少，长度按照
     * @param type 类型
     * @param psoCount 帧数
     * @return 理论金额
     */
    private static double calcPathMoney(int type, int psoCount) {
        int baseMoney = Goods.item[type].earn;
        double theoryMoney = baseMoney * (1 - (double) psoCount / 1000);  // 不算碰撞
        return theoryMoney;
    }


    /**
     * 初始化攻击的点，依次选取每个工作台价值最高的作为攻击点
     */
    private static void initAttackPoint() {
        // 获取敌方的工作台信息
        Map<Integer, ArrayList<Station>> fighterStationsMap = Main.fighterStationsMap;
        Station[] fighterStations = Main.fighterStations;

        // 遍历 Map
        for (Map.Entry<Integer, ArrayList<Station>> entry : fighterStationsMap.entrySet()) {
            int type = entry.getKey();
            if (type >= 8) {
                continue;
            }
            ArrayList<Station> fighterStationsList = entry.getValue();
            int minPosCnt = 1000000;
            int chooseId = -1;
            int maxPathCnt = -1;
            // 每个工作台选取一个价值最高的作为攻击对象，价值最高即路径最短
            for (int i = 0; i < fighterStationsList.size(); i++) {
                Station station = fighterStationsList.get(i);
                int id = station.id;
                int curPosCnt = 0;
                int curPathCnt = 0;

                // 取size的大小和路径数量两个维度，路径通路越大越好，在路径通路相同的情况下，选择路径数量最少的工作台
                Map<Point, HashSet<Pos>> fullPos = fighterStations[id].paths.getResSetMap(false);
                // 计算路径通量

                // 计算路径点的总数量
                for (Point key : fullPos.keySet()) {
                    HashSet<Pos> posSet = fullPos.get(key);
                    if (posSet.size() != 0) {
                        curPathCnt++;
                    }
                    curPosCnt += posSet.size();
                }
                // 路径通量优先级大于路径数量
                if (maxPathCnt < curPathCnt) {
                    maxPathCnt = curPathCnt;
                    chooseId = id;
                    minPosCnt = curPosCnt;
                } else if (maxPathCnt == curPathCnt) {  // 路径通路相等的情况下，比路径长度，短的优先
                    if (minPosCnt > curPosCnt) {
                        minPosCnt = curPosCnt;
                        chooseId = id;
                    }
                }
            }
            // 将该工作台加入到attackPoint
            attackPoint[type - 1] = fighterStations[chooseId].pos;
        }

    }

    /**
     * 更换攻击点目标
     */
    public void changeTarget() {
        // 一段时间没有敌人，换一个攻击点
        int seq = getFitPlace();
        if (attackPoint[seq] == null) return;
        addRobot(robot, attackPoint[seq], attackType);  // 延续上一次的攻击类型
        robot.attack.pointSeq = seq;
    }

    /**
     * 找一个合适的点
     * 找一个没人把手的点
     * @return 点信息
     */
    private int getFitPlace() {
        int len = attackPoint.length;

        for (int i = 0; i < len; i++) {
            int seq = (pointSeq + i) % len;
            Point point = attackPoint[seq];
            if (point == null) continue;

            boolean occupy = false;
            for (Robot rob : robots) {
                if (rob.attack.target.equals(point)) {
                    occupy = true;
                    break;
                }
            }
            if (!occupy) {
                return seq;  // 这个点没人占用,就这个点
            }
        }

        return 0;
    }

    /**
     * 打印攻击点，用于调试
     */
    public static void printPoint() {
        Main.printLog(Main.zoneMap.size());
        Main.printLog("attackPoint");
        for (int i = 0; i < attackPoint.length; i++) {
            Main.printLog(attackPoint[i]);
        }
    }
}
