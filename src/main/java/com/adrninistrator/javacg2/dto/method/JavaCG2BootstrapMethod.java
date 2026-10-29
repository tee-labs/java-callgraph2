package com.adrninistrator.javacg2.dto.method;

import lombok.Getter;
import lombok.Setter;
import org.apache.bcel.generic.Type;

/**
 * @author adrninistrator
 * @date 2025/7/24
 * @description:
 */
@Getter
public class JavaCG2BootstrapMethod {

    // bootstrap方法信息
    @Setter
    private JavaCG2MethodInfo bootstrapMethodInfo;

    // lambda表达式方法信息
    @Setter
    private JavaCG2MethodInfo lambdaMethodInfo;

    // lambda表达式方法实际参数类型
    @Setter
    private Type[] lambdaMethodActualArgumentTypes;

    // lambda表达式方法实际返回类型
    @Setter
    private Type lambdaMethodActualReturnType;
}
