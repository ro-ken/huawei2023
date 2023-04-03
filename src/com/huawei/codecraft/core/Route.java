package com.huawei.codecraft.core;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.Line;
import com.huawei.codecraft.util.Path;
import com.huawei.codecraft.util.Point;

import java.util.ArrayList;
import java.util.concurrent.TransferQueue;

// 运动过程描述
public class Route{
    Robot robot;
    public Point target;    // 目标点
    public ArrayList<Point> path;   // 要经过的一些列点
    public int pathIndex;   // 指向next一个点
    public Point next;  // 下一个要到的点

    public Point vector;    //两点的向量
    public Point speed;     // 速度向量
    double clockwise = 0;    // 1为正向，-1为反向 ，0 不动

    double printLineSpeed;
    double printTurnSpeed;

    public double realDistance;
    public double realAngleDistance;    // 速度与目标向量的夹角
    public double setMinAngle;   // 设置临界减速角度
    public double setMinDistance;   // 设置临界减速距离
    double theoryTurn;
    double angleOffset;
    double stopMinDistance;
    double stopMinAngleDistance;
    public boolean endWaitMode = false; // 是否使用终点等待模式

    public static double emergencyDistanceCoef = 0.7;   // 半径乘子，每个机器人紧急距离，外人不得靠近
    public static double verticalSafeDistanceCoef = 1.5;   // 半径乘子，每个机器人紧急距离，外人不得靠近
    public static double lineSpeedCoef = 3;   // y = kx
    boolean isEmergency;// 是否紧急
    Point emergencyPos;    // 紧急机器人位置;

    public boolean tmpSafeMode = false;    // 是否去临时安全点
    boolean cycleRoadMode = false;    // 此时是否正在会车，圆形绕过去     每次得赋值 false

    public static double wallCoef = 3.2;      // 靠墙判定系数，多大算靠墙
    public static double perceptionDistanceCoef = 2;  // 刹车距离 * 2 + emergencyDistance;这个距离以内要做出反应
    public static double perceptionAngleRange = Robot.pi/4;   // 前方一半视野角度
    public static double staPerAngleRange = Robot.pi/6;   // 静态视野
    public static double emergencyAngle = Robot.pi/2;   // 前方一半视野角度
    public static double cornerStopMinDistance = 0.3;   // 在墙角，提前多少减速

    // 下面是新加参数
    public static double robotInPointDis = 0.2 ;    // 判断机器人到达某个点的相隔距离


    ArrayList<Integer> unsafeRobotIds;
    public int unsafeLevel;     //当前不安全级别  (1-3)

    public Route(Point tarPos,Robot robot,ArrayList<Point> path) {
        target = tarPos;
        vector = new Point();
        speed = new Point();
        this.robot = robot;
        unsafeRobotIds = new ArrayList<>();
        this.path = path;
        next = getNextPoint();    // 取出下一个点
        next = getNextPoint();    // 第一个点是自己的位置，不要
    }

    private Point getNextPoint() {
        if (pathIndex == path.size()){
            return target;
        }else {
            return path.get(pathIndex++);
        }
    }

    @Override
    public String toString() {
        return "Route{" +
                "clockwise=" + clockwise +
                ", realA=" + realAngleDistance +
                ", theoryTurn=" + theoryTurn +
                ", stopA=" + stopMinAngleDistance +
                '}';
    }

    private int calcAvoidBumpClockwise(Point speed,Point posVec) {
        int cw;
        double dot = speed.calcDot(posVec);

        if (dot < 0){
            // speed 在逆时针方位
            cw = 1;
        }else {
            cw = -1;
        }
        return cw;
    }

    public void calcClockwiseCoef() {
        // 紧急事件中只会有1个机器人,解决同转问题
        int id = unsafeRobotIds.get(0);
        if (Main.clockCoef[robot.id] != -1) {   // 判断过了，被前面的机器人置为了 -1
            // 计算两个向量的旋转角度是否一致

            Point speed2 = Main.robots[id].route.speed;
            Point vec1 = robot.pos.calcVector(Main.robots[id].pos);//new Point(Main.robots[id].pos.x - robot.pos.x, Main.robots[id].pos.y - robot.pos.y);
            Point vec2 = Main.robots[id].pos.calcVector(robot.pos);//new Point(robot.pos.x - Main.robots[id].pos.x, robot.pos.y - Main.robots[id].pos.y);
            double dot1 = speed.calcDot(vec1);
            double dot2 = speed2.calcDot(vec2);
            if (dot1 * dot2 <= 0) {
                Main.clockCoef[id] *= -1;
            }
        }
    }

    public void calcClockwise() {
        if (theoryTurn>robot.turn && theoryTurn - robot.turn < Robot.pi || theoryTurn<robot.turn && robot.turn - theoryTurn > Robot.pi){
            clockwise = 1;
        }else {
            clockwise = -1;
        }
    }

    // 计算向量的模长
    public double calcDeltaAngle(Point other) {
        return vector.calcDeltaAngle(other);
    }




    // 计算当前速度减速到0需要多长的距离
    private void calcMinDistance() {
        stopMinDistance = robot.getMinDistanceByCurSpeed();
        stopMinAngleDistance = robot.getMinAngleDistanceByCurSpeed();
    }

    public void calcParamEveryFrame() {
        calcVector();   // 距离矢量
        calcTheoryTurn();//理论偏角
        calcClockwise();    // 转动方向
        calcMinDistance();
    }


    public void calcSafePrintSpeed() {

        // 若工作台在角落，需要提前减速，
        if (next.nearWall()){
            stopMinDistance +=cornerStopMinDistance;
        }

        //计算线速度
        if (realAngleDistance < Robot.canForwardRad && stopMinDistance < realDistance){
            // 速度太小，加速
//            printLineSpeed = Robot.maxSpeed;
            if (realAngleDistance < Robot.maxForwardRad){
                printLineSpeed = Robot.maxSpeed;
            }else if (isNotInEdge()){       // todo  到时候得改一改
                printLineSpeed = Robot.rotateSpeedEquation.getY(realAngleDistance);
            }else {
                printLineSpeed = 0;
            }

        }else {
            // 减速
            printLineSpeed = 0;
        }

        //计算角速度
        if (stopMinAngleDistance < realAngleDistance){
            printTurnSpeed = Robot.maxRotate * clockwise;
        }else {
            printTurnSpeed = 0;
        }

    }

    public void calcTheoryTurn() {
        // 计算夹角弧度
        theoryTurn = Math.atan2(vector.y, vector.x);
        double tmp = Math.abs(robot.turn - theoryTurn);
        realAngleDistance = Math.min(tmp,2* Robot.pi-tmp);
//        Main.printLog("tmp"+tmp+"real"+realAngleDistance);
    }

    private void calcUnsafePrintSpeed() {
        // 紧急情况，和其他机器人靠得很近，逃离
//        Main.printLog(isEmergency);

        if (isEmergency){
            processEmergEvent();
        }else {
            processNormalEvent();
        }
//        calcClockwiseCoef();
//        Main.clockCoef[robot.id] = 1;
    }


    private void calcUnsafePrintSpeed2() {

        if (unsafeLevel == 0){
            processNormalEvent();
        }else if (unsafeLevel == 1){
            processLevel1Event();
        }else if (unsafeLevel == 2){
            processLevel2Event();
        }else if (unsafeLevel == 3){
            processLevel3Event();
        }
    }

    private void processLevel3Event() {
        // 处理机器人在脸上的情况

    }

    private void processLevel2Event() {
        // 处理前方有障碍物的情况

    }

    private void processLevel1Event() {
        // 安全等级最低，判断长远距离
        // 只处理直线碰撞情况，其他可能算不准，反而会花费时间
        // 若两边有车，不转向，车在一侧，转向，分为两种情况，交叉和异测

        int wise = 0;

        for(int i:unsafeRobotIds){
            // 判断速度的夹角是否接近180°
            Robot oth = Main.robots[i];
            double angle = vector.calcDeltaAngle(oth.route.vector);
            if (angle > Robot.pi - 0.1){   // 0.1 约为5°
                Point posVec = robot.pos.calcVector(oth.pos);
                wise += calcAvoidBumpClockwise(speed, posVec);
            }
        }
        if (wise == 0){
            clockwise = 0;
        }else if (wise >0){
            clockwise = 1;
        }else {
            clockwise = -1;
        }

    }

    // 关键参数，每一帧需要重新计算
    private void calcVector() {
        vector.x = next.x - robot.pos.x;
        vector.y = next.y - robot.pos.y;
        realDistance = vector.norm();
        speed.x = robot.lineVx;
        speed.y = robot.lineVy;
    }

    private double calcVerticalDistance(Point pos) {
        // 先算线速度，夹角小于pi/2 刹车，大于pi/2 全速

        Point posVec = robot.pos.calcVector(pos);
        double dis = posVec.norm();
        double angle = speed.calcDeltaAngle(posVec);
        double verDis = dis * Math.sin(angle); // 垂直距离 = 斜边 * sin (t)
        return verDis;
    }

     // 前方有障碍物，不能移动
     private boolean frontNotSafe() {
        boolean safe = true;

        for (int i = 0; i < 4; i++) {
            if (i == robot.id) continue;

            double dis = next.calcDistance(Main.robots[i].pos);
            Point vec = robot.pos.calcVector(Main.robots[i].pos);
            double verDis = calcVerticalDistance(Main.robots[i].pos);// 计算向量和速度垂直的距离
            double angle = calcDeltaAngle(vec);
            double verSafeDis = verticalSafeDistanceCoef * robot.getRadius() + 2 * robot.getRadius();
            if (angle < staPerAngleRange && dis < realDistance){
                if (Robot.judgeWidth && verDis > verSafeDis){
                    continue;
                }
                safe = false;
            }
        }
        return !safe;
    }


    // 当前移动是否安全
    private boolean isMoveSafe2() {
        int safeNum = 0;    // 安全的数量

        isEmergency = false;
        unsafeRobotIds.clear(); // 先清空
        unsafeLevel = 0;    // 级别置0
        double emgDis = emergencyDistanceCoef * robot.getRadius() + 2 * robot.getRadius();
        double verSafeDis = verticalSafeDistanceCoef * robot.getRadius() + 2 * robot.getRadius();
        double safeDis = perceptionDistanceCoef * stopMinDistance + emgDis;

        // todo  判断还有些问题
        // 只有在速度为0 的时候才预测轨迹是否重合
        for (int i = 0; i < 4; i++) {
            if (i == robot.id) continue;
            Robot oth = Main.robots[i];

            if (unsafeLevel<=1 && fitMeetCase(oth)){
                if (!robot.routeBumpDetect(oth)){
                    // 不会发生碰撞，直接进入下一次判断
                    safeNum ++;
                    continue;
                }else {
                    unsafeLevel = 1;    // 当前安全级别，把不安全的都加入
                    unsafeRobotIds.add(i);
                }
            }
            if (normalSafeDetect(oth,emgDis,verSafeDis,safeDis)){
                safeNum ++;
            }

        }

        return safeNum == 3;
    }

    // 当前移动是否安全
    private boolean isMoveSafe3() {
        int safeNum = 0;    // 安全的数量

        unsafeRobotIds.clear(); // 先清空
        unsafeLevel = 0;    // 级别置0
        double emgDis = emergencyDistanceCoef * robot.getRadius() + 2 * robot.getRadius();
        double verSafeDis = verticalSafeDistanceCoef * robot.getRadius() + 2 * robot.getRadius();
        double safeDis = perceptionDistanceCoef * stopMinDistance + emgDis;

        // todo  判断还有些问题
        // 只有在速度为0 的时候才预测轨迹是否重合
        for (int i = 0; i < 4; i++) {
            if (i == robot.id) continue;
            Robot oth = Main.robots[i];

            if (unsafeLevel<=1 && fitMeetCase(oth)){
                if (!robot.routeBumpDetect(oth)){
                    // 不会发生碰撞，直接进入下一次判断
                    safeNum ++;
                    continue;
                }else {
                    unsafeLevel = 1;    // 当前安全级别，把不安全的都加入
                    unsafeRobotIds.add(i);
                }
            }
            if (normalSafeDetect(oth,emgDis,verSafeDis,safeDis)){
                safeNum ++;
            }
        }

        return safeNum == 3;
    }


    // 当前移动是否安全
    private boolean isMoveSafe() {
        boolean safe = true;
        isEmergency = false;

        unsafeRobotIds.clear(); // 先清空
        double emgDis = emergencyDistanceCoef * robot.getRadius() + 2 * robot.getRadius();
        double verSafeDis = verticalSafeDistanceCoef * robot.getRadius() + 2 * robot.getRadius();
        double safeDis = perceptionDistanceCoef * stopMinDistance + emgDis;
        for (int i = 0; i < 4; i++) {
            if (i == robot.id) continue;
//            if (robot.carry ==0 && Main.robots[i].carry==0) continue;   // 空载不检测碰撞
            double dis = robot.pos.calcDistance(Main.robots[i].pos);
            if (dis<safeDis){
                // 目前只判断了两个条件，夹角和是否在内圈，后面第二圈也可以加一下判断
                Point vec = robot.pos.calcVector(Main.robots[i].pos);
                double verDis = calcVerticalDistance(Main.robots[i].pos);// 计算向量和速度垂直的距离
                double angle = calcDeltaAngle(vec);
                if (dis < emgDis && angle < emergencyAngle) {
                    // 目前只考虑一个紧急情况，若有多个，选取最紧急的
                    safe = false;
                    isEmergency = true;
                    emergencyPos = Main.robots[i].pos;
                    unsafeRobotIds.add(i);
                    break;  // 紧急情况
                }else {
                    if (angle < perceptionAngleRange){
                        if (Robot.judgeWidth && verDis > verSafeDis){
                            continue;
                        }
                        safe = false;
                        unsafeRobotIds.add(i);
                    }
                }
            }
        }
        return safe;
    }

    // 两个机器人符合对撞的条件
    private boolean fitMeetCase(Robot oth) {
        double dis = robot.pos.calcDistance(oth.pos);   //相对距离
        double d1 = robot.pos.calcDistance(next);
        double d2 = oth.pos.calcDistance(oth.route.next);
        if (dis >= d1 || dis >= d2){
            return false;   // 不符合条件
        }
        return (realAngleDistance < 0.1) && (oth.route.realAngleDistance < 0.1);
    }

    private boolean normalSafeDetect(Robot other,double emgDis, double verSafeDis, double safeDis) {
        boolean safe = true;

        double dis = robot.pos.calcDistance(other.pos);
        if (dis<safeDis){
            // 目前只判断了两个条件，夹角和是否在内圈，后面第二圈也可以加一下判断
            Point vec = robot.pos.calcVector(other.pos);
            double verDis = calcVerticalDistance(other.pos);// 计算向量和速度垂直的距离
            double angle = calcDeltaAngle(vec);
            if (dis < emgDis && angle < emergencyAngle) {
                // 目前只考虑一个紧急情况，若有多个，选取最紧急的
                if (unsafeLevel <3){
                    unsafeRobotIds.clear(); //重新队列
                }

                safe = false;
                isEmergency = true;
                emergencyPos = other.pos;
                unsafeRobotIds.add(other.id);
                unsafeLevel = 3;    // 不安全级别最高
            }else {
                // 若等于3 说明前面有不安全的节点，此节点此时不需要判断了
                if (unsafeLevel<=2 && verDis < verSafeDis){
                    if (unsafeLevel <2){
                        unsafeRobotIds.clear(); //重新队列
                    }
                    unsafeLevel = 2;
                    safe = false;
                    unsafeRobotIds.add(other.id);
                }
            }
        }
        return safe;
    }


    public boolean isNotInEdge() {

        boolean flag1 = robot.pos.x - wallCoef * robot.getRadius() < 0 || robot.pos.y - wallCoef * robot.getRadius() < 0;
        boolean flag2 = robot.pos.x + wallCoef * robot.getRadius() > 50 || robot.pos.y + wallCoef * robot.getRadius() > 50;

        return !flag1 && !flag2;
    }

    private void processEmergEvent() {
        // 先算线速度，夹角小于pi/2 刹车，大于pi/2 全速
        Point speed = new Point(robot.lineVx,robot.lineVy);
        Point posVec = robot.pos.calcVector(emergencyPos);
        double angle = speed.calcDeltaAngle(posVec);

        if (angle<Robot.pi/2){
            printLineSpeed = 0;
        }else {
            printLineSpeed = 6;
        }

        clockwise = calcAvoidBumpClockwise(speed,posVec);
        printTurnSpeed = Robot.maxRotate * clockwise * Main.clockCoef[robot.id];

    }

    private void processNormalEvent() {
        // 总体思想，前方物体在越靠近中心，速度越小，转向越大
        // 若有前方多个物体，速度按最靠中心的，转向选择最近的计算

        double speedCoef = 1;
        double rotateCoef = 1;
        double minDis = 10000;

        assert unsafeRobotIds.size() != 0;
        for(Integer i:unsafeRobotIds){
            Robot rot = Main.robots[i];
            Point posVec = robot.pos.calcVector(rot.pos);
            double angle = speed.calcDeltaAngle(posVec);
            double cos = speed.calcDeltaCos(posVec);
//            if ( 1-cos < speedCoef){
//                speedCoef = 1-cos;    //速度选择小的 安全
//            }
            double y = angle * lineSpeedCoef;
            if ( y < speedCoef){
                speedCoef = y;    //速度选择小的 安全
            }
            double dis = robot.pos.calcDistance(rot.pos);
            if (dis < minDis){
                rotateCoef = cos;
                // 不能处理，方向斜碰撞情况 \/
                clockwise = calcAvoidBumpClockwise(speed,posVec);
            }
        }
        printLineSpeed = Robot.maxSpeed * speedCoef;
        printTurnSpeed = Robot.maxRotate * clockwise * rotateCoef;
    }

    // 给定设置角度和速度时设置减速偏转策略
    public void rush() {
        calcSafePrintSpeed();   // 先计算安全速度

        if (!isMoveSafe()) {
            calcUnsafePrintSpeed();     // 不安全重新计算速度
//            calcUnsafePrintSpeed2();     // 不安全重新计算速度
        }

        Main.Forward(robot.id,printLineSpeed);
        Main.Rotate(robot.id,printTurnSpeed);
    }

    public void rush2() {
        if (blockDetect()){
            // 若发生阻塞，需要重新规划路线
            setNewPath();
        }

        calcSafePrintSpeed();   // 先计算安全速度
        cycleRoadMode = false;
        if (!tmpSafeMode){
            // 如果不是这个模式，需要检测和其他机器人是否碰撞
            if (canBump()){
                cycleRoadMode = true;
                    // 如果会碰撞，判断对方是否是临时模式，若不是，在做判断，若是，正常行走就行
                if (!Main.robots[unsafeRobotIds.get(0)].route.tmpSafeMode){
                    // 若会碰撞，判断路的宽度
                    if (roadIsWide()){
                        // 路很宽，绕一下
                        // 此时不考虑是否与墙发生碰撞，过了就行

                    }else {
                        // 路不够宽，并且对方也不是临时模式
                        cycleRoadMode = false;
                        setTmpSafeMode();
                    }
                }

            }
        }
        if (!cycleRoadMode){
            // 如果不是 正在错车，需要判断和墙的距离
            Point wall = frontHasWall();
            if (wall != null){
                // 前方有墙，需要稍微绕一绕


            }
        }else {


        }

        Main.Forward(robot.id,printLineSpeed);
        Main.Rotate(robot.id,printTurnSpeed);
    }

    private void setNewPath() {

    }

    private Point frontHasWall() {
        // 判断前面是否有墙挡着
        // 若有墙，返回最近的墙体，若无墙，返回空
        if (vector.x == 0) return null;
        Point belowP = getNearBumpWall(robot.belowLine);
        Point miP = getNearBumpWall(robot.midLine);
        Point topP = getNearBumpWall(robot.topLine);
        return selectClosestPoint(belowP,miP,topP);
    }

    private Point selectClosestPoint(Point p1, Point p2, Point p3) {
        // 选择一个里机器人最近的点

        return null;
    }

    private Point getNearBumpWall(Line line) {
        // 查看此条线段最近的墙体
        if (posIsWall(line.left)){
            return line.left.fixPoint2Center();
        }
        double step = line.left.x < line.right.x ? 0.5 : -0.5;
        Point s0 = line.left.fixPoint2Center();
        double x = s0.x + 0.24;
        Point s1 = line.getFixPoint(x);
        Point t = new Point(s0.x,s0.y);
        while (t.y<=s1.y){      // todo
            if (posIsWall(t)){
                return t;
            }
        }



        return null;
    }


    private void setTmpSafeMode() {
        // 判断两辆车，应该让谁避让
        Robot weakRobot = selectWeakRobot(Main.robots[unsafeRobotIds.get(0)]);
        weakRobot.route.selectTmpSafePoint();
        weakRobot.route.tmpSafeMode = true;
    }

    // 选择安全点，到安全点去
    private void selectTmpSafePoint() {

    }

    private Robot selectWeakRobot(Robot oth) {
        // 判断自己和另一个机器人谁更弱小，谁让路
        // todo 后面可以修改
        if (robot.carry == 1 && oth.carry == 0){
            return oth;
        }
        if (robot.carry == 0 && oth.carry == 1){
            return robot;
        }
        // 比较两个节点剩余的路程，远的让路
        int fps1 = calcLeftFps();
        int fps2 = oth.route.calcLeftFps();

        if (fps1 < fps2){
            return oth;
        }else {
            return robot;
        }
    }

    // 计算剩下的路还剩多少fps
    private int calcLeftFps() {
        int fps = robot.pos.distanceToFps(robot.carry == 0,next);  // 目前还剩多少距离
        fps += Path.calcPathFps(robot.carry == 0,path,pathIndex-1);
        return fps;
    }

    private boolean roadIsWide() {
        // 判断路的宽度是否够两个车过
        // 先找出机器人所在点的位置，以及方向
        // 若有是载物的，判断是否小于5个点，其他情况判断是否小于4个点
        // 若绝对值小于45度，判断纵向的点是否
        int minWide = calcMinWide(Main.robots[unsafeRobotIds.get(0)]);

        int realWide = 0;
        double angle = vector.calcDeltaAngle(new Point(1, 0));
        if (angle > Robot.pi/4 && angle < Robot.pi * 3 /4){
            realWide = calcVerticalWide(robot.pos);
        }else {
            realWide = calcHorizontalWide(robot.pos);
        }

        return minWide <= realWide;
    }

    private int calcHorizontalWide(Point pos) {
        int wide = 1;   // 本身肯定不是墙

        for (int i = 1; i < 10; i++) {
            // 每一格式0.5
            if (posIsWall(pos.x+i*0.5,pos.y)){
                break;
            }
            wide ++;
        }

        for (int i = 1; i < 10; i++) {
            // 每一格式0.5
            if (posIsWall(pos.x-i*0.5,pos.y)){
                break;
            }
            wide ++;
        }

        return wide;
    }

    private int calcVerticalWide(Point pos) {
        int wide = 1;   // 本身肯定不是墙

        for (int i = 1; i < 10; i++) {
            // 每一格式0.5
            if (posIsWall(pos.x,pos.y+i*0.5)){
                break;
            }
            wide ++;
        }

        for (int i = 1; i < 10; i++) {
            // 每一格式0.5
            if (posIsWall(pos.x,pos.y-i*0.5)){
                break;
            }
            wide ++;
        }

        return wide;
    }

    private boolean posIsWall(double x, double y) {
        if (notInMap(x,y)){
            return true;
        }
        // 找出 x,y 属于哪一个点的区域
        int x1 = fixPointX(x);
        int y1 = fixPointY(y);
        // -2 对应的是墙
        return Main.wallMap[x1][y1] == -2;
    }
    private boolean posIsWall(Point point) {
        return posIsWall(point.x,point.y);
    }

    private int fixPointX(double x) {
        // 找出 对应地图坐标的那个点
        return (int) (x / 0.5);
    }
    private int fixPointY(double y) {
        // 找出 对应地图坐标的那个点
        return (int) ((50-y) / 0.5);
    }

    private boolean notInMap(double x, double y) {
        // 是否不在地图内
        boolean flag1 = robot.pos.x <= 0 || robot.pos.y <= 0;
        boolean flag2 = robot.pos.x >= 50 || robot.pos.y >= 50;

        return flag1 || flag2;
    }

    private int calcMinWide(Robot oth) {
        // 返回最小宽度，及空点的个数
        if (robot.carry == 0 && oth.carry == 0){
            return 4;
        }else {
            // todo 两个载物的，5格容易撞，后期可改成6格
            return 5;
        }
    }

    private boolean canBump() {
        boolean safe = true;
        isEmergency = false;

        unsafeRobotIds.clear(); // 先清空
        double emgDis = emergencyDistanceCoef * robot.getRadius() + 2 * robot.getRadius();
        double verSafeDis = verticalSafeDistanceCoef * robot.getRadius() + 2 * robot.getRadius();
        double safeDis = perceptionDistanceCoef * stopMinDistance + emgDis;

        // 若有冲突，选择最近的点
        double minDis = 1000000;
        for (int i = 0; i < 4; i++) {
            if (i == robot.id) continue;
            double dis = robot.pos.calcDistance(Main.robots[i].pos);
            if (dis<safeDis){
                // 判断夹角和是否在内圈
                Point vec = robot.pos.calcVector(Main.robots[i].pos);
                double verDis = calcVerticalDistance(Main.robots[i].pos);// 计算向量和速度垂直的距离
                double angle = calcDeltaAngle(vec);
                if (dis < emgDis && angle < emergencyAngle) {
                    // 目前只考虑一个紧急情况，若有多个，选取最紧急的
                    safe = false;
                    isEmergency = true;
                    emergencyPos = Main.robots[i].pos;
                    unsafeRobotIds.add(i);
                    break;  // 紧急情况
                }else {
                    if (angle < perceptionAngleRange && verDis < verSafeDis){
                        safe = false;
                        if (dis < minDis){
                            minDis  = dis;
                            unsafeRobotIds.clear();
                            unsafeRobotIds.add(i);
                        }
                    }
                }
            }
        }
        return !safe;
    }

    private boolean blockDetect() {

        return false;
    }

    // 是否使用终点等待模式，防止终点碰撞
    private void useEndWaitMode() {
        // 若到了终点附件需判断,如果预定数量大于1，并且我不是最近的，或者我前面有其他station，需要减速暂停，终点等待模式
        if (robot.nextStation.bookNum >= 1 && !target.nearWall()){  // 靠墙容易堵住
            if(realDistance < robot.nextStation.getSafeDis()){
                boolean flag1 = selfNotClosest();
                boolean flag2 = frontNotSafe();

                if (flag1 || flag2){
                    printLineSpeed = 0;     // 需要暂停

                    //计算角速度，朝向目标
                    if (stopMinAngleDistance < realAngleDistance){
                        printTurnSpeed = Robot.maxRotate * clockwise;
                    }else {
                        printTurnSpeed = 0;
                    }
                }
            }
        }
    }

    // 有很多机器人去这个工作站，我不是最近的一个
    private boolean selfNotClosest() {
        boolean flag = false;
        for (int i = 0; i < 4; i++) {
            if (i!=robot.id){
                Route r = Main.robots[i].route;
                if (r == null || !r.target.equals(target)) continue;
                if (i>robot.id){
                    // 前面距离算过了，后面距离还没算
                    r.calcVector();
                }
                flag = true;
                if (realDistance < r.realDistance) return false;
            }
        }
        return flag;
    }

    // 是否到达下一个点
    public boolean arriveNext() {
        double dis = robot.pos.calcDistance(next);
        return dis <= robotInPointDis;
    }

    // 更换下一个点
    public void updateNext() {
        if (path.size() == 0){
            Main.printLog("path error");
        }
        next = getNextPoint();
    }
}