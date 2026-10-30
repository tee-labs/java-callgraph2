package com.adrninistrator.javacg2.dto.jardiff;

import lombok.Getter;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: JarDiff比较发现的发生变化的方法信息（比对逻辑参考java-all-call-graph的ModifiedMethodInfo）
 */
@Getter
public class JavaCG2JarDiffModifiedMethodInfo {

    // 发生变化的完整方法
    private final String fullMethod;

    // 方法返回类型
    private final String methodReturnType;

    // 方法在旧jar文件中是否存在，true:存在（方法被修改） false:不存在（新增的方法）
    private final boolean oldMethodExists;

    public JavaCG2JarDiffModifiedMethodInfo(String fullMethod, String methodReturnType, boolean oldMethodExists) {
        this.fullMethod = fullMethod;
        this.methodReturnType = methodReturnType;
        this.oldMethodExists = oldMethodExists;
    }

    @Override
    public String toString() {
        return fullMethod;
    }
}
