package com.huawei.codecraft.util;

import com.huawei.codecraft.Main;

import java.util.ArrayList;

// 运动过程描述
class Route{
    Robot robot;
    public Point target;
//    double ox,oy;
    public Point vector;    //两点的向量
//    double rx,ry;   // 距离矢量
    double clockwise = 0;    // 1为正向，-1为反向 ，0 不动

    double printLineSpeed;
    double printTurnSpeed;


    public double realDistance;
    private double realAngleDistance;
    public double setMinAngle;   // 设置临界减速角度
    public double setMinDistance;   // 设置临界减速距离
    double theoryTurn;
    double angleOffset;
    double stopMinDistance;
    double stopMinAngleDistance;

    double emergencyDistanceCoef = 0.7;   // 半径乘子，每个机器人紧急距离，外人不得靠近
    double verticalSafeDistanceCoef = 1.5;   // 半径乘子，每个机器人紧急距离，外人不得靠近
    boolean isEmergency;// 是否紧急
    Point emergencyPos;    // 紧急机器人位置;



    double perceptionDistanceCoef = 2;  // 刹车距离 * 2 + emergencyDistance;这个距离以内要做出反应
    double perceptionAngleRange = Robot.pi/4;   // 前方一半视野角度
    double emergencyAngle = Robot.pi/2;   // 前方一半视野角度
    ArrayList<Integer> unsafeRobotIds;

    public Route(double ox,double oy,Robot robot) {
        target = new Point(ox,oy);
        vector = new Point();
        this.robot = robot;
        unsafeRobotIds = new ArrayList<>();
    }

    // 给定设置角度和速度
    public void rush() {
        calcParamEveryFrame();    // 参数计算
        calcSafePrintSpeed();
        Main.printForward(robot.id,printLineSpeed);
        Main.printRotate(robot.id,printTurnSpeed);
    }

    // 给定设置角度和速度时设置减速偏转策略
    public void rush2() {
        calcParamEveryFrame();    // 参数计算

        if (isMoveSafe()){
            Main.printLog("safe");
            calcSafePrintSpeed();
        }else {
            Main.printLog("unsafe");
            calcUnsafePrintSpeed();
        }
        Main.printForward(robot.id,printLineSpeed);
        Main.printRotate(robot.id,printTurnSpeed);

    }

    private void calcUnsafePrintSpeed() {
        // 紧急情况，和其他机器人靠得很近，逃离
        Main.printLog(isEmergency);
        if (isEmergency){
            processEmergEvent();
        }else {
            processNormalEvent();
        }
    }

    private void processNormalEvent() {
        // 总体思想，前方物体在越靠近中心，速度越小，转向越大
        // 若有前方多个物体，速度按最靠中心的，转向选择最近的计算

        double speedCoef = 1;
        double rotateCoef = 1;
        double minDis = 10000;
        Point speed = new Point(robot.lineVx,robot.lineVy);
        assert unsafeRobotIds.size() != 0;
        for(Integer i:unsafeRobotIds){
            Robot rot = Main.robots[i];
            Point posVec = robot.pos.calcVector(rot.pos);
            double cos = calcDeltaCos(speed, posVec);
            if ( 1-cos < speedCoef){
                speedCoef = cos;    //速度选择小的 安全
            }
            double dis = robot.pos.calcDistance(rot.pos);
            if (dis < minDis){
                rotateCoef = cos;
                clockwise = calcAvoidBumpClockwise(speed,posVec);
            }
        }
        printLineSpeed = Robot.maxSpeed * speedCoef;
        printTurnSpeed = Robot.maxRotate * clockwise * rotateCoef;
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

    private void processEmergEvent() {
        // 先算线速度，夹角小于pi/2 刹车，大于pi/2 全速
        Point speed = new Point(robot.lineVx,robot.lineVy);
        Point posVec = robot.pos.calcVector(emergencyPos);
        double angle = calcDeltaAngle(speed, posVec);
        if (angle<Robot.pi/2){
            printLineSpeed = 0;
        }else {
            printLineSpeed = 6;
        }
        clockwise = calcAvoidBumpClockwise(speed,posVec);
        printTurnSpeed = Robot.maxRotate * clockwise;
    }

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

    private double calcVerticalDistance(Point pos) {
        // 先算线速度，夹角小于pi/2 刹车，大于pi/2 全速
        Point speed = new Point(robot.lineVx,robot.lineVy);
        Point posVec = robot.pos.calcVector(pos);
        double dis = Main.norm(posVec);
        double angle = calcDeltaAngle(speed,posVec);
        double verDis = dis * Math.sin(angle); // 垂直距离 = 斜边 * sin (t)
        return verDis;
    }

    private void calcSafePrintSpeed() {
        //计算线速度
        if (realAngleDistance < Robot.canForwardRad && stopMinDistance < realDistance){
            // 速度太小，加速
//            printLineSpeed = Robot.maxSpeed;
            if (realAngleDistance < Robot.maxForwardRad){
                printLineSpeed = Robot.maxSpeed;
            }else {
                printLineSpeed = Robot.rotateSpeedEquation.getY(realAngleDistance);
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

    public void calcClockwise() {
        if (theoryTurn>robot.turn && theoryTurn - robot.turn < Robot.pi || theoryTurn<robot.turn && robot.turn - theoryTurn > Robot.pi){
            clockwise = 1;
        }else {
            clockwise = -1;
        }
    }

    public void calcSetMinAngle() {
        double tmpAngle = Math.abs(robot.turn - theoryTurn);
        double deltaAngle = Math.min(tmpAngle,2* Robot.pi-tmpAngle);
        double minAngle = robot.getMinAngle();
        if (deltaAngle <= minAngle){
            setMinAngle = deltaAngle/2;
//            turnSpeedCoef = 0.4;
        }else {
            setMinAngle = robot.getMinAngle()/2;
//            turnSpeedCoef = 0.8;
        }
    }

//    public void calcSetMinDistance() {
//
//        angleOffset = Math.abs(Math.atan2(0.4, realDistance));
//        if (realDistance <= robot.getMinDistance()){
//            setMinDistance = realDistance/2;
//        }else {
//            setMinDistance = robot.getMinDistance()/2;
//        }
//    }


    public void calcParamEveryFrame() {

        calcVector();   // 距离矢量
        calcTheoryTurn();//理论偏角
        calcClockwise();    // 转动方向
        calcMinDistance();
    }


    // 计算当前速度减速到0需要多长的距离
    private void calcMinDistance() {
        stopMinDistance = robot.getMinDistanceByCurSpeed();
        stopMinAngleDistance = robot.getMinAngleDistanceByCurSpeed();
    }

    // 关键参数，每一帧需要重新计算
    private void calcVector() {
        vector.x = target.x - robot.pos.x;
        vector.y = target.y - robot.pos.y;
        realDistance = Main.norm(vector);

    }

    // 计算向量的模长
    public double calcDeltaAngle(Point other) {
        return calcDeltaAngle(vector,other);
    }

    // 计算向量的模长
    public static double calcDeltaAngle(Point vector1,Point vector2) {
        double cosTheta = calcDeltaCos(vector1,vector2);
        double theta = Math.acos(cosTheta); // 将余弦值转化为弧度值
        return theta;
    }

    // 计算向量的模长
    public static double calcDeltaCos(Point vector1,Point vector2) {
        double dotProduct = Main.dotProduct(vector1,vector2); // 计算点积
        double normA = Main.norm(vector1); // 计算向量a的模长
        double normB = Main.norm(vector2); // 计算向量b的模长

        double cosTheta = dotProduct / (normA * normB); // 计算余弦值
        return cosTheta;
    }


    // 判断是否到达了目的地
    public boolean isArriveTarget() {
        double dis = target.calcDistance(robot.pos);
        return dis < robot.getRadius() * robot.arriveMinDistance;
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

}