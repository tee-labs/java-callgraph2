package com.adrninistrator.javacg2.conf;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author adrninistrator
 * @date 2022/11/4
 * @description: 配置参数类
 */
@Getter
public class JavaCG2ConfInfo {

    // 当前使用的生成文件的目录
    @Setter
    private String outputDirPath;

    // 处理方法调用时是否解析可能的类型与值
    @Setter
    private boolean parseMethodCallTypeValue;

    // 处理类的方法前是否需要先解析构造函数以非静态字段可能的类型
    @Setter
    private boolean firstParseInitMethodType;

    // 是否需要分析dto的字段之间的关联关系
    @Setter
    private boolean analyseFieldRelationship;

    // 解析方法调用时，通过new创建的被调用类型是否处理原始类型
    @Setter
    private boolean handleCalleeNewRaw;

    // 解析方法调用时，通过new创建的被调用类型是否处理实际类型
    @Setter
    private boolean handleCalleeNewActual;

    // 解析方法调用时，被调用对象为Spring Bean，是否处理原始类型（支持字段注入）
    @Setter
    private boolean handleCalleeSpringBeanRaw;

    // 解析方法调用时，被调用对象为Spring Bean，是否处理实际类型（支持字段注入）
    @Setter
    private boolean handleCalleeSpringBeanActual;

    /*
        在处理通过get/set方法的字段关联关系时使用，指定方法返回值与被调用对象或参数认为是等值转换的方法
        外层key   类名
        外层value
            内层key   方法名
            内层value 与方法返回值等值的被调用对象（使用0表示）或方法参数（从1开始）序号
     */
    @Setter
    private Map<String, Map<String, Integer>> frEqConversionMethodMap;

    // 是否使用Jar兼容性检查模式，仅解析类、方法、字段等基础信息
    @Setter
    private boolean parseJarCompatibilityMode;

    // 是否仅解析类信息
    @Setter
    private boolean parseOnlyClassMode;

    // 是否执行Jar兼容性检查并生成检查报告结果文件（使用Jar兼容性检查模式时自动执行）
    @Setter
    private boolean checkJarCompatibilityEnable;

    // 是否使用内置的Spring XML文件中定义的Bean解析类
    @Setter
    private boolean useBuiltInSpringXmlBeanParser;

    // 是否使用内置的Spring XML文件中的定时任务、AOP、组件扫描信息解析类
    @Setter
    private boolean useBuiltInSpringXmlTaskAopScanParser;
}
