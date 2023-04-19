package com.huawei.codecraft.menu;

import com.huawei.codecraft.util.Point;

import java.util.Arrays;

public class VectorAngle2D {
    public static void main(String[] args) {
        double[] direction = {1, 1}; // 方向向量
        double angle = Math.PI / 2; // 夹角（弧度制）
        
        double[] unitDirection = normalize(direction); // 单位向量
        double[] u = {-unitDirection[1], unitDirection[0]}; // 与单位向量垂直的向量
        
        double[] v1 = add(scale(Math.cos(angle), unitDirection), scale(Math.sin(angle), u));
        double[] v2 = add(scale(Math.cos(angle), unitDirection), scale(-Math.sin(angle), u));
        
        System.out.println(Arrays.toString(v1));
        System.out.println(Arrays.toString(v2));
    }

    public static Point[] calc2vec(Point vec,double angle) {
        // 根据一个向量和夹角，算另外两个向量

        double[] direction = {1, 1}; // 方向向量

        double[] unitDirection = normalize(direction); // 单位向量
        double[] u = {-unitDirection[1], unitDirection[0]}; // 与单位向量垂直的向量

        double[] v1 = add(scale(Math.cos(angle), unitDirection), scale(Math.sin(angle), u));
        double[] v2 = add(scale(Math.cos(angle), unitDirection), scale(-Math.sin(angle), u));

        Point[] res = new Point[2];
        res[0] = new Point(v1[0],v1[1]);
        res[1] = new Point(v2[0],v2[1]);

        return res;
    }


    // 求向量长度
    public static double magnitude(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1]);
    }
    
    // 求向量的单位向量
    public static double[] normalize(double[] v) {
        double mag = magnitude(v);
        return new double[] {v[0] / mag, v[1] / mag};
    }

    // 求向量的标量乘积
    public static double[] scale(double c, double[] v) {
        return new double[] {c * v[0], c * v[1]};
    }
    
    // 求向量的加法
    public static double[] add(double[] v1, double[] v2) {
        return new double[] {v1[0] + v2[0], v1[1] + v2[1]};
    }
}
