package com.adrninistrator.javacg2.dto.variabledatasource;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2024/6/10
 * @description: 变量的数据来源，使用字段
 */
@Getter
public class VariableDataSourceField extends AbstractVariableDataSource {

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
