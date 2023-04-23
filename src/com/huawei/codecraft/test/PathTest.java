package com.huawei.codecraft.test;

import com.huawei.codecraft.util.LimitedQueue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Queue;


/**
 * ClassName: test
 * Package: com.huawei.codecraft.menu
 * Description: 用于测试
 */
public class PathTest {

    @Test
    public void test1(){
        LimitedQueue<String> queue = new LimitedQueue<>(5);
        queue.add("A");
        queue.add("B");
        queue.add("C");
        queue.add("D");
        queue.add("E");
        queue.add("F");
        queue.add("G");

//        System.out.print("正序遍历结果：");
//        queue.forwardPrint(); // 输出 "正序遍历结果：C D E F G "
//
//        System.out.print("逆序遍历结果：");
//        queue.reversePrint(); // 输出 "逆序遍历结果：G F E D C "
    }

}
