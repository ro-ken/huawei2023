package com.huawei.codecraft.menu;

import com.huawei.codecraft.util.LimitedQueue;

import java.util.ArrayList;
import java.util.Queue;

public class test {

    public static void main(String[] args) {
        Queue<String> queue = new LimitedQueue<>(5);
        queue.add("A");
        queue.add("B");
        queue.add("C");
        queue.add("D");
        queue.add("E");
        queue.add("F");
        System.out.println(queue); // 输出 [B, C, D, E, F]

    }
}
