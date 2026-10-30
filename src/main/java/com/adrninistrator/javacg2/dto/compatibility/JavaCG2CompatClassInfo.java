package com.adrninistrator.javacg2.dto.compatibility;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Jar兼容性检查使用的类信息，来自class_info或dup_class_info结果文件中的一行
 */
@Getter
@Setter
public class JavaCG2CompatClassInfo {

    // 完整类名
    private String className;

    // 类的access_flags
    private int accessFlags;

    // 类文件的HASH值（MD5）
    private String classFileHash;

    // 类所在的jar文件序号
    private int jarNum;

    // 类在jar包中的路径
    private String classPathInJar;
}
