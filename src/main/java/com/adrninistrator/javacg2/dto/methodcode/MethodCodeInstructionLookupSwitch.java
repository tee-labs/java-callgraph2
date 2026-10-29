package com.adrninistrator.javacg2.dto.methodcode;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author adrninistrator
 * @date 2025/3/20
 * @description: 方法代码指令，lookupswitch
 */
@Getter
public class MethodCodeInstructionLookupSwitch extends MethodCodeInstruction {

    // 默认跳转目标指令偏移量
    @Setter
    private int defaultTargetOffset;

    // 键值对数量
    @Setter
    private int pairsNum;

    // 跳转目标列表
    @Setter
    private List<MethodCodeInstructionLookupSwitchJump> jumpList;
}
