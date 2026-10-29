package com.adrninistrator.javacg2.dto.spring;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2025/6/27
 * @description:
 */
@Getter
public class SpringAopPointcutInfo {

    // 完整方法
    @Setter
    private String fullMethod;

    // pointcut的表达式
    @Setter
    private String expression;
}
