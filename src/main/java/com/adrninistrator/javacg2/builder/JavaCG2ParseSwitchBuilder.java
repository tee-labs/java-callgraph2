package com.adrninistrator.javacg2.builder;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.el.enums.interfaces.ElConfigInterface;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: 解析类、方法与方法调用相关开关对应的Builder，通过代码以链式方式指定解析类、方法、方法调用时使用的忽略（黑名单）与包含（白名单）表达式
 * 每个表达式配置枚举对应一个设置方法，通过原始Aviator表达式文本指定（字符串参数需要以单引号包裹，不以分号结尾），忽略表达式执行结果为true时跳过解析对应的类、方法或方法调用，包含表达式执行结果为true时解析，为false时跳过
 * 包含（白名单）与忽略（黑名单）表达式可同时生效，两道判断串联：先判断包含表达式，未命中时直接跳过且不再判断忽略表达式；命中后再判断忽略表达式，即跳过 =（包含表达式已设置且未命中）或（忽略表达式命中）
 * 包含（白名单）表达式未设置（空白）时不生效，即全部解析
 * 同一个表达式配置枚举对应的方法多次调用时，以后调用的为准（表达式文本整体替换），多种判断条件需要在同一个表达式中通过||等逻辑运算符组合
 * 未通过当前类设置的表达式配置使用枚举中定义的默认值（parse_ignore_method.av枚举中默认表达式忽略hashCode与equals方法，其余默认为空白，即不生效）
 * 设置跳过解析方法的表达式时会整体替换默认表达式，假如还需要忽略默认的hashCode与equals(java.lang.Object)方法，需要在表达式中包含对应的判断条件
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
     * 通过原始表达式文本设置跳过解析类的表达式
     * 允许使用的变量为JavaCG2ElAllowedVariableEnum中以EAVE_PARSE_开头且说明为类相关的枚举常量：class_name、package_name、simple_class_name
     *
     * @param elText 原始表达式文本，如"string.equalsAny(class_name, 'a.b.Class1')"、"string.startsWithAny(package_name, 'a.b.') || simple_class_name == 'Class1'"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2ParseSwitchBuilder setIgnoreClassExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_CLASS, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置仅解析类的表达式（白名单）
     * 表达式执行结果为true时解析，为false时跳过，未设置（空白）时全部解析
     * 与setIgnoreClassExpression同时生效，先判断包含（白名单）表达式，后判断忽略（黑名单）表达式；包含表达式未命中时跳过解析，且不再判断忽略表达式
     * 允许使用的变量为JavaCG2ElAllowedVariableEnum中以EAVE_PARSE_开头且说明为类相关的枚举常量：class_name、package_name、simple_class_name
     *
     * @param elText 原始表达式文本，如"string.startsWithAny(package_name, 'com.business.')"、"string.equalsAny(class_name, 'a.b.Class1')"，null不允许，空白表示不生效（全部解析）
     * @return
     */
    public JavaCG2ParseSwitchBuilder setIncludeClassExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_CLASS, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置跳过解析方法的表达式
     * 允许使用的变量为JavaCG2ElAllowedVariableEnum中以EAVE_PARSE_开头的枚举常量：class_name、package_name、simple_class_name、method_name、method_arg_num、full_method
     *
     * @param elText 原始表达式文本，如"string.endsWithAny(full_method, 'hashCode()', 'equals(java.lang.Object)')"、"method_arg_num != 4"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2ParseSwitchBuilder setIgnoreMethodExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置仅解析方法的表达式（白名单）
     * 表达式执行结果为true时解析，为false时跳过，未设置（空白）时全部解析
     * 与setIgnoreMethodExpression同时生效，先判断包含（白名单）表达式，后判断忽略（黑名单）表达式；包含表达式未命中时跳过解析，且不再判断忽略表达式
     * 允许使用的变量为JavaCG2ElAllowedVariableEnum中以EAVE_PARSE_开头的枚举常量：class_name、package_name、simple_class_name、method_name、method_arg_num、full_method
     *
     * @param elText 原始表达式文本，如"string.startsWithAny(class_name, 'com.business.')"、"string.equalsAny(method_name, 'method1', 'method2')"，null不允许，空白表示不生效（全部解析）
     * @return
     */
    public JavaCG2ParseSwitchBuilder setIncludeMethodExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_METHOD, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置跳过解析方法调用的表达式
     * 允许使用的变量为CommonElAllowedVariableEnum中的枚举常量：method_call_type，以及以er_开头（调用方）或ee_开头（被调用方）的变量
     *
     * @param elText 原始表达式文本，如"string.equalsAny(method_call_type, 'VIR', 'INT')"、"string.containsIC(ee_class_name, 'test')"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2ParseSwitchBuilder setIgnoreMethodCallExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置仅解析方法调用的表达式（白名单）
     * 表达式执行结果为true时解析，为false时跳过，未设置（空白）时全部解析
     * 与setIgnoreMethodCallExpression同时生效，先判断包含（白名单）表达式，后判断忽略（黑名单）表达式；包含表达式未命中时跳过解析，且不再判断忽略表达式，仅当白名单命中后才会继续判断parse_ignore_method_call.av
     * 允许使用的变量为CommonElAllowedVariableEnum中的枚举常量：method_call_type，以及以er_开头（调用方）或ee_开头（被调用方）的变量
     *
     * @param elText 原始表达式文本，如"string.startsWithAny(er_package_name, 'com.business.')"、"string.containsIC(ee_class_name, 'test')"，null不允许，空白表示不生效（全部解析）
     * @return
     */
    public JavaCG2ParseSwitchBuilder setIncludeMethodCallExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_METHOD_CALL, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置跳过解析方法调用被调用对象和参数可能的类型与值的表达式
     * 允许使用的变量为CommonElAllowedVariableEnum中以er_开头（调用方）的变量
     * 仅当parse.method.call.type.value参数为true时当前表达式才会生效
     *
     * @param elText 原始表达式文本，如"string.equalsAny(er_class_name, 'a.b.Class1')"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2ParseSwitchBuilder setIgnoreTypeValueCallerExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL_TYPE_VALUE_CALLER, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置解析方法调用被调用对象和参数可能的类型与值时仅处理哪些方法（白名单）的表达式
     * 表达式执行结果为true时处理，为false时跳过，未设置（空白）时全部处理
     * 与setIgnoreTypeValueCallerExpression同时生效，先判断包含（白名单）表达式，后判断忽略（黑名单）表达式；包含表达式未命中时跳过解析，且不再判断忽略表达式
     * 允许使用的变量为CommonElAllowedVariableEnum中以er_开头（调用方）的变量
     * 仅当parse.method.call.type.value参数为true时当前表达式才会生效
     *
     * @param elText 原始表达式文本，如"string.startsWithAny(er_package_name, 'com.business.')"，null不允许，空白表示不生效（全部解析）
     * @return
     */
    public JavaCG2ParseSwitchBuilder setIncludeTypeValueCallerExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_METHOD_CALL_TYPE_VALUE_CALLER, elText);
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
}
