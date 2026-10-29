package com.adrninistrator.javacg2.dto.spring;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2025/6/27
 * @description:
 */
@Getter
public class SpringAopAdviceInfo {

    // 完整方法
    @Setter
    private String fullMethod;

    // 方法返回类型
    @Setter
    private String returnType;

    // advice注解简单类
    @Setter
    private String annotationSimpleClassName;

    // advice的表达式
    @Setter
    private String expression;

    // aspect的排序值
    @Setter
    private int aspectOrder;
}
