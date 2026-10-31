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
 * @description: 合并jar、war文件、目录相关开关对应的Builder，通过代码以链式方式指定合并jar、war文件时使用的忽略表达式
 * 每个表达式配置枚举对应一个设置方法，通过原始Aviator表达式文本指定（字符串参数需要以单引号包裹，不以分号结尾），表达式执行结果为true时跳过合并（及解析）对应的文件
 * 同一个表达式配置枚举对应的方法多次调用时，以后调用的为准（表达式文本整体替换），多种判断条件需要在同一个表达式中通过||等逻辑运算符组合
 * 未通过当前类设置的表达式配置使用枚举中定义的默认值（默认为空白，即不生效）
 * 当前类的实例只能通过applyTo方法应用到一个JavaCG2ConfigureWrapper对象，假如需要应用多个，请创建当前类的多个实例
 */
@Slf4j
public class JavaCG2MergeFileSwitchBuilder {

    // 表达式开关配置的公共逻辑
    private final ElSwitchConfigHolder elSwitchConfigHolder = new ElSwitchConfigHolder();

    public static JavaCG2MergeFileSwitchBuilder builder() {
        return new JavaCG2MergeFileSwitchBuilder();
    }

    private JavaCG2MergeFileSwitchBuilder() {
    }

    /**
     * 通过原始表达式文本设置跳过合并目录中jar文件的表达式
     * 允许使用的变量为JavaCG2ElAllowedVariableEnum中以EAVE_MF_开头且说明为目录中文件相关的枚举常量：file_path、file_dir_path、file_name
     *
     * @param elText 原始表达式文本，如"string.equalsAny(file_name, 'test.jar')"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder setIgnoreJarInDirExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_JAR_IN_DIR, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置跳过合并目录中war文件的表达式
     * 允许使用的变量为JavaCG2ElAllowedVariableEnum中以EAVE_MF_开头且说明为目录中文件相关的枚举常量：file_path、file_dir_path、file_name
     *
     * @param elText 原始表达式文本，如"string.equalsAny(file_name, 'test.war')"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder setIgnoreWarInDirExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_WAR_IN_DIR, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置跳过合并目录中class文件的表达式
     * 允许使用的变量为JavaCG2ElAllowedVariableEnum中以EAVE_MF_开头且说明为目录中文件相关的枚举常量：file_path、file_dir_path、file_name
     *
     * @param elText 原始表达式文本，如"string.equalsAny(file_name, 'Test1.class')"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder setIgnoreClassInDirExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_CLASS_IN_DIR, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置跳过合并目录中非jar、war、class文件的表达式
     * 允许使用的变量为JavaCG2ElAllowedVariableEnum中以EAVE_MF_开头且说明为目录中文件相关的枚举常量：file_path、file_dir_path、file_name、file_ext
     *
     * @param elText 原始表达式文本，如"string.equalsAny(file_ext, '.xml', '.properties')"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder setIgnoreOtherInDirExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_OTHER_IN_DIR, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置跳过合并jar、war文件中jar文件的表达式
     * 允许使用的变量为JavaCG2ElAllowedVariableEnum中以EAVE_MF_开头且说明为jar、war文件中文件相关的枚举常量：file_path、file_dir_path、file_name
     *
     * @param elText 原始表达式文本，如"string.startsWithAny(file_path, 'lib/')"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder setIgnoreJarInJarWarExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_JAR_IN_JAR_WAR, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置跳过合并jar、war文件中class文件的表达式
     * 允许使用的变量为JavaCG2ElAllowedVariableEnum中以EAVE_MF_开头且说明为jar、war文件中文件相关的枚举常量：file_path、file_dir_path、file_name、class_file_path
     *
     * @param elText 原始表达式文本，如"string.startsWithAny(class_file_path, 'BOOT-INF/')"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder setIgnoreClassInJarWarExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_CLASS_IN_JAR_WAR, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置跳过合并jar、war文件中非jar、war、class文件的表达式
     * 允许使用的变量为JavaCG2ElAllowedVariableEnum中以EAVE_MF_开头且说明为jar、war文件中文件相关的枚举常量：file_path、file_dir_path、file_name、file_ext
     *
     * @param elText 原始表达式文本，如"string.equalsAny(file_ext, '.xml')"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder setIgnoreOtherInJarWarExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_OTHER_IN_JAR_WAR, elText);
        return this;
    }

    /**
     * 通过原始表达式文本设置“通过class文件对应指定层级的目录路径判断是否跳过合并当前jar、war文件”的表达式
     * 允许使用的变量为class_dir_prefix_level_N，N为数字，代表class文件路径层级，变量类型为Set，需要通过include方法判断集合中是否包含指定元素
     *
     * @param elText 原始表达式文本，如"!include(class_dir_prefix_level_2, 'com/adrninistrator')"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder setIgnoreJarWarByClassDirPrefixExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_JAR_WAR_BY_CLASS_DIR_PREFIX, elText);
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
