package com.huawei.codecraft;

import com.huawei.codecraft.util.Point;

import java.util.Arrays;public class VectorAngle {
    public static void main(String[] args) {
        double[] a = {1,0 }; // 二维向量a
        double[] b = {0, 1}; // 二维向量b

        double dotProduct = Main.dotProduct(a[0],a[1], b[0],b[1]); // 计算点积
        double normA = Main.norm(a[0],a[1]); // 计算向量a的模长
        double normB = Main.norm( b[0],b[1]); // 计算向量b的模长

        double cosTheta = dotProduct / (normA * normB); // 计算余弦值
        double theta = Math.acos(cosTheta); // 将余弦值转化为弧度值

        System.out.println("向量a和向量b的夹角为：" + theta + "弧度");
    }


}
