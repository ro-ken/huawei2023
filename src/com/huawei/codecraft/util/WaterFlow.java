package com.huawei.codecraft.util;

import com.huawei.codecraft.Main;

import java.util.*;

/**
 * @Author: ro_kin
 * @Data:2023/3/21 11:57
 * @Description: TODO
 */

// 生产流水线
public class WaterFlow {
    public Station target;     // 流水线终极目标
    public Station target2;
    boolean isType7;    // 是否是7号工作站
    int sellMinFps;     // 卖掉 target货物的fps
    ArrayList<Robot> robots ;
    Map<Integer,ArrayList<Station>> curTasks ;  //  7号工作站的456号任务
    Map<Integer,Integer> completed; // 完成的任务数量

    @Override
    public String toString() {
        return "WaterFlow{" +
                "target=" + target +
                ", robots=" + robots.size() +
                ", mon/fps=" + target.cycleAvgValue +
                '}';
    }

    public WaterFlow(Station target) {
        this.target = target;
        isType7 = target.type == 7;
        curTasks = new HashMap<>();
        completed = new HashMap<>();
        robots = new ArrayList<>();
        sellMinFps = target.distanceToFps(false,target.closest89.pos);
        init();
    }

    private void init() {
        for (int i = 4; i <=6 ; i++) {
            curTasks.put(i,new ArrayList<>());
            completed.put(i,0);
        }
    }


    public void assignRobot(int robotNum) {

        if (robotNum == 1){
            for (int i = 0; i < robotNum; i++) {
                Robot rob = selectClosestRobot();
                if (rob != null){
                    rob.waterFlow = this;
                    robots.add(rob);
                }
            }
        }else {
            for (int i = 0; i < 4; i++) {
                Robot rob = Main.robots[i];
                if (rob.waterFlow == null){
                    rob.waterFlow = this;
                    robots.add(rob);
                    if (robots.size() == robotNum) {
                        break;
                    }
                }
            }
        }

        // 预先分配一个任务
        for (Robot rob : robots){
            assign456Task(rob);
        }
    }

    private Robot selectClosestRobot() {
        // 选择距离target最近的机器人
        double minDis = 100000;
        Robot minRot = null;
        // 分配最近的机器人
        for (int i = 0; i < 4; i++) {
            Robot rob = Main.robots[i];
            if (rob.waterFlow == null){
                double dis = target.pos.calcDistance(rob.pos);
                if (dis<minDis){
                    minDis = dis;
                    minRot = rob;
                }
            }
        }
        return minRot;
    }

    public Station selectDeadStation(Robot rob){
        //123-456-9 的情况
        // 可以用贪心，把局部的钱最大化，在生产流水线
        // 选择单位价值大于target，且离机器人最近的任务
        double minFps = 100000;
        Station next = null;
        for (int i = 0; i < Main.stationNum; i++) {
            Station st = Main.stations[i];
            if (st.type<=3 || st.type >=7 || st.taskBook) continue;
            if (st.positionNoBook() && st.rowStatus == 0){
                // 位置没有预定，而且全空才考虑
                double fps = st.distanceToFps(true,rob.pos);
                double valueFps = st.fastestComposeMoney * 3 / (fps+ st.fastestComposeFps * 3);

                if (valueFps > target.cycleAvgValue){
                    if (fps < minFps){
                        minFps = fps;
                        next = st;
                    }
                }
            }
        }
        return next;
    }

    // 分配一个456号任务，必须把curTask字段赋值
    private void assign456Task(Robot rob) {

        if (!isType7){
            Station next = selectDeadStation(rob);

            if (next != null){
                rob.setTask(next);
            }else {
                rob.setTask(target);
            }
//
//            rob.setTask(target);
            return;
        }

        // 分配任务，分配后申请资源，离开释放
        // 选择目前进度最低的456，进度 = 完成数 + 任务被领取数
        // 进度相同，可按「距离」贪心选择 或 安装「价值」贪心选择

        ArrayList<Integer> tasks = selectSlowestTask();
        Main.printLog(tasks);
        Station task = null;

        if (Main.specialMapMode && Main.mapSeq == 4){
            task = newTask(4);
            Main.printLog("task:" + task);
            if (task != null){
                rob.setTask(task);
                return;
            }
        }

        Main.printLog("task:" + task);
        if (tasks.size() == 1){
            // 有一个进度最低的，直接选择该任务
            task = newTask(tasks.get(0));
            Main.printLog("1:"+tasks.get(0));
        }else {
            int taskId = selectTaskByValue(tasks);
            Main.printLog("best:"+taskId);
            task = newTask(taskId);
            if (task == null && tasks.size() == 2){
                // 未分配成功，分配另一个
                if (tasks.get(0) == taskId){
                    taskId = tasks.get(1);
                }else {
                    taskId = tasks.get(0);
                }
                task = newTask(taskId);
            }
        }
        if (task == null){
            //当前任务机器人过多，分配可用的

            task = newAvailableTask();
        }
        if (task == null){
            // 456被占满，分配无效,等待
            Main.printLog("no task");
            return;
        }

        rob.setTask(task);
//                curTask = task;
    }

    private Station newAvailableTask() {
        Station task = null;
        if (Main.specialMapMode){
            if (Main.mapSeq == 2 || Main.mapSeq == 4){
                // 设置优先级 4最高
                task = newAvailableTaskBySeq(new int[]{4,6,5});
            }else {
                task = newAvailableTaskBySeq(new int[]{6,5,4});
            }
        }else {
            task = newAvailableTaskBySeq(new int[]{6,5,4});
        }
        return task;
    }

    private Station newAvailableTaskBySeq(int[] seq) {
        Station task = null;
        for (int i = 0; i < 3; i++) {
            if (task == null){
                task = newTask(seq[i]);
            }
        }
        return task;
    }

    private int selectTaskByValue(ArrayList<Integer> tasks) {
        if (tasks.size() == 0) {
            if (Main.specialMapMode){
                if (Main.mapSeq == 2 || Main.mapSeq == 4){
                    return 4;
                }
            }
            return 6;    // 6 最高
        }
        return Math.max(tasks.get(0),tasks.get(1)); // 2个任务 选最大  4 < 5 < 6
    }

    private Station newTask(int taskId) {
        PriorityQueue<Pair> pairs = target.canBuyStationsMap.get(taskId);
        for (Pair p :pairs){
            // 选择当前没有被占用,并且可以生产的station
            Station st = p.key;
//            if (st.taskBook) continue;
            Main.printLog("111");
            boolean flag2 = (st.leftTime ==0 ||st.proStatus==1 && st.leftTime>0 ) && !st.haveEmptyPosition();
            if (Main.specialMapMode && Main.mapSeq == 4 && st.type == 4 && st.bookNum2 <2 && !flag2) return st;
            Main.printLog("222");
            if (st.taskBook) continue;
            Main.printLog("333");
            if (st.leftTime >= 0 && !st.haveEmptyPosition()) continue;  // 阻塞，并且位置也满了
            Main.printLog("444");
            return st;
        }

        return null;
    }

    // 选择一个完成度最低的任务
    private ArrayList<Integer> selectSlowestTask() {
        int task4 = completed.get(4) + curTasks.get(4).size();
        int task5 = completed.get(5) + curTasks.get(5).size();
        int task6 = completed.get(6) + curTasks.get(6).size();
//        Main.printLog("4:"+task4+"  5:"+task5+"  6:"+task6);

        ArrayList<Integer> res = new ArrayList<>();

        if (task4 == task5 && task4 == task6){
            return res;   // 为空表示有三个
        }
        int min = Math.min(task4,task5);
        min = Math.min(min,task6);
        if (task4 == min){
            res.add(4);
        }
        if (task5 == min){
            res.add(5);
        }
        if (task6 == min){
            res.add(6);
        }
        return res;
    }

    // 机器人执行完毕，重新分配任务
    public void assignTask(Robot robot) {

        if (!isType7){
            Station next = selectDeadStation(robot);
            Main.printLog("next"+ next);
            if (next!=null){
                robot.setTask(next);
            }else {
//                if (target.proStatus == 1 && target.closest89.bookNum<=2){
                if (target.proStatus == 1){
                    // 机器人太多不好运送
                    robot.setSrcDest(target,target.closest89);
                }else {
                    robot.setTask(target);
                }
//                robot.setTask(target);
            }

            return;
        }



        // 有两种类型的任务，一种是合成任务，给一个456工作台，让机器人自己去合成
        // 另外一个是运输任务，直接给源目的地，让机器人去搬运
        Station now = robot.lastStation;
        Main.printLog("now:"+now);
        if (now == null) return;
        if (now.type <= 6){
            if (now.proStatus == 1){
                boolean flag1 = target.canBuy(now.type);//!target.positionIsFull(now.type) && !target.bookRow[now.type];  // 有空位
                double tt = now.distanceToFps(false,target.pos);    // 运送到 target的时间
                // 下面变量含义：没有产品，且在生产，原料满了，没有预定，送过去以后就生产完了，刚好取走货物
                boolean flag2 = !target.bookRow[now.type] && target.positionFull() && target.proStatus == 0 && target.leftTime < tt;
                if (flag1 || flag2){
                    // 有空位 送
                    robot.setSrcDest(now,target);

                }else {

                    //无空位,重新选择一个
                    urgentTask(robot);
                    //看目前有没有成品，送一个过去
//                  assign456Task(robot);
                }

            }else {
                // 没有产品，继续当前生成
                if (now.haveEmptyPosition())
                {
                    robot.setTask(now);
                }else urgentTask(robot);
            }

//        }else if (target.proStatus == 1 && !target.bookPro){
        }else{
            if (target.proStatus == 1 && !target.bookPro){
                    // 七八九的情况，判断7是否有物品
                    //卖出 7号
                    robot.setSrcDest(target,target.canSellStations.peek().getKey());
            }else {
                if (target2!=null && target2.proStatus == 1 && !target2.bookPro){
                    robot.setSrcDest(target2,target2.canSellStations.peek().getKey());
                }else {
                    urgentTask(robot);
                }
            }

            if (now.type == 7 && target2 != null){
                // 如果满了，切换target
                if (!target.haveEmptyPosition()){
                    Station st = target;
                    target = target2;
                    target2 = st;
                }
            }
        }
    }

    //处理当前最需要处理的任务
    private void urgentTask(Robot robot) {

        Main.printLog("555");
        if (Main.specialMapMode && Main.mapSeq == 4){
            Station task = newTask(4);
            Main.printLog("ttkk"+task);
            if (task != null){
                robot.setTask(task);
                return;
            }
        }

        // 最先判断7是否有空位，而且有产品，把产品运送到7，不能停下来
        ArrayList<Integer> empty = target.getEmptyRaw();
        Station src = null;
        if (empty.size() >0 ){
            // 查看目前是否有产品，若有就运送过来（选择距离最近的） robot -> 456
            double minDis = 1000000;
            for(int tp:empty){
                for (Pair pair : target.canBuyStationsMap.get(tp)) {
                    Station st = pair.key;
                    if (st.taskBook ) continue; // 有机器人在生产
                    if (st.proStatus == 0 || st.bookPro) continue;  // 没有产品，或被预定
                    double dis = robot.pos.calcDistance(st.pos);
                    if (dis < minDis){
                        minDis = dis;
                        src = st;
                        break;
                    }
                }
            }
        }

        // 不为空，可以搬运
        if (src != null){
            robot.setSrcDest(src,target);
        }else {
            assign456Task(robot);       // 不送7了，
//            // 若没有物品，判断7是否有物品
//            if (target.proStatus == 1 && !target.bookPro){
//                // 若能运送，则运送
//                robot.setSrcDest(target,target.closest89);
//            }else {
//                // 若7也没有，则处理一个456号任务
//                assign456Task(robot);
//            }
        }
    }
}

