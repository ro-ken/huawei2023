package com.huawei.codecraft.util;

import com.huawei.codecraft.Main;

/**
 * @Author: ro_kin
 * @Data:2023/3/13 19:47
 * @Description: TODO
 */
public class Robot {

    public static final double pi  = 3.1415926;
    public static final double emptyRadius = 0.45;
    public static final double fullRadius = 0.53;
    public static final int density = 20;
    public static final int maxSpeed = 6;//m/s
    public static final int minSpeed = -2;//m/s
    public static final double maxRotate = pi/2;//rad/s
    public static final int maxForce = 250;//N
    public static final int maxRotateForce = 50;//N*m

    public static final double radOffset = 0.1 ; //(rad)最大误差 0.003
    public static final double angleSpeedOffset = 0.1 ; //(rad)最大误差 0.003
    public static double empthA;     //加速度
    public static double fullA;     //加速度

    public static double empthRotateA;     //角加速度
    public static double fullRotateA;     //角加速度
    public static double emptyMinAngle; // 加速减速临界值 ,空载
    public static double fullMinAngle; // 加速减速临界值 ，满载


    int id;
    public int StationId; // -1 无 ，从0 开始 表示第几个工作台
    public int carry;  // 携带物品 1-7
    public double timeValue;  // 时间价值系数 [0.8 - 1]
    public double bumpValue;       //碰撞价值系数 [0.8 - 1]
    public double angV; //角速度  弧度/s 正逆，负顺
    public double lineVx;    //线速度， m/s
    public double lineVy;    //线速度， m/s
    public double turn; //朝向 [-pi,pi] 0朝向右，pi/2  朝上
    public double x;
    public double y;

    public Station nextStation;    // null : no target to go
    public Station srcStation;
    public Station destStation;

    Route route;

    static {
        empthA = calcAcceleration(emptyRadius);
        fullA = calcAcceleration(fullRadius);

        empthRotateA = calcRotateAcce(emptyRadius);
        fullRotateA = calcRotateAcce(fullRadius);

        emptyMinAngle = calcMinAngle(true);
        fullMinAngle = calcMinAngle(false);
    }


    private static double calcMinAngle(boolean isEmpty) {
        double a = isEmpty ? empthRotateA:fullRotateA;
        return Math.pow(maxRotate,2)/(a);
    }

    private static double calcRotateAcce(double radius) {
        System.out.println(radius);
        double s = pi * radius * radius;
        double m = s * density;
        double I = m * radius * radius * 0.5;
        double rotateA = maxRotateForce/I;
        return rotateA;
    }

    private static double calcAcceleration(double radius) {
        double s = pi * radius * radius;
        double m = s * density;
        double a = maxForce / m;
        return a;
    }


    public Robot(int stationId, double x, double y, int robotId) {
        StationId = stationId;
        this.x = x;
        this.y = y;
        this.nextStation = null;
        this.srcStation = null;
        this.destStation = null;
        id=robotId;
    }

    public static double calcTimeValue(int fps) {
        return f(fps,9000,0.8);
    }

    public static double calcBumpValue(int impulse) {
        return f(impulse,1000,0.8);
    }

    public static double f(int x, double maxX, double minRate){
        if (x>=maxX){
            return minRate;
        }
        else {
            double t1 = 1-Math.pow(1-x/maxX,2);
            double t2 = 1-Math.pow(t1,0.5);
            return t2 * (1-minRate) + minRate;
        }
    }

    // 选一个最佳的工作站
    public void selectBestStation() {
        Station station = selectClosestStation();
//        Station maxStation = selectBestValueStation();
        if (station == null){
            System.out.println("no station can use...");
            return;
        }

        nextStation = srcStation = station;
        System.out.println("src"+srcStation);
        destStation = srcStation.availNextStation;
        System.out.println("dest" + destStation);
        srcStation.bookPro = true;      // 预定位置
        destStation.bookRow[srcStation.type] = true;
    }

    private Station selectClosestStation() {
        Station closestStation = null;
        double minDistance = 10000;
        for(int i=0;i<Main.stationNum;i++){

            Station station = Main.stations[i];
            if (station.leftTime == -1 || station.bookPro) continue;
            double dis = station.calcDistance(x,y);
            if (dis < minDistance){
                // 卖方有货，卖方有位置
                if (station.type>3 && station.leftTime ==-1) continue;  // 未生产产品
                Station oth = station.chooseAvailableNextStation();
                if (oth != null){
                    closestStation = station;
                    minDistance = dis;
                }
            }
//            System.out.println("maxvalue"+maxValue);
        }
        return closestStation;
    }

    private Station selectBestValueStation() {
        Station maxStation = null;
        int maxValue = -10000;
        for(int i=0;i<Main.stationNum;i++){

            Station station = Main.stations[i];
            if (station.leftTime == -1) continue;
            int value = station.calcValue(x, y, true);
//            System.out.println("value"+value);
            value = station.canSellStations.peek().getValue()-value;    // 赚的钱 - 花费的时间
//            System.out.println("getValue + num"+station.canSellStations.size());
//            System.out.println("getValue"+station.canSellStations.peek().getValue());
            if (value > maxValue){
                // 卖方有货，卖方有位置
                Station oth = station.canSellStations.peek().getKey();
//                System.out.println("station = "+station.Id);
//                if (station.proStatus == 1 && !oth.bookRow[station.type] && !oth.positionIsFull(station.type)){
                if (!oth.bookRow[station.type] && !oth.positionIsFull(station.type)){
                    maxStation = station;
                    maxValue = value;
                }
            }
//            System.out.println("maxvalue"+maxValue);
        }
        return maxStation;
    }

    // 计算路线
    public void calcRoute() {
        if (nextStation != null){
            route = new Route(nextStation.x,nextStation.y,this);
            route.calcParam();
        }
    }

    public double getMinDistance() {
        return carry > 0? Station.fullMinDistance : Station.emptyMinDistance;
    }

    public double getMinAngle() {
        return carry > 0? fullMinAngle:emptyMinAngle;
    }

    public void rush() {
        if (route.status == 1){
            adjustAngle();
        }else if (route.status == 2){
            goToTarget();
        }
    }

    @Override
    public String toString() {
        String s = route == null ? " ": ", tarturn=" + route.theoryTurn +", set=" + route.setMinAngle ;
        return "Robot{" +
                "id=" + id +
                ", carry=" + carry +
                ", Vr=" + angV +
                ", Vx=" + lineVx +
                ", Vy=" + lineVy +
                ", turn=" + turn +
                s +
                '}';
    }

    private void goToTarget() {

        if (angV!=0){
            route.adjustTurn();
        }
        if (route.status == 1) return;  // 先减速调整角度
        double rx = nextStation.x - x;
        double ry = nextStation.y - y;
        double deltaDistance = Math.pow(rx*rx+ry*ry,0.5);
        if (deltaDistance > route.setMinDistance){
            Main.printForward(id,6);

            System.out.println(this);
        }else {
            Main.printForward(id,0);
        }
    }

    private void adjustAngle() {
        if (lineVy!=0 || lineVy !=0){
            Main.printForward(id,0);    // 先减速
        }
        double tmpAngle = Math.abs(turn - route.theoryTurn);
        double deltaAngle = Math.min(tmpAngle,2*pi-tmpAngle);

        if (deltaAngle < radOffset){
            route.status = 2;    // run
            goToTarget();
            return;
        }
        // 不一定要减速到0 ,在一定范围就可以出发了
        if (angV < angleSpeedOffset){
            if (deltaAngle < radOffset){
                route.status = 2;    // run
            }else{
                if (route.isRotate){
                    calcRoute();    // 转得不对，重新调整
                    route.isRotate = false;
                }else {
                    double putRotate = pi * route.clockwise * route.turnSpeedCoef;
                    Main.printRotate(id,putRotate);
                    System.out.println("rotate:"+putRotate);
                    route.isRotate = true;
                }
            }
        }
        else {
            if (deltaAngle > route.setMinAngle){
                Main.printRotate(id,pi * route.clockwise * route.turnSpeedCoef);
                System.out.println("rotate:"+pi * route.clockwise * route.turnSpeedCoef);
            }else {
                Main.printRotate(id,0);
                System.out.println("rotate:0");
            }
        }
    }

    public boolean isArrive() {
        if (StationId == nextStation.Id){
            System.out.println("robot arrived id ="+StationId);
            return true;
        }else{
            return false;
        }
    }

    public void changeTarget() {
        if (nextStation == srcStation){
            nextStation = destStation;
            calcRoute();
        }else {
            nextStation = srcStation = destStation = null;
        }
        System.out.println("state change");
        System.out.println("next station" + nextStation);
    }
}


// 运动过程描述
class Route{
    double ox,oy;
    double clockwise = 0;    // 1为正向，-1为反向 ，0 不动
    int status = 1;    //路线所处节点， 1 旋转，2加速
    boolean isRotate = false;

    public double setMinAngle;   // 设置临界减速角度
    public double setMinDistance;   // 设置临界减速距离
    public double turnSpeedCoef = 1;    // 设置旋转系数，角度越小，转得越慢
    public double lineSpeedCoef = 1;    // 设置直线系数，距离越近，走的越慢
    double theoryTurn;
    double angleOffset;
    Robot robot;

    public Route(double ox,double oy,Robot robot) {
        this.ox = ox;
        this.oy = oy;
        this.robot = robot;
    }

    public void adjustTurn() {
        calcTheoryTurn();
        double delta = Math.abs(robot.turn - theoryTurn);
        if (delta > Robot.radOffset){
            status = 1; //角度太大，减速先调整角度
            return;
        }else{
            //微修
            if (delta < angleOffset){
                Main.printRotate(robot.id,0);
            }else {
                calcClockwise();
                Main.printRotate(robot.id,Robot.radOffset * clockwise);
            }
        }


    }

    public void calcTheoryTurn() {
        double rx = ox - robot.x;
        double ry = oy - robot.y;

        // 计算夹角弧度
        theoryTurn = Math.atan2(ry, rx);
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

    public void calcSetMinDistance() {
        double rx = ox - robot.x;
        double ry = oy - robot.y;
        double deltaDistance = Math.pow(rx*rx+ry*ry,0.5);
        angleOffset = Math.abs(Math.atan2(0.4, deltaDistance));
        if (deltaDistance <= robot.getMinDistance()){
            setMinDistance = deltaDistance/2;
        }else {
            setMinDistance = robot.getMinDistance()/2;
        }
    }
    private void calcOffset() {

    }
    public void calcParam() {

        calcTheoryTurn();//转向方向转向顺时针
        calcClockwise();
        calcSetMinAngle();
        calcSetMinDistance();
        calcOffset();
    }

}