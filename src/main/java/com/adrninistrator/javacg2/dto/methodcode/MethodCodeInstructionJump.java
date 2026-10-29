package com.adrninistrator.javacg2.dto.methodcode;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2025/3/21
 * @description: 方法代码指令，跳转
 */
@Getter
public class MethodCodeInstructionJump extends MethodCodeInstruction {

    // 跳转目标指令偏移量
    @Setter
    private int targetOffset;
}
