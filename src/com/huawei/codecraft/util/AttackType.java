package com.huawei.codecraft.util;


/**
 * ClassName: AttackType
 * Package: com.huawei.codecraft.util
 * Description: 攻击类型
 *
 * @author :ro_kin
 * @date : 2023/4/19
 */
public enum AttackType {
    BLOCK,      // 占一个位置
    RUSH,        // 目标在范围内乱撞
    FOLLOWING,        // 跟随目标 1换1
}
