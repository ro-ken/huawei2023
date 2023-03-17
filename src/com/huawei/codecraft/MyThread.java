package com.huawei.codecraft;

/**
 * @Author: ro_kin
 * @Data:2023/3/17 19:42
 * @Description: TODO
 */
public class MyThread extends Thread{
    @Override
    public void run() {
//        Main.printLog();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
