package com.huawei.codecraft;

import com.huawei.codecraft.util.Point;
import java.util.Scanner;
import java.util.Vector;

public class VectorOrientation {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        Vector<Integer> a = new Vector<Integer>(2);
        a.add(1);
        a.add(1);
        Vector<Integer> b = new Vector<Integer>(2);
        b.add(2);
        b.add(1);

        // 计算向量 a 和 b 的叉积
        int crossProduct = a.get(0) * b.get(1) - a.get(1) * b.get(0);

        // 判断向量 a 在向量 b 的左边还是右边
        if (crossProduct > 0) {
            System.out.println("Vector a is on the left of vector b.");
        } else if (crossProduct < 0) {
            System.out.println("Vector a is on the left of vector b.");
        }
    }
}