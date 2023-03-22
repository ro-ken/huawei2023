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
    Station target;     // 流水线终极目标
    boolean isType7;    // 是否是7号工作站
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
        init();
    }

    private void init() {
        for (int i = 4; i <=6 ; i++) {
            curTasks.put(i,new ArrayList<>());
            completed.put(i,0);
        }
    }


    public void assignRobot(int robotNum) {
        // 分配最近的机器人
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
        // 预先分配一个任务
        for (Robot rob : robots){
            assign456Task(rob);
        }
    }

    // 分配一个456号任务，必须把curTask字段赋值
    private void assign456Task(Robot rob) {

        if (!isType7){
            rob.setTask(target);
            return;
        }

        // 分配任务，分配后申请资源，离开释放
        // 选择目前进度最低的456，进度 = 完成数 + 任务被领取数
        // 进度相同，可按「距离」贪心选择 或 安装「价值」贪心选择
        ArrayList<Integer> tasks = selectSlowestTask();
        Main.printLog(tasks);
        Station task = null;

        if (tasks.size() == 1){
            // 有一个进度最低的，直接选择该任务
            task = newTask(tasks.get(0));
            Main.printLog("1:"+tasks.get(0));
        }else {
            int taskId = selectTaskByValue(tasks);
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
        // 还是从大到小
        Station task = newTask(6);
        if (task == null){
            task = newTask(5);
        }
        if (task == null){
            task = newTask(4);
        }
        return task;
    }

    private int selectTaskByValue(ArrayList<Integer> tasks) {
        if (tasks.size() == 0) return 6;    // 6 最高
        return Math.max(tasks.get(0),tasks.get(1)); // 2个任务 选最大  4 < 5 < 6
    }

    private Station newTask(int taskId) {
        PriorityQueue<Pair> pairs = target.canBuyStationsMap.get(taskId);
//        Main.printLog(taskId);
//        Main.printLog(pairs);
        for (Pair p :pairs){
            // 选择当前没有被占用,并且可以生产的station
            Station st = p.key;
            if (st.taskBook) continue;
            if (st.leftTime >= 0 && !st.haveEmptyPosition()) continue;  // 阻塞，并且位置也满了

            return st;
        }

        return null;
    }

    // 选择一个完成度最低的任务
    private ArrayList<Integer> selectSlowestTask() {
        int task4 = completed.get(4) + curTasks.get(4).size();
        int task5 = completed.get(5) + curTasks.get(5).size();
        int task6 = completed.get(6) + curTasks.get(6).size();
        Main.printLog("4:"+task4+"  5:"+task5+"  6:"+task6);

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
            if (target.proStatus == 1){
                robot.setSrcDest(target,target.closest89);
            }else {
                robot.setTask(target);
            }
            return;
        }

        // 有两种类型的任务，一种是合成任务，给一个456工作台，让机器人自己去合成
        // 另外一个是运输任务，直接给源目的地，让机器人去搬运
        Station now = robot.lastStation;
        Main.printLog("now:"+now);
        if (now.type <= 6){
            if (now.proStatus == 1){
                if (!target.positionIsFull(now.type) && !target.bookRow[now.type]){
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


        }else if (target.proStatus == 1 && !target.bookPro){
            // 七八九的情况，判断7是否有物品
               //卖出 7号
            robot.setSrcDest(target,target.canSellStations.peek().getKey());
        }else {
            urgentTask(robot);
        }

    }

    //处理当前最需要处理的任务
    private void urgentTask(Robot robot) {
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

