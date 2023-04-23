package com.huawei.codecraft.util;


/**
 * ClassName: StationItem
 * Package: com.huawei.codecraft.util
 * Description: 工作台信息
 */
public class StationItem {
    public int type;
    public int[] call;
    public int period;
    public int[] canSell;  // product can sell for who

    /**
     * 构造器
     *
     * @param type    工作台种类
     * @param call    工作台可以从哪买
     * @param period  工作周期
     * @param canSell 工作台可以卖到哪
     */
    public StationItem(int type, int[] call, int period, int[] canSell) {
        this.type = type;
        this.call = call;
        this.period = period;
        this.canSell = canSell;
    }
}