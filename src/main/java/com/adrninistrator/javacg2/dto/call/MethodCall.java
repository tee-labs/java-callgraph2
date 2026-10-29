package com.adrninistrator.javacg2.dto.call;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.enums.JavaCG2CalleeObjTypeEnum;
import com.adrninistrator.javacg2.common.enums.JavaCG2YesNoEnum;
import com.adrninistrator.javacg2.util.JavaCG2ClassMethodUtil;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.bcel.generic.Type;

/**
 * @author adrninistrator
 * @date 2022/9/20
 * @description: 方法之间调用关系
 */
@Getter
public class MethodCall {

    // 方法调用序号
    @Setter
    private int callId;

    // 方法调用是否启用
    @Setter
    private boolean enabled;

    // 方法调用类型
    @Setter
    private String methodCallType;

    // 调用者类名
    @Setter
    private String callerClassName;

    // 调用者方法名
    @Setter
    private String callerMethodName;

    // 调用者方法参数类型
    @Setter
    private String callerMethodArgTypes;

    // 调用者源代码行号
    @Setter
    private int callerSourceLine;

    // 调用者方法返回类型
    @Setter
    private String callerReturnType;

    // 被调用者类名
    @Setter
    private String calleeClassName;

    // 被调用者方法名
    @Setter
    private String calleeMethodName;

    // 被调用者方法参数类型
    @Setter
    private String calleeMethodArgTypes;

    // 被调用方法的参数类型数组，原始的
    @Setter
    private Type[] rawCalleeArgTypes;

    // 被调用方法的参数类型数组，用于显示的
    @Setter
    private Type[] displayCalleeArgTypes;

    // 被调用者类的数组维度，0代表非数组
    @Setter
    private int calleeArrayDimensions;

    // 被调用类型
    @Setter
    private JavaCG2CalleeObjTypeEnum calleeObjTypeEnum;

    // 原始返回类型
    @Setter
    private String rawReturnType;

    // 实际返回类型
    @Setter
    private String actualReturnType;

    // 描述
    @Setter
    private String description;

    //
    // 返回调用者完整方法
    public String genCallerFullMethod() {
        return JavaCG2ClassMethodUtil.formatFullMethod(callerClassName, callerMethodName, callerMethodArgTypes);
    }

    // 返回被调用类型对应的字符串
    public String genObjTypeEnum() {
        if (calleeObjTypeEnum == null) {
            return "";
        }
        return calleeObjTypeEnum.getType();
    }

    // 返回被调用者完整方法
    public String genCalleeFullMethod() {
        return JavaCG2ClassMethodUtil.formatFullMethod(calleeClassName, calleeMethodName, calleeMethodArgTypes);
    }

    // 生成在调用关系文件中的内容
    public String genMethodCallContent(String callerJarNum, String calleeJarNum) {
        return JavaCG2FileUtil.appendFileColumn(
                String.valueOf(callId),
                enabled ? JavaCG2YesNoEnum.YES.getStrValue() : JavaCG2YesNoEnum.NO.getStrValue(),
                genCallerFullMethod(),
                JavaCG2Constants.FILE_KEY_CALL_TYPE_FLAG1 + methodCallType + JavaCG2Constants.FILE_KEY_CALL_TYPE_FLAG2 + genCalleeFullMethod(),
                String.valueOf(callerSourceLine),
                callerReturnType,
                String.valueOf(calleeArrayDimensions),
                genObjTypeEnum(),
                rawReturnType,
                actualReturnType,
                callerJarNum,
                calleeJarNum,
                description == null ? "" : description
        );
    }
}
