package com.adrninistrator.javacg2.dto.type;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2024/10/24
 * @description: 泛型相关的类型信息
 */
@Getter
public class JavaCG2GenericsType extends JavaCG2Type {

    // 泛型类型变量名称
    @Setter
    private String typeVariablesName;

    // 通配符
    @Setter
    private String wildcard;

    // 通配符引用的类型，如extends xxx，super xxx
    @Setter
    private String referenceType;

    @Override
    public String toString() {
        return "JavaCG2GenericsType{" +
                "typeVariablesName='" + typeVariablesName + '\'' +
                ", wildcard='" + wildcard + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", type='" + type + '\'' +
                ", arrayDimensions=" + arrayDimensions +
                '}';
    }
}
