package com.adrninistrator.javacg2.builder;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.el.enums.CommonElAllowedVariableEnum;
import com.adrninistrator.javacg2.el.enums.ElStringAnyFunctionEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElAllowedVariableEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.el.enums.interfaces.ElAllowedVariableInterface;
import com.adrninistrator.javacg2.el.enums.interfaces.ElConfigInterface;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: _javacg2_parse_class_method_switch/与_javacg2_parse_method_call_switch/目录下表达式配置文件对应的Builder，通过代码以链式方式指定解析类、方法、方法调用时使用的忽略表达式，不依赖配置文件
 * 语义化方法内部生成合法的Aviator表达式文本（字符串参数自动以单引号包裹，不以分号结尾），表达式执行结果为true时跳过解析对应的类、方法或方法调用
 * 同一个表达式配置枚举对应的方法多次调用时，以后调用的为准（表达式文本整体替换）
 * 未通过当前类设置的表达式配置使用jar包中内置配置文件的默认内容（parse_ignore_method.av内置默认表达式忽略hashCode与equals方法，其余内置默认为空白，即不生效）
 * 当前类的实例只能通过applyTo方法应用到一个JavaCG2ConfigureWrapper对象，假如需要应用多个，请创建当前类的多个实例
 */
@Slf4j
public class JavaCG2ParseSwitchBuilder {

    // 表达式开关配置的公共逻辑
    private final ElSwitchConfigHolder elSwitchConfigHolder = new ElSwitchConfigHolder();

    public static JavaCG2ParseSwitchBuilder builder() {
        return new JavaCG2ParseSwitchBuilder();
    }

    private JavaCG2ParseSwitchBuilder() {
    }

    /**
     * 按完整类名跳过解析类，使用string.equalsAny函数判断完整类名是否与指定的一个或多个类名完全相等
     *
     * @param classNames 一个或多个完整类名，如"a.b.Class1"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreClassByClassName(String... classNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_CLASS, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_PARSE_CLASS_NAME, classNames);
    }

    /**
     * 按完整包名跳过解析类，使用string.equalsAny函数判断完整包名是否与指定的一个或多个包名完全相等
     *
     * @param packageNames 一个或多个完整包名，如"a.b"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreClassByPackageName(String... packageNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_CLASS, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_PARSE_PACKAGE_NAME, packageNames);
    }

    /**
     * 按完整包名前缀跳过解析类，使用string.startsWithAny函数判断完整包名是否以指定的一个或多个前缀开头
     *
     * @param packagePrefixes 一个或多个完整包名前缀，如"a.b."
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreClassByPackageNamePrefix(String... packagePrefixes) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_CLASS, ElStringAnyFunctionEnum.STARTS_WITH_ANY, JavaCG2ElAllowedVariableEnum.EAVE_PARSE_PACKAGE_NAME,
                packagePrefixes);
    }

    /**
     * 按简单类名跳过解析类，使用string.equalsAny函数判断简单类名是否与指定的一个或多个简单类名完全相等
     *
     * @param simpleClassNames 一个或多个简单类名，如"Class1"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreClassBySimpleClassName(String... simpleClassNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_CLASS, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_PARSE_SIMPLE_CLASS_NAME,
                simpleClassNames);
    }

    /**
     * 按完整方法跳过解析方法，使用string.equalsAny函数判断完整方法是否与指定的一个或多个完整方法完全相等
     *
     * @param fullMethods 一个或多个完整方法，如"a.b.Class1:f1()"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodByFullMethod(String... fullMethods) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_PARSE_FULL_METHOD, fullMethods);
    }

    /**
     * 按完整方法后缀跳过解析方法，使用string.endsWithAny函数判断完整方法是否以指定的一个或多个后缀结尾
     * 与jar包中内置默认表达式使用的判断方式相同，如"hashCode()"、"equals(java.lang.Object)"
     *
     * @param fullMethodSuffixes 一个或多个完整方法后缀，如"hashCode()"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodByFullMethodSuffix(String... fullMethodSuffixes) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD, ElStringAnyFunctionEnum.ENDS_WITH_ANY, JavaCG2ElAllowedVariableEnum.EAVE_PARSE_FULL_METHOD,
                fullMethodSuffixes);
    }

    /**
     * 按完整类名跳过解析方法，使用string.equalsAny函数判断完整类名是否与指定的一个或多个类名完全相等
     *
     * @param classNames 一个或多个完整类名，如"a.b.Class1"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodByClassName(String... classNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_PARSE_CLASS_NAME, classNames);
    }

    /**
     * 按完整类名前缀跳过解析方法，使用string.startsWithAny函数判断完整类名是否以指定的一个或多个前缀开头
     *
     * @param classPrefixes 一个或多个完整类名前缀，如"a.b."
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodByClassNamePrefix(String... classPrefixes) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD, ElStringAnyFunctionEnum.STARTS_WITH_ANY, JavaCG2ElAllowedVariableEnum.EAVE_PARSE_CLASS_NAME,
                classPrefixes);
    }

    /**
     * 按方法名跳过解析方法，使用string.equalsAny函数判断方法名是否与指定的一个或多个方法名完全相等
     *
     * @param methodNames 一个或多个方法名，如"method1"，不包括括号及方法参数
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodByMethodName(String... methodNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_PARSE_METHOD_NAME, methodNames);
    }

    /**
     * 按方法调用类型跳过解析方法调用，使用string.equalsAny函数判断方法调用类型是否与指定的一个或多个类型完全相等
     *
     * @param callTypes 一个或多个方法调用类型，如"VIR"、"INT"，参考JavaCG2CallTypeEnum类
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodCallByCallType(String... callTypes) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL, ElStringAnyFunctionEnum.EQUALS_ANY, CommonElAllowedVariableEnum.EAVE_METHOD_CALL_TYPE, callTypes);
    }

    /**
     * 按调用方完整类名跳过解析方法调用，使用string.equalsAny函数判断调用方完整类名是否与指定的一个或多个类名完全相等
     *
     * @param classNames 一个或多个调用方完整类名，如"a.b.Class1"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodCallByCallerClassName(String... classNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL, ElStringAnyFunctionEnum.EQUALS_ANY, CommonElAllowedVariableEnum.EAVE_MC_ER_CLASS_NAME, classNames);
    }

    /**
     * 按调用方完整包名跳过解析方法调用，使用string.equalsAny函数判断调用方完整包名是否与指定的一个或多个包名完全相等
     *
     * @param packageNames 一个或多个调用方完整包名，如"a.b"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodCallByCallerPackageName(String... packageNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL, ElStringAnyFunctionEnum.EQUALS_ANY, CommonElAllowedVariableEnum.EAVE_MC_ER_PACKAGE_NAME,
                packageNames);
    }

    /**
     * 按调用方完整包名前缀跳过解析方法调用，使用string.startsWithAny函数判断调用方完整包名是否以指定的一个或多个前缀开头
     *
     * @param packagePrefixes 一个或多个调用方完整包名前缀，如"a.b."
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodCallByCallerPackageNamePrefix(String... packagePrefixes) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL, ElStringAnyFunctionEnum.STARTS_WITH_ANY, CommonElAllowedVariableEnum.EAVE_MC_ER_PACKAGE_NAME,
                packagePrefixes);
    }

    /**
     * 按调用方完整方法跳过解析方法调用，使用string.equalsAny函数判断调用方完整方法是否与指定的一个或多个完整方法完全相等
     *
     * @param fullMethods 一个或多个调用方完整方法，如"a.b.Class1:method1(int)"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodCallByCallerFullMethod(String... fullMethods) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL, ElStringAnyFunctionEnum.EQUALS_ANY, CommonElAllowedVariableEnum.EAVE_MC_ER_FULL_METHOD,
                fullMethods);
    }

    /**
     * 按被调用方完整类名跳过解析方法调用，使用string.equalsAny函数判断被调用方完整类名是否与指定的一个或多个类名完全相等
     *
     * @param classNames 一个或多个被调用方完整类名，如"a.b.Class1"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodCallByCalleeClassName(String... classNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL, ElStringAnyFunctionEnum.EQUALS_ANY, CommonElAllowedVariableEnum.EAVE_MC_EE_CLASS_NAME, classNames);
    }

    /**
     * 按被调用方完整包名跳过解析方法调用，使用string.equalsAny函数判断被调用方完整包名是否与指定的一个或多个包名完全相等
     *
     * @param packageNames 一个或多个被调用方完整包名，如"a.b"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodCallByCalleePackageName(String... packageNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL, ElStringAnyFunctionEnum.EQUALS_ANY, CommonElAllowedVariableEnum.EAVE_MC_EE_PACKAGE_NAME,
                packageNames);
    }

    /**
     * 按被调用方完整包名前缀跳过解析方法调用，使用string.startsWithAny函数判断被调用方完整包名是否以指定的一个或多个前缀开头
     *
     * @param packagePrefixes 一个或多个被调用方完整包名前缀，如"a.b."
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodCallByCalleePackageNamePrefix(String... packagePrefixes) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL, ElStringAnyFunctionEnum.STARTS_WITH_ANY, CommonElAllowedVariableEnum.EAVE_MC_EE_PACKAGE_NAME,
                packagePrefixes);
    }

    /**
     * 按被调用方方法名跳过解析方法调用，使用string.equalsAny函数判断被调用方方法名是否与指定的一个或多个方法名完全相等
     *
     * @param methodNames 一个或多个被调用方方法名，如"method1"，不包括括号及方法参数
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodCallByCalleeMethodName(String... methodNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL, ElStringAnyFunctionEnum.EQUALS_ANY, CommonElAllowedVariableEnum.EAVE_MC_EE_METHOD_NAME,
                methodNames);
    }

    /**
     * 按被调用方完整方法跳过解析方法调用，使用string.equalsAny函数判断被调用方完整方法是否与指定的一个或多个完整方法完全相等
     *
     * @param fullMethods 一个或多个被调用方完整方法，如"a.b.Class1:method1(int)"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreMethodCallByCalleeFullMethod(String... fullMethods) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL, ElStringAnyFunctionEnum.EQUALS_ANY, CommonElAllowedVariableEnum.EAVE_MC_EE_FULL_METHOD,
                fullMethods);
    }

    /**
     * 按调用方完整类名跳过解析方法调用被调用对象和参数可能的类型与值，使用string.equalsAny函数判断调用方完整类名是否与指定的一个或多个类名完全相等
     * 仅当parse.method.call.type.value参数为true时当前表达式才会生效
     *
     * @param classNames 一个或多个调用方完整类名，如"a.b.Class1"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreTypeValueByCallerClassName(String... classNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL_TYPE_VALUE_CALLER, ElStringAnyFunctionEnum.EQUALS_ANY,
                CommonElAllowedVariableEnum.EAVE_MC_ER_CLASS_NAME, classNames);
    }

    /**
     * 按调用方完整包名跳过解析方法调用被调用对象和参数可能的类型与值，使用string.equalsAny函数判断调用方完整包名是否与指定的一个或多个包名完全相等
     * 仅当parse.method.call.type.value参数为true时当前表达式才会生效
     *
     * @param packageNames 一个或多个调用方完整包名，如"a.b"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreTypeValueByCallerPackageName(String... packageNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL_TYPE_VALUE_CALLER, ElStringAnyFunctionEnum.EQUALS_ANY,
                CommonElAllowedVariableEnum.EAVE_MC_ER_PACKAGE_NAME, packageNames);
    }

    /**
     * 按调用方完整包名前缀跳过解析方法调用被调用对象和参数可能的类型与值，使用string.startsWithAny函数判断调用方完整包名是否以指定的一个或多个前缀开头
     * 仅当parse.method.call.type.value参数为true时当前表达式才会生效
     *
     * @param packagePrefixes 一个或多个调用方完整包名前缀，如"a.b."
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreTypeValueByCallerPackageNamePrefix(String... packagePrefixes) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL_TYPE_VALUE_CALLER, ElStringAnyFunctionEnum.STARTS_WITH_ANY,
                CommonElAllowedVariableEnum.EAVE_MC_ER_PACKAGE_NAME, packagePrefixes);
    }

    /**
     * 按调用方完整方法跳过解析方法调用被调用对象和参数可能的类型与值，使用string.equalsAny函数判断调用方完整方法是否与指定的一个或多个完整方法完全相等
     * 仅当parse.method.call.type.value参数为true时当前表达式才会生效
     *
     * @param fullMethods 一个或多个调用方完整方法，如"a.b.Class1:method1(int)"
     * @return
     */
    public JavaCG2ParseSwitchBuilder ignoreTypeValueByCallerFullMethod(String... fullMethods) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL_TYPE_VALUE_CALLER, ElStringAnyFunctionEnum.EQUALS_ANY,
                CommonElAllowedVariableEnum.EAVE_MC_ER_FULL_METHOD, fullMethods);
    }

    /**
     * 通过原始表达式文本设置跳过解析类的表达式
     * 可用于语义化方法未覆盖的用法，如使用string.containsIC等函数
     *
     * @param elText 原始表达式文本，如"simple_class_name != 'Class1'"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2ParseSwitchBuilder setIgnoreClassExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_CLASS, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置跳过解析方法的表达式
     * 可用于语义化方法未覆盖的用法，如使用string.containsIC、method_arg_num等
     *
     * @param elText 原始表达式文本，如"method_arg_num != 4"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2ParseSwitchBuilder setIgnoreMethodExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置跳过解析方法调用的表达式
     * 可用于语义化方法未覆盖的用法，如使用string.containsIC、er_method_arg_num等
     *
     * @param elText 原始表达式文本，如"method_call_type != 'VIR'"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2ParseSwitchBuilder setIgnoreMethodCallExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置跳过解析方法调用被调用对象和参数可能的类型与值的表达式
     * 可用于语义化方法未覆盖的用法，如使用string.containsIC、er_method_arg_num等
     * 仅当parse.method.call.type.value参数为true时当前表达式才会生效
     *
     * @param elText 原始表达式文本，如"er_method_name != 'test1'"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2ParseSwitchBuilder setIgnoreTypeValueCallerExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL_TYPE_VALUE_CALLER, elText);
        return this;
    }

    /**
     * 将当前收集的表达式配置应用到配置参数包装对象
     *
     * @param javaCG2ConfigureWrapper
     */
    public void applyTo(JavaCG2ConfigureWrapper javaCG2ConfigureWrapper) {
        if (javaCG2ConfigureWrapper == null) {
            throw new JavaCG2ConfigException("不允许传入null " + JavaCG2ConfigureWrapper.class.getSimpleName());
        }
        elSwitchConfigHolder.applyTo(javaCG2ConfigureWrapper);
    }

    // 获取已设置表达式配置的枚举集合
    Set<ElConfigInterface> getSetElConfigSet() {
        return elSwitchConfigHolder.getSetElConfigSet();
    }

    // 记录使用字符串比较函数生成的表达式配置，字符串参数自动以单引号包裹
    private JavaCG2ParseSwitchBuilder putStringAny(ElConfigInterface elConfig, ElStringAnyFunctionEnum stringAnyFunctionEnum, ElAllowedVariableInterface elVariable,
                                                   String... args) {
        elSwitchConfigHolder.putStringAny(elConfig, stringAnyFunctionEnum, elVariable, args);
        return this;
    }
}
