package com.huawei.codecraft.util;


/**
 * ClassName: Goods
 * Package: com.huawei.codecraft.util
 * Description: 记录货物信息
 *
 * @Author: ro_kin
 * @Data:2023/3/13 19:56
 */
public class Goods {
    public static final Good[] item;

    static {
        item = new Good[8];
        item[1] = new Good(1, new int[]{}, 3000, 6000, 3000);
        item[2] = new Good(2, new int[]{}, 4400, 7600, 3200);
        item[3] = new Good(3, new int[]{}, 5800, 9200, 3400);
        item[4] = new Good(4, new int[]{1, 2}, 15400, 22500, 7100);
        item[5] = new Good(5, new int[]{1, 3}, 17200, 25000, 7800);
        item[6] = new Good(6, new int[]{2, 3}, 19200, 27500, 8300);
        item[7] = new Good(7, new int[]{4, 5, 6}, 76000, 105000, 29000);
    }

    public Goods() {

    }
}

