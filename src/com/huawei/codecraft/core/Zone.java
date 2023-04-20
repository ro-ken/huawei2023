package com.huawei.codecraft.core;

import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.Pair;
import com.huawei.codecraft.util.StationStatus;

import java.util.*;

// 地图划分为不同的区域，每个区域不连通
public class Zone {
    public int id;  // 区域号
    public  Map<Integer, ArrayList<Station>> stationsMap = new HashMap<>(); // 类型，以及对应的工作站集合
    public  Map<Integer, ArrayList<Station>> fighterStationsMap = new HashMap<>(); // 类型，以及对方的对应工作站集合

    ArrayList<Station> targets = new ArrayList<>(); // 存储7号工作台，按照价值降序
    public ArrayList<Robot> robots = new ArrayList<>();
    public Zone(int id) {
        this.id = id;
    }
    public Zone() {
    }

    @Override
    public String toString() {
        return "Zone{" +
                "id=" + id +
                ", stationsMap=" + stationsMap +
                ", robots=" + robots +
                '}';
    }

    public int getStationNum(){
        int res = 0;
        for (ArrayList<Station> value : stationsMap.values()) {
            res += value.size();
        }
        return res;
    }

    public ArrayList<Station> getStations(){
        ArrayList<Station> stas = new ArrayList<>();
        for (ArrayList<Station> value : stationsMap.values()) {
            stas.addAll(value);
        }
        return stas;
    }

    // 机器人全局调度器
    public void scheduler(Robot robot) {
        Station now = robot.lastStation;
        if (now == null){
            commonSched(robot);
        }else{
            if (now.canSell()){
                Station target = selectBestSellStation(now);
                // 自己可卖 ， 有买家买
                if (target != null){
                    robot.setSrcDest(now,target);
                    return;
                }
            }
            if (now.type <= 6){
                // 工作台456,首先把这个工作台合完，在合成其他的
                setTask(robot,now);
            }else {
                commonSched(robot);
            }
        }
    }

    //通用调度算法，也是兜底算法，把所有情况都考虑进去，不能让机器人闲着
    private void commonSched(Robot robot) {

        Station target = selectAvailableTarget();

        if (target == null){
            // 没有可用的target，直接贪心
            // 贪心算法改进，分为是否有9，可组成小的流水线
            Station src = selectTimeShortestStation(robot);
            if (src != null){
                robot.setSrcDest(src,src.availNextStation);
            }
            return;
        }

        Station src = null;

        // 1、选择target最慢的任务
//        src = selectSlowestSrcStation(target);
//        if (src != null && target.canBuy(src.type)){
//            robot.setSrcDest(src,target);
//            return;
//        }

        // 1、选择target最慢的任务
        Station dest = selectSlowestStation(target);
        if (dest != null){
            src = selectClosestSrcToDest(robot,dest);
            Main.printLog("src:" + src + " dest: "+ dest);
            if (src != null){
                robot.setSrcDest(src,dest);
            }
            return;
        }

        // 2、买卖未来可买卖商品
        dest = selectSlowestStationLast(target);
        if (dest != null){
            src = selectClosestSrcToDestLast(robot,dest);
            if (src != null){
                robot.setSrcDest(src,dest);
            }
            return;
        }

        // 3、是否有7号产品出售
        if (target.canSell() && target.closest89 != null){
            robot.setSrcDest(target,target.closest89);
            return;
        }

        // 4、买卖任何可买卖商品，贪心，todo 暴力冲撞
        src = selectTimeShortestStation(robot);
        if (src != null){
            robot.setSrcDest(src,src.availNextStation);
            return;
        }
    }

    //
    private Station selectSlowestSrcStation(Station target) {
        // 如果有456合成好了，可优先考虑卖掉
        // 选择target 进度最慢的任务
        ArrayList<Integer> tasks = selectSlowestTask(target);
        Main.printLog("tasks" + tasks);
        int taskId = -1;
        HashSet<Integer> used = new HashSet<>();
        if (tasks.size() == 1){
            // 有一个进度最低的，直接选择该任务
            taskId = tasks.get(0);
        }else {
            taskId = fairSelectTask(tasks,target);
            Main.printLog("taskId" + taskId);
        }
        used.add(taskId);
        Station task = newTask(taskId,target);
        if (task == null && tasks.size() == 2){
            // 未分配成功，分配另一个
            if (tasks.get(0) == taskId){
                taskId = tasks.get(1);
            }else {
                taskId = tasks.get(0);
            }
            used.add(taskId);
            task = newTask(taskId,target);
        }
        if (task == null){
            for (int i = 4; i <=6 ; i++) {
                if (!used.contains(i)){
                    task = newTask(i,target);
                    if (task != null) {
                        break;  // 找到一个可用的
                    }
                }
            }
        }
        return task;
    }

    private Station selectClosestSrcToDestLast(Robot robot, Station dest) {
        // 选择距离dest和自己最近的src
        // 距离 =  robot -> src -> dest
        double minTime = 100000;
        Station st = null;
        int books = dest.bookRawNum();
        if (dest.haveEmptyPosition() || dest.getBookRawNum()>=2 || dest.bookRawNum()>=2){
            return null;    //这个要配合上一个函数使用，此类是不能有空位的
        }

        for (int ty : dest.getRaws()) {

            // 前面判断过了， books <2
            if (books == 0){
                if (!dest.canBuy(ty)) continue;
            }else {
                // 不能是满的，
                if (dest.positionIsFull(ty)) continue;
            }

            for (Station s:Main.stationsMap.get(ty)){

                if (s.place == StationStatus.BLOCK) continue;       // 阻塞不能送
                // 第一段空载，第二段满载
                double t1 = s.pathToFps(true,robot.pos); // 若寻路算法高效，需换成robot的pos
                double t2 = s.pathToFps(false,dest.pos);
                double t = t1 + t2;
                if (s.place == StationStatus.CANBUMP){      // 已改
                    t += Robot.srcBumpCost;    // 加上代价
                }

                if (t < minTime){
                    minTime = t;
                    st = s;
                }
            }
        }
        return st;
    }

    private Station selectSlowestStationLast(Station target) {
        ArrayList<Integer> tasks = selectSlowestTask(target);
        int taskId = -1;
        HashSet<Integer> used = new HashSet<>();
        if (tasks.size() == 1){
            // 有一个进度最低的，直接选择该任务
            taskId = tasks.get(0);
        }else {
            taskId = fairSelectTask(tasks,target);
        }
        used.add(taskId);
        Station task = newTaskLast(taskId,target);
        if (task == null && tasks.size() == 2){
            // 未分配成功，分配另一个
            if (tasks.get(0) == taskId){
                taskId = tasks.get(1);
            }else {
                taskId = tasks.get(0);
            }
            used.add(taskId);
            task = newTaskLast(taskId,target);
        }
        if (task == null){
            for (int i = 4; i <=6 ; i++) {
                if (!used.contains(i)){
                    task = newTaskLast(i,target);
                    if (task != null) {
                        break;  // 找到一个可用的
                    }
                }
            }
        }
        return task;
    }

    private Station newTaskLast(int taskId, Station target) {
        Station res = null;
        double minFps = 100000;
        PriorityQueue<Pair> pairs = target.canBuyStationsMap.get(taskId);
        for (Pair p :pairs){
            // 选择当前没有被占用,并且可以生产的station
            Station st = p.key;
            if (st.place == StationStatus.BLOCK) continue;
            // 后一项保证前面运算的货物能生产，原料格能够空出来,
            // 为了防止堵死情况，可以再加个判断，pairs.size() == 1
            if (st.getBookRawNum()<2 && (st.proStatus==0 || st.leftTime == -1)){

                double fps = p.value;
                if (st.place == StationStatus.CANBUMP){      // 已改
                    // 周围有机器人，加上权重
                    fps += Robot.dest456BumpCost;
                }
                if (fps < minFps){
                    minFps = fps;
                    res = st;
                }

            }
        }
        return res;
    }

    private Station selectSlowestStation(Station target) {
        // 选择target 进度最慢的任务
        ArrayList<Integer> tasks = selectSlowestTask(target);
        Main.printLog("tasks" + tasks);
        int taskId = -1;
        HashSet<Integer> used = new HashSet<>();
        if (tasks.size() == 1){
            // 有一个进度最低的，直接选择该任务
            taskId = tasks.get(0);
        }else {
            taskId = fairSelectTask(tasks,target);
            Main.printLog("taskId" + taskId);
        }
        used.add(taskId);
        Station task = newTask(taskId,target);
        if (task == null && tasks.size() == 2){
            // 未分配成功，分配另一个
            if (tasks.get(0) == taskId){
                taskId = tasks.get(1);
            }else {
                taskId = tasks.get(0);
            }
            used.add(taskId);
            task = newTask(taskId,target);
        }
        if (task == null){
            for (int i = 4; i <=6 ; i++) {
                if (!used.contains(i)){
                    task = newTask(i,target);
                    if (task != null) {
                        break;  // 找到一个可用的
                    }
                }
            }
        }
        return task;
    }

    // 从target选一个能用的new task
    private Station newTask(int taskId,Station target) {

        Station res = null;
        double minFps = 100000;

        PriorityQueue<Pair> pairs = target.canBuyStationsMap.get(taskId);
        for (Pair p :pairs){
            // 选择当前没有被占用,并且可以生产的station
            Station st = p.key;

            // 选择没有被阻塞的工作站
            if(!st.haveEmptyPosition() || st.place == StationStatus.BLOCK) continue;
            double fps = p.value;
            if (st.place == StationStatus.CANBUMP){      // 已改
                // 周围有机器人，加上权重
                fps += Robot.dest456BumpCost;
            }
            if (fps < minFps){
                minFps = fps;
                res = st;
            }
        }
        return res;
    }

    private int fairSelectTask(ArrayList<Integer> tasks,Station target) {
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

            if (list.size() == 3){
                return 6;
            }
            return Math.max(list.get(0),list.get(1)); // 2个任务 选最大  4 < 5 < 6
        }else {
            if (list.size() == 3){
                return 6;
            }
            return Math.max(list.get(0),list.get(1)); // 2个任务 选最大  4 < 5 < 6
        }
    }

    private ArrayList<Integer> selectSlowestTask(Station target) {
//        456个数计算
//        7的456原料位 + 机器人的nextStation
//        选择少的搬运

        int[] task = new int[3];
        ArrayList<Integer> res = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int type = i+4;
            if (!target.canBuy(type)){
                task[i] += 3;   // 做完的算3个
            }
            for (Robot robot : robots) {
                if (robot.destStation != null){
                    if (robot.destStation.type == type){
                        task[i] += 2;   // 在做的算2
                    }
                }
            }
        }
//        Main.printLog("task:" + task[0] +","+ task[1] +","+ task[2]);
        int min = Math.min(Math.min(task[0],task[1]),task[2]);
        for (int i = 0; i < 3; i++) {
            if (task[i] == min){
                res.add(i+4);
            }
        }
        return res;
    }

    //选择取货时间最短的，取货时间 = max {走路时间，生成时间}
    public Station selectTimeShortestStation(Robot robot) {
        Station shortestStation = null;
        double shortest = 10000;

        for (ArrayList<Station> list : stationsMap.values()) {
            for (Station station : list) {
//                    if (station.type == 7) continue;
                if (station.place == StationStatus.BLOCK) continue; // 阻塞了
                if (station.leftTime == -1 || (station.bookPro && station.type>3)) continue;
                double dis = station.pos.calcDistance(robot.pos);
                double time1 = robot.calcFpsToPlace(dis);
                double time = Math.max(time1,station.leftTime);
                if (station.place == StationStatus.CANBUMP){     // 加上权重
                    time += Robot.srcBumpCost;
                }
                Station oth = station.chooseAvailableNextStation();
                if (oth == null) continue;
                if (oth.place == StationStatus.CANBUMP){
                    time += oth.type== 7?Robot.destBumpCost:Robot.dest456BumpCost;
                }

                if (time < shortest){
                    // 卖方有货，卖方有位置
                    shortestStation = station;
                    shortest = time;
                }
            }
        }
        return shortestStation;
    }

    private Station selectAvailableTarget() {
        // 选择可用的 7 号工作站类型

        Station res = null;
        double maxValue = 0;

        for (Station target : targets) {
            if (target.place != StationStatus.BLOCK){   // 已改
                double value = target.cycleAvgValue;
                if (target.place == StationStatus.EMPTY){   // 已改
                    value += 3;     // 若周围没有机器人 权重 +5
                }
                if (value > maxValue){
                    res = target;
                    maxValue = value;
                }
            }
        }
        return res;
    }

    private Station selectBestSellStation(Station now) {
        // 456 -> 79 , 7 -> 89
        // 选择一个最佳的售卖工作站
        Station res = null;
        double minFps = 100000;

        double maxValue = 0;
        if (now.type <=6 && now.type >=4){
            // 456 优先送7
            for (Station target : targets) {
                if (!target.canBuy(now.type) || target.place == StationStatus.BLOCK){
                    // 不能买 或被占用
                    continue;
                }
                double value = target.cycleAvgValue;
                if (target.place == StationStatus.EMPTY){   // 已改
                    value += 3;     // 若周围没有机器人 权重 +5
                }
                if (value > maxValue){
                    res = target;
                    maxValue = value;
                }
            }
        }
        if (res != null){
            return res;
        }

        for (Pair pair : now.canSellStations) {
            Station target = pair.key;
            if (target.place != StationStatus.BLOCK){     // 已改
                // 选空的点
                if (target.canBuy(now.type)){
                    double fps = pair.value;
                    if (target.place == StationStatus.CANBUMP){      // 已改
                        // 周围有机器人，加上权重
                        fps += Robot.destBumpCost;
                    }
                    if (fps < minFps){
                        minFps = fps;
                        res = target;
                    }
                }
            }
        }

        return res;
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

    private Station selectClosestSrcToDest(Robot robot, Station dest) {
        // 选择距离dest和自己最近的src,选择src时要综合考虑敌人位置
        // 距离 =  robot -> src -> dest
        double minTime = 100000;
        Station st = null;
        for (int ty : dest.getRaws()) {
            // 时间小于等于0 ，表明未生产，将会一直阻塞
            if (!dest.canBuy(ty)) continue;


            for (Station s: Main.stationsMap.get(ty)){
                if (s.place == StationStatus.BLOCK) continue;       // 阻塞不能送
                // 第一段空载，第二段满载
                double t1 = s.pathToFps(true,robot.pos); // 若寻路算法高效，需换成robot的pos
                double t2 = s.pathToFps(false,dest.pos);
                double t = t1 + t2;
                if (s.place == StationStatus.CANBUMP){       // 已改
                    t += Robot.srcBumpCost;    // 加上代价
                }
                if (t < minTime){
                    minTime = t;
                    st = s;
                }
            }
        }

        return st;
    }

    public void setPrioQueue() {
        // 给工作台设置一个优先队列
        if (stationsMap.containsKey(7)){
            targets.addAll(stationsMap.get(7));
            Collections.sort(targets);  // 重新排序
        }
    }

}
