package com.adrninistrator.javacg2.dto.spring;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author adrninistrator
 * @date 2025/6/18
 * @description: Java代码中定义的Spring Bean信息
 */
@Getter
public class SpringBeanInJava {

    // Spring Bean可能的类名
    @Setter
    private List<String> classNameList;

    // Spring Bean定义时使用的注解类名
    @Setter
    private String annotationClassName;

    // Spring Bean定义时所在的类名
    @Setter
    private String defineClassName;
}
