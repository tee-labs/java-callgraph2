package com.adrninistrator.javacg2.dto.methodcode;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2025/3/21
 * @description: 方法代码指令，包含一个参数
 */
@Getter
public class MethodCodeInstructionArg extends MethodCodeInstruction {

    // 参数
    @Setter
    private String arg;
}
