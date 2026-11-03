package com.adrninistrator.javacg2.dto.instruction;

import com.adrninistrator.javacg2.dto.method.JavaCG2MethodInfo;
import lombok.Getter;
import org.apache.bcel.generic.Type;

/**
 * @author adrninistrator
 * @date 2026/11/4
 * @description: 方法调用指令解析出的不可变要素，用于在单个方法的处理作用域内按指令位置缓存，避免同一指令被重复解析常量池
 */
@Getter
public class JavaCG2InvokeInstructionInfo {

    // 被调用方法信息（被调用类名/方法名/参数类型/返回类型/数组维度），解析后不再变化，允许共享
    private final JavaCG2MethodInfo methodInfo;

    // 方法调用指令中引用类型的字符串形式（ReferenceType.toString()，数组类型包含"["前缀），与methodInfo中的被调用类名的语义不同，分别记录
    private final String rawReferenceTypeStr;

    // 被调用方法参数类型的字符串形式（如 (int,java.lang.String)）
    private final String argTypeStr;

    public JavaCG2InvokeInstructionInfo(JavaCG2MethodInfo methodInfo, String rawReferenceTypeStr, String argTypeStr) {
        this.methodInfo = methodInfo;
        this.rawReferenceTypeStr = rawReferenceTypeStr;
        this.argTypeStr = argTypeStr;
    }

    // 被调用类名
    public String getClassName() {
        return methodInfo.getClassName();
    }

    // 被调用方法名
    public String getMethodName() {
        return methodInfo.getMethodName();
    }

    // 被调用方法参数类型
    public Type[] getArgumentTypes() {
        return methodInfo.getMethodArgumentTypes();
    }

    // 被调用方法返回类型
    public Type getReturnType() {
        return methodInfo.getMethodReturnType();
    }

    // 被调用方对象数组的维度，为0代表不是数组类型
    public int getArrayDimensions() {
        return methodInfo.getArrayDimensions();
    }
}
