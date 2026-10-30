package com.adrninistrator.javacg2.dto.jardiff;

import lombok.Getter;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: JarDiff比较发现的发生变化的类信息
 */
@Getter
public class JavaCG2JarDiffModifiedClassInfo {

    // 完整类名
    private final String className;

    // 类在旧jar文件中是否存在，true:存在（class文件HASH发生变化） false:不存在（新增的类）
    private final boolean oldClassExists;

    // 新class文件在jar文件中的路径
    private final String newClassPathInJar;

    // 新类所在的jar文件序号
    private final int newJarNum;

    // 旧class文件在jar文件中的路径，新增的类为null
    private final String oldClassPathInJar;

    // 旧类所在的jar文件序号，新增的类为null
    private final Integer oldJarNum;

    public JavaCG2JarDiffModifiedClassInfo(String className, boolean oldClassExists, String newClassPathInJar, int newJarNum,
                                           String oldClassPathInJar, Integer oldJarNum) {
        this.className = className;
        this.oldClassExists = oldClassExists;
        this.newClassPathInJar = newClassPathInJar;
        this.newJarNum = newJarNum;
        this.oldClassPathInJar = oldClassPathInJar;
        this.oldJarNum = oldJarNum;
    }

    @Override
    public String toString() {
        return className;
    }
}
