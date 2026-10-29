package com.adrninistrator.javacg2.dto.field;

import com.adrninistrator.javacg2.util.JavaCG2ClassMethodUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2025/2/19
 * @description: 对类中字段的方法调用
 */
@Getter
public class ClassFieldMethodCall extends ClassField {

    // 被调用的方法名
    @Setter
    private String methodName;

    // 被调用的方法参数类型
    @Setter
    private String[] argTypes;

    // 被调用的方法返回类型
    @Setter
    private String returnType;

    public String genFullMethod() {
        return JavaCG2ClassMethodUtil.formatFullMethodStr(getClassName(), methodName, argTypes);
    }
}
