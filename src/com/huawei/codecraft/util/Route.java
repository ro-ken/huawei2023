package com.huawei.codecraft.util;

import com.huawei.codecraft.Main;

// 运动过程描述
class Route{
    double ox,oy;
    double rx,ry;   // 距离矢量
    double clockwise = 0;    // 1为正向，-1为反向 ，0 不动

    @Override
    public String toString() {
        return "Route{" +
                "clockwise=" + clockwise +
                ", realA=" + realAngleDistance +
                ", theoryTurn=" + theoryTurn +
                ", stopA=" + stopMinAngleDistance +
                '}';
    }

    public double realDistance;
    private double realAngleDistance;
    public double setMinAngle;   // 设置临界减速角度
    public double setMinDistance;   // 设置临界减速距离
//    public double turnSpeedCoef = 1;    // 设置旋转系数，角度越小，转得越慢
//    public double lineSpeedCoef = 1;    // 设置直线系数，距离越近，走的越慢
    double theoryTurn;
    double angleOffset;
    double stopMinDistance;
    double stopMinAngleDistance;
    Robot robot;


    public Route(double ox,double oy,Robot robot) {
        this.ox = ox;
        this.oy = oy;
        this.robot = robot;
    }

    // 给定设置角度和速度
    public void rush() {
        calcParamEveryFrame();    // 参数计算
        //计算线速度
        if (realAngleDistance < Robot.canForwardRad && stopMinDistance < realDistance){
            // 速度太小，加速
                Main.printForward(robot.id,6);
        }else {
            // 减速
            Main.printForward(robot.id,0);
        }

        //计算角速度
        if (stopMinAngleDistance < realAngleDistance){
            double coff = Math.min(1,(realAngleDistance-stopMinAngleDistance)/realAngleDistance);
            Main.printRotate(robot.id, Robot.pi * clockwise);
//            Main.printLog("rotate" + Robot.pi * clockwise * coff);
        }else {
            Main.printRotate(robot.id,0);
        }

    }


    public void calcTheoryTurn() {
        // 计算夹角弧度
        theoryTurn = Math.atan2(ry, rx);
        double tmp = Math.abs(robot.turn - theoryTurn);
        realAngleDistance = Math.min(tmp,2* Robot.pi-tmp);
        Main.printLog("tmp"+tmp+"real"+realAngleDistance);
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
        Main.printLog("fdsafasdf");
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
        rx = ox - robot.x;
        ry = oy - robot.y;
        realDistance = Math.pow(rx*rx+ry*ry,0.5);

    }
}