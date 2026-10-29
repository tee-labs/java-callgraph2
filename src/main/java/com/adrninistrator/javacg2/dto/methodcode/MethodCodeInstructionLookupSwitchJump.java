package com.adrninistrator.javacg2.dto.methodcode;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2025/3/21
 * @description: lookupswitch指令中的数据
 */
@Getter
public class MethodCodeInstructionLookupSwitchJump {

    // 索引
    @Setter
    private int index;

    // 跳转目标指令偏移量
    @Setter
    private int targetOffset;
}
