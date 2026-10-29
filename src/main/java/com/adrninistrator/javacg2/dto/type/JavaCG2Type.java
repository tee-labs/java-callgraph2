package com.adrninistrator.javacg2.dto.type;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2024/11/14
 * @description: 包含数组维度的类型
 */
@Getter
public class JavaCG2Type {

    // 类型
    @Setter
    protected String type;

    // 属于数组类型时的数组维度，0代表不是数组
    @Setter
    protected int arrayDimensions;
}
