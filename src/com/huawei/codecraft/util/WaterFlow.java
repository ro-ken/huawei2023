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
    public Station target2;     // 流水线备用目标
    boolean isType7;    // 是否是7号工作站
    int sellMinFps;     // 卖掉 target货物的fps
    ArrayList<Robot> robots ;
    Map<Integer,ArrayList<Station>> curTasks ;  //  7号工作站的456号任务
    Map<Integer,Integer> completed; // 完成的任务数量
    Map<Integer,Integer> halfComp; // 456 现有原料的数量，合成后completed +1,这个减2

    // 构造函数
    public WaterFlow(Station target) {
        this.target = target;
        isType7 = target.type == 7;
        curTasks = new HashMap<>();
        completed = new HashMap<>();
        halfComp = new HashMap<>();
        robots = new ArrayList<>();
        sellMinFps = target.pathToFps(false,target.closest89.pos);
        init();
    }
    private void init() {
        for (int i = 4; i <=6 ; i++) {
            curTasks.put(i,new ArrayList<>());
            completed.put(i,0);
            halfComp.put(i,0);
        }
    }
    @Override
    public String toString() {
        return "WaterFlow{" +
                "target=" + target +
                ", robots=" + robots.size() +
                ", mon/fps=" + target.cycleAvgValue +
                '}';
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
            commonSched(rob);
        }
    }

       // 机器人流水线全局调度器
       public void scheduler(Robot robot) {
           if (!isType7){
               simpleSched(robot);
           }else if (robot.lastStation != null) {
               if (robot.lastStation.type <= 6){
                   sta456Sched(robot);
               }else{
                   sta789Sched(robot);
               }
           }
       }

    //通用调度算法，也是兜底算法，把所有情况都考虑进去，不能让机器人闲着
    private void commonSched(Robot robot) {

        if (!isType7){
            simpleSched(robot);
            return;
        }

        // 1、是否有456要运送的
        Station src = closestAndHaveProSta(robot);
        if (src != null && src.bookNum==0){
            // todo 是否有近的123 可带一个过去
            robot.setSrcDest(src,target);
            return;
        }

        // 2、是否有需要做的任务
        Station dest = selectSlowestStation();
        if (dest != null){
            src = selectClosestSrcToDest(robot,dest);
            robot.setSrcDest(src,dest);
            return;
        }

        // 3、是否有7号产品出售
        if (target.canSell()){
            robot.setSrcDest(target,target.closest89);
            return;
        }

        // 4、买卖任何可买卖商品
        dest = robot.selectTimeShortestStation();
        if (dest != null){
            robot.setSrcDest(dest,dest.availNextStation);
        }
    }

    // 机器人在789号工作台时的调度
    private void sta789Sched(Robot robot) {
        if (target.proStatus == 1 && !target.bookPro){
            // 七八九的情况，判断7是否有物品
            robot.setSrcDest(target,target.canSellStations.peek().getKey());
        }else {
            commonSched(robot);
        }
    }

    // 机器人在456工作台时的调度
    private void sta456Sched(Robot robot) {
        Station now = robot.lastStation;
        if (now.canSell()){
            boolean flag1 = target.canBuy(now.type); // 有空位
            double tt = now.pathToFps(false,target.pos);    // 运送到 target的时间
            // 下面变量含义：没有产品，且在生产，原料满了，没有预定，送过去以后就生产完了，刚好取走货物
            boolean flag2 = !target.bookRow[now.type] && target.positionFull() && target.proStatus == 0 && target.leftTime < tt;
            if (flag1 || flag2){
                // 有空位 送
                robot.setSrcDest(now,target);
                return;
            }
        }
        // 若能送过去就送，不能送就调用本地最佳选择策略
        sta456BestSelect(robot);
    }

    // 在456策略选择，是继续合成本产品还是去合成其他的456
    private void sta456BestSelect(Robot robot) {
        // 本产品最少 || 就缺这个产品 -> 合成该产品
        boolean isLeast = typeIsLeast(robot.lastStation.type);
        boolean targetNeed = target.canBuyNum() == 1 && target.canBuy(robot.lastStation.type);
        Station[] srcDest = selectOtherStation(robot.lastStation);
        if (isLeast || targetNeed || srcDest == null){
            setTask(robot,robot.lastStation);
        }else {
            robot.setSrcDest(srcDest[0],srcDest[1]);
        }
    }

    private Station[] selectOtherStation(Station cur) {
        // 如果发现去合成别的station更近的话，就选择更换station
        // 如果时间选择时间太长，就别选了 todo ，留着，最后来写
        Station[] srcDest = null;
        int type = cur.type;
        for (int i = 4; i <=6 ; i++) {
            if (i != type){
                Station st = Objects.requireNonNull(target.canBuyStationsMap.get(i).peek()).key;    // 目前只判断一个
                for (int tp : st.getRaws()) {
                    // 遍历所有的123，判断距离是否更近
                    for (Pair pair : st.canBuyStationsMap.get(tp)) {
                        Station src = pair.key;
                    }

                }

            }

        }

        return srcDest;
    }

    // 合成算法 购买原料 合成task station
    private void setTask(Robot robot, Station task) {
        if (task.haveEmptyPosition()){
            // 能合成就合成，不能合成调通用
            Station src = selectClosestSrcToDest(robot,task);
            robot.setSrcDest(src,task);
        }else {
            commonSched(robot);
        }
    }

    private void setSimpleTask(Robot robot, Station task) {
        // 查看有几个空位，有两个，选最近的，有一个直接选，
        Station src = null;
        ArrayList<Integer> raws = task.canBuyRaws();
        if (raws.size() == 2){
           src = selectClosestSrcToDest(robot,task);
        }else if (raws.size() == 1){
            src = Objects.requireNonNull(task.canBuyStationsMap.get(raws.get(0)).peek()).key;   // 只有一个取最近的一个
        }else {
            // 原料格满了，如果还能装，没有产品，也没人预定，等生产完成就可以继续装
            if (task.positionFull() && task.positionNoBook() && task.proStatus==0 && task.leftTime >0){
                src = selectClosestSrcToDest(robot,task);
            }
        }
        if (src != null){
            robot.setSrcDest(src,task);
        }
    }

    public Station selectClosestSrcToDest(Robot robot,Station dest) {
        // 选择距离dest和自己最近的src
        // 距离 =  robot -> src -> dest
        double minTime = 100000;
        Station st = null;
        Main.printLog(dest);
        for (int ty : dest.getRaws()) {
            if (isType7){
                // 时间小于等于0 ，表明未生产，将会一直阻塞
                if (dest.bookRow[ty] || dest.positionIsFull(ty)) continue;
            }
            for (Station s:Main.map.get(ty)){
                // 第一段空载，第二段满载
                double t1 = s.pathToFps(true,robot.pos); // 若寻路算法高效，需换成robot的pos todo
                double t2 = s.pathToFps(false,dest.pos);
                double t = t1 + t2;
                if (t < minTime){
                    minTime = t;
                    st = s;
                }
            }
        }
        Main.printLog(st);
        return st;
    }

    private boolean typeIsLeast(int type) {
        // 判断此类型的产品完成数是否是最少的
        int num = completed.get(type);
        for (int i = 4; i <= 6; i++) {
            if (i != type){
                if (completed.get(i)<=num){
                    return false;
                }
            }
        }
        // 所有产品比它大才为true;
        return true;
    }

    // 机器人的target为456时的调度
    private void simpleSched(Robot robot) {
        if (target.type <=3){
            robot.setSrcDest(target,target.closest89);
        }else {
            // 456 -> 9
            if (target.proStatus == 1){
                robot.setSrcDest(target,target.closest89);
            }else {
                setSimpleTask(robot,target);
            }
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
            if (st.type<=3 || st.type >=7) continue;
            if (st.positionNoBook() && st.rowStatus == 0){
                // 位置没有预定，而且全空才考虑
                double fps = st.pathToFps(true,rob.pos);    // todo，计算量有些大了
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

    // 公平选择某个任务
    private int fairSelectTask(ArrayList<Integer> tasks) {
        // 1、首先按照工作站类型的数量来选择，某种类型的工作站只有一个的优先
        int min = 3;
        ArrayList<Integer> list = new ArrayList<>();
        for (int tp : tasks) {
            int size = target.canBuyStationsMap.get(tp).size()>1?2:1;   // 超过2个算2个
            if (size < min){
                list.clear();
                list.add(tp);
                min = size;
            }else if (size == min){
                list.add(tp);
            }
        }
        // 有唯一最小值，返回
        if (list.size() == 1){
            return list.get(0);
        }

        // 若只有一个，有多个最小值，按照距离远的搬运，防止出现等待的情况
        // 若有2个以上，价值高的先搬运
        if (min == 1){
            double max = 0;
            int maxId = 0;
            for (int tp : list) {
                double fps = Objects.requireNonNull(target.canBuyStationsMap.get(tp).peek()).value;
                if (fps > max){
                    max = fps;
                    maxId = tp;
                }
            }
            return maxId;
        }else {
            if (list.size() == 3){
                return 6;
            }
            return Math.max(list.get(0),list.get(1)); // 2个任务 选最大  4 < 5 < 6
        }
    }

    // 选择一个完成度最低的任务
    private ArrayList<Integer> selectSlowestTask() {
        int[] task = new int[3];
        ArrayList<Integer> res = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            task[i] = completed.get(i+4)*2 + halfComp.get(i+4);
        }
        int min = Math.min(Math.min(task[0],task[1]),task[2]);
        for (int i = 0; i < 3; i++) {
            if (task[i] == min){
                res.add(i+4);
            }
        }
        return res;
    }

    // 从target选一个能用的new task
    private Station newTask(int taskId) {
        PriorityQueue<Pair> pairs = target.canBuyStationsMap.get(taskId);
        for (Pair p :pairs){
            // 选择当前没有被占用,并且可以生产的station
            Station st = p.key;
            if(st.haveEmptyPosition()){
                return st;
            }
        }
        return null;
    }

    // 进度最慢的工作站
    private Station selectSlowestStation() {
        ArrayList<Integer> tasks = selectSlowestTask();
        int taskId = -1;
        HashSet<Integer> used = new HashSet<>();
        if (tasks.size() == 1){
            // 有一个进度最低的，直接选择该任务
            taskId = tasks.get(0);
        }else {
            taskId = fairSelectTask(tasks);
        }
        used.add(taskId);
        Station task = newTask(taskId);
        if (task == null && tasks.size() == 2){
            // 未分配成功，分配另一个
            if (tasks.get(0) == taskId){
                taskId = tasks.get(1);
            }else {
                taskId = tasks.get(0);
            }
            used.add(taskId);
            task = newTask(taskId);
        }
        if (task == null){
            for (int i = 4; i <=6 ; i++) {
                if (!used.contains(i)){
                    task = newTask(i);
                    if (task != null) {
                        break;  // 找到一个可用的
                    }
                }
            }
        }
        return task;
    }

    private Station closestAndHaveProSta(Robot robot) {
        // 最先判断7是否有空位，而且有产品，把产品运送到7，不能停下来
        ArrayList<Integer> empty = target.getEmptyRaw();
        Station src = null;
        if (empty.size() >0 ){
            // 查看目前是否有产品，若有就运送过来（选择距离最近的） robot -> 456
            double minDis = 1000000;
            for(int tp:empty){
                for (Pair pair : target.canBuyStationsMap.get(tp)) {
                    Station st = pair.key;
                    if (st.canSell()) {
                        double dis = st.pathToFps(true,robot.pos);  // todo 需要实时计算路径长度
                        if (dis < minDis){
                            minDis = dis;
                            src = st;
                            break;
                        }
                    }
                }
            }
        }
        return src;
    }
}

