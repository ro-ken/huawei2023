package com.huawei.codecraft.core;

import com.huawei.codecraft.Main;

/**
 * @Author: ro_kin
 * @Data:2023/3/17 19:42
 * @Description: TODO
 */

// 还未用到
public class MyThread extends Thread{
    public int lastFrame = 0;   // 上一帧序号

    @Override
    public void run() {
//        Main.printLog("Thread2 ... ");

        while (true){
            if (lastFrame == Main.frameID){
                continue;   //当前帧信息处理完毕
            }
            lastFrame = Main.frameID;
            // 算出4个机器人的运动方程
            // 默认在旋转不参与碰撞计算
//            for (int i = 0; i < 4; i++) {
//                Main.robots[i].calcMoveEquation();
//            }
//            // 对每个机器人依次进行碰撞计算
//            for (int i = 0; i < 3; i++) {
//                for (int j = i+1; j < 4; j++) {
//                    Main.robots[i].calcBump(Main.robots[j]);
//                }
//            }

            // 两两判断方程是否有相交，判断六次


            // 相交在判断时序是否有重叠


            // 若有重叠，则需绕道


        }

    }
}
