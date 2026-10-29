package com.adrninistrator.javacg2.dto.instruction.parseresult;

import com.adrninistrator.javacg2.dto.element.BaseElement;
import lombok.Getter;

/**
 * @author adrninistrator
 * @date 2023/8/3
 * @description: PUTSTATIC指令解析结果
 */
@Getter
public class PutStaticParseResult extends BaseInstructionParseResult {

    // 类名
    private final String className;

    // 字段名称
    private final String fieldName;

    // 字段类型
    private final String fieldType;

    // 对应的值
    private final BaseElement value;

    public PutStaticParseResult(String className, String fieldName, String fieldType, BaseElement value) {
        this.className = className;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.value = value;
    }
}
