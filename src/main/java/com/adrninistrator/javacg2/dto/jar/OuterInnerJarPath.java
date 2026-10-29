package com.adrninistrator.javacg2.dto.jar;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2025/1/27
 * @description: jar/war文件及其中的jar文件
 */
@Getter
public class OuterInnerJarPath {

    // 外层jar/war文件路径
    @Setter
    private String outerJarPath;

    // 内层jar文件路径
    @Setter
    private String innerJarPath;
}
