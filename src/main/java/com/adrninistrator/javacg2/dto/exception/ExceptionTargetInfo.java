package com.adrninistrator.javacg2.dto.exception;

import lombok.Getter;
import org.apache.bcel.generic.InstructionHandle;

/**
 * @author adrninistrator
 * @date 2022/10/30
 * @description: Exception table的target指令及异常类型
 */
@Getter
public class ExceptionTargetInfo {

    // Exception table的target指令
    private final InstructionHandle target;

    // 异常类型
    private final String exceptionType;

    public ExceptionTargetInfo(InstructionHandle target, String exceptionType) {
        this.target = target;
        this.exceptionType = exceptionType;
    }
}
