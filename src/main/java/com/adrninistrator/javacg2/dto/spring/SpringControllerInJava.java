package com.adrninistrator.javacg2.dto.spring;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Java代码中通过类及方法注解定义的Spring Controller信息
 * 各列含义与java-all-call-graph的jacg_spring_controller表保持一致
 */
@Getter
public class SpringControllerInJava {

    // 方法hash+字节数
    @Setter
    private String methodHash;

    // 完整类名
    @Setter
    private String className;

    // 序号，从0开始，大于0代表有多种可能
    @Setter
    private int seq;

    // 用于显示的URI
    @Setter
    private String showUri;

    // 类上的注解path属性原始值
    @Setter
    private String classPath;

    // 方法上的注解path属性原始值
    @Setter
    private String methodPath;

    // 方法上的注解类名
    @Setter
    private String annotationName;

    // 方法所在的jar文件序号
    @Setter
    private int jarNum;

    // 方法可能用于文件上传，1:是，0:否，2:不确定
    @Setter
    private int maybeFileUpload;

    // 方法可能用于文件下载，1:是，0:否，2:不确定
    @Setter
    private int maybeFileDownload;

    // 完整方法（类名+方法名+参数）
    @Setter
    private String fullMethod;

    // 方法返回类型，包含数组标志
    @Setter
    private String returnType;
}
