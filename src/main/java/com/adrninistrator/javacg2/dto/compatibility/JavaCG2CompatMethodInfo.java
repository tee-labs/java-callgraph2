package com.adrninistrator.javacg2.dto.compatibility;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Jar兼容性检查使用的方法信息，来自method_info或dup_method_info结果文件中的一行
 */
@Getter
@Setter
public class JavaCG2CompatMethodInfo {

    // 完整方法（类名+方法名+参数）
    private String fullMethod;

    // 方法的access_flags
    private int accessFlags;

    // 返回类型类名（不包含数组标志）
    private String returnType;

    // 方法所在的jar文件序号
    private int jarNum;
}
