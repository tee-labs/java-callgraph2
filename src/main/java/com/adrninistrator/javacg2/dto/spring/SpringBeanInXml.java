package com.adrninistrator.javacg2.dto.spring;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2025/6/18
 * @description: XML文件中定义的Spring Bean信息
 */
@Getter
public class SpringBeanInXml {

    // Spring Bean名称
    @Setter
    private String springBeanName;

    // Spring Bean类名
    @Setter
    private String className;

    // Spring Bean profile
    @Setter
    private String profile;

    // XML文件路径
    @Setter
    private String xmlFilePath;
}
