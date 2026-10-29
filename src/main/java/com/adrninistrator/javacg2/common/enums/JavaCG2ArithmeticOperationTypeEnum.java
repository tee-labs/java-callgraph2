package com.adrninistrator.javacg2.common.enums;

import lombok.Getter;

/**
 * @author adrninistrator
 * @date 2023/8/5
 * @description: 算术运算类型枚举
 */
@Getter
public enum JavaCG2ArithmeticOperationTypeEnum {
    AOTE_ADD("+"),
    AOTE_SUB("-"),
    AOTE_MUL("*"),
    AOTE_DIV("/"),
    ;

    private final String type;

    JavaCG2ArithmeticOperationTypeEnum(String type) {
        this.type = type;
    }
}
