package com.adrninistrator.javacg2.dto.field;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2025/2/21
 * @description: 类中的字段
 */
@Getter
public class ClassField {

    // 类名
    @Setter
    private String className;

    // 字段名称
    @Setter
    private String fieldName;

    // 字段类型
    @Setter
    private String fieldType;
}
