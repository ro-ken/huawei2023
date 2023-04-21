package com.huawei.codecraft.util;


/**
 * ClassName: StationStatus
 * Package: com.huawei.codecraft.util
 * Description: 工作台状态
 */
public enum StationStatus {
    EMPTY,      // 周围没有敌人
    BLOCK,      // 周围被敌人阻塞
    CANBUMP,    // 周围有敌人，但可以撞开
}
