package com.adrninistrator.javacg2.dto.method;

import lombok.Getter;
import lombok.Setter;
import org.apache.bcel.generic.Type;

/**
 * @author adrninistrator
 * @date 2021/8/22
 * @description: 方法信息
 */
@Getter
public class JavaCG2MethodInfo {
    // 类名
    @Setter
    private String className;

    // 方法名
    @Setter
    private String methodName;

    // 方法参数类型
    @Setter
    private Type[] methodArgumentTypes;

    // 方法返回类型
    @Setter
    private Type methodReturnType;

    // 类的数组维，0代表非数组
    @Setter
    private int arrayDimensions;

    public JavaCG2MethodInfo(String className, String methodName, Type[] methodArgumentTypes, Type methodReturnType, int arrayDimensions) {
        this.className = className;
        this.methodName = methodName;
        this.methodArgumentTypes = methodArgumentTypes;
        this.methodReturnType = methodReturnType;
        this.arrayDimensions = arrayDimensions;
    }

    public JavaCG2MethodInfo(String className, String methodName, Type[] methodArgumentTypes, Type methodReturnType) {
        this.className = className;
        this.methodName = methodName;
        this.methodArgumentTypes = methodArgumentTypes;
        this.methodReturnType = methodReturnType;
        this.arrayDimensions = 0;
    }

    public JavaCG2MethodInfo copy() {
        return new JavaCG2MethodInfo(this.className, this.methodName, this.methodArgumentTypes, this.methodReturnType);
    }

    @Override
    public String toString() {
        return className + " " + methodName;
    }
}
