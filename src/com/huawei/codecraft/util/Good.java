package com.huawei.codecraft.util;


/**
 * ClassName: Good
 * Package: com.huawei.codecraft.util
 * Description: 货物类
 */
public class Good {
    public int type;   // 物品类型 1-7
    public int[] call;     //生产配方
    public int buy;    // 购买价
    public int sell;   // 出售价
    public int earn;   // 赚钱

    public Good(int type, int[] call, int buy, int sell, int earn) {
        this.type = type;
        this.call = call;
        this.buy = buy;
        this.sell = sell;
        this.earn = earn;

    }

}
