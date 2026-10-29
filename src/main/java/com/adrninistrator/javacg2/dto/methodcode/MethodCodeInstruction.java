package com.adrninistrator.javacg2.dto.methodcode;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2025/3/18
 * @description: 方法代码指令
 */
@Getter
public class MethodCodeInstruction {

    // 指令偏移量
    @Setter
    private int offset;

    // 指令名称
    @Setter
    private String instructionName;
}
