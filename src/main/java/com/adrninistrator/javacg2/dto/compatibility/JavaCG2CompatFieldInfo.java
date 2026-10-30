package com.adrninistrator.javacg2.dto.compatibility;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Jar兼容性检查使用的字段信息，来自field_info或dup_field_info结果文件中的一行
 */
@Getter
@Setter
public class JavaCG2CompatFieldInfo {

    // 完整类名
    private String className;

    // 字段名称
    private String fieldName;

    // 字段类型（不包含数组标志）
    private String fieldType;

    // 字段修饰符
    private String modifiers;

    // static标志，1:是，0:否
    private String staticFlag;

    // 字段所在的jar文件序号
    private int jarNum;
}
