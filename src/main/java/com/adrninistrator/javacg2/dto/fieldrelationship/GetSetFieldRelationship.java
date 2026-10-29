package com.adrninistrator.javacg2.dto.fieldrelationship;

import com.adrninistrator.javacg2.common.enums.JavaCG2FieldRelationshipTypeEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2023/10/16
 * @description: get/set方法对应的字段关系
 */
@Getter
public class GetSetFieldRelationship {

    // 记录ID
    @Setter
    private int recordId;

    // set方法调用的指令位置
    @Setter
    private Integer setInvokeInstructionPosition;

    // get方法调用的指令位置
    @Setter
    private Integer getInvokeInstructionPosition;

    // 调用方，源代码行号
    @Setter
    private int callerLineNumber;

    // get方法完整类名
    @Setter
    private String getClassName;

    // get方法方法名
    @Setter
    private String getMethodName;

    // set方法完整类名
    @Setter
    private String setClassName;

    // set方法方法名
    @Setter
    private String setMethodName;

    // 关联关系是否有效
    @Setter
    private String valid;

    // 关联关系类型
    @Setter
    private JavaCG2FieldRelationshipTypeEnum type;
}
