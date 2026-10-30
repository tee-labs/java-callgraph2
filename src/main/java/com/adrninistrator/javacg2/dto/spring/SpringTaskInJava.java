package com.adrninistrator.javacg2.dto.spring;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Java代码中通过@Scheduled注解定义的Spring定时任务信息
 * 各列含义与java-all-call-graph的jacg_spring_task表在Java代码中定义时的记录保持一致
 */
@Getter
public class SpringTaskInJava {

    // 方法hash+字节数
    @Setter
    private String methodHash;

    // 完整类名
    @Setter
    private String className;

    // 方法名
    @Setter
    private String methodName;

    // 完整方法（类名+方法名+参数）
    @Setter
    private String fullMethod;

    // 方法返回类型，包含数组标志
    @Setter
    private String returnType;
}
