package com.adrninistrator.javacg2.dto.methodcode;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author adrninistrator
 * @date 2025/3/21
 * @description: 方法代码指令，tableswitch
 */
@Getter
public class MethodCodeInstructionTableSwitch extends MethodCodeInstruction {

    // 默认跳转目标指令偏移量
    @Setter
    private int defaultTargetOffset;

    // case最小值
    @Setter
    private int low;

    // case最大值
    @Setter
    private int high;

    // 跳转目标列表
    @Setter
    private List<Integer> jumpTargetOffsetList;
}
