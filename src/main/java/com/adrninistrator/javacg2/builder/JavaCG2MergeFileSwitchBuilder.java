package com.adrninistrator.javacg2.builder;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.el.enums.ElStringAnyFunctionEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElAllowedVariableEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.el.enums.interfaces.ElAllowedVariableInterface;
import com.adrninistrator.javacg2.el.enums.interfaces.ElConfigInterface;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: _javacg2_merge_file_switch/目录下表达式配置文件对应的Builder，通过代码以链式方式指定合并jar、war文件时使用的忽略表达式，不依赖配置文件
 * 语义化方法内部生成合法的Aviator表达式文本（字符串参数自动以单引号包裹，不以分号结尾），表达式执行结果为true时跳过合并（及解析）对应的文件
 * 同一个表达式配置枚举对应的方法多次调用时，以后调用的为准（表达式文本整体替换）
 * 未通过当前类设置的表达式配置使用jar包中内置配置文件的默认内容（默认为空白，即不生效）
 * 当前类的实例只能通过applyTo方法应用到一个JavaCG2ConfigureWrapper对象，假如需要应用多个，请创建当前类的多个实例
 */
public class JavaCG2MergeFileSwitchBuilder {

    // 表达式开关配置的公共逻辑
    private final ElSwitchConfigHolder elSwitchConfigHolder = new ElSwitchConfigHolder();

    public static JavaCG2MergeFileSwitchBuilder builder() {
        return new JavaCG2MergeFileSwitchBuilder();
    }

    private JavaCG2MergeFileSwitchBuilder() {
    }

    /**
     * 按文件名忽略目录中的jar文件，使用string.equalsAny函数判断文件名是否与指定的一个或多个文件名完全相等
     *
     * @param fileNames 一个或多个文件名，如"test.jar"
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder ignoreJarInDirByFileName(String... fileNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_JAR_IN_DIR, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_MF_FILE_NAME,
                fileNames);
    }

    /**
     * 按文件名忽略目录中的war文件，使用string.equalsAny函数判断文件名是否与指定的一个或多个文件名完全相等
     *
     * @param fileNames 一个或多个文件名，如"test.war"
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder ignoreWarInDirByFileName(String... fileNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_WAR_IN_DIR, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_MF_FILE_NAME,
                fileNames);
    }

    /**
     * 按文件名忽略目录中的class文件，使用string.equalsAny函数判断文件名是否与指定的一个或多个文件名完全相等
     *
     * @param fileNames 一个或多个文件名，如"test.class"
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder ignoreClassInDirByFileName(String... fileNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_CLASS_IN_DIR, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_MF_FILE_NAME,
                fileNames);
    }

    /**
     * 按文件名忽略jar、war文件中的jar文件，使用string.equalsAny函数判断文件名是否与指定的一个或多个文件名完全相等
     *
     * @param fileNames 一个或多个文件名，如"inner.jar"
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder ignoreJarInJarWarByFileName(String... fileNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_JAR_IN_JAR_WAR, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_MF_FILE_NAME,
                fileNames);
    }

    /**
     * 按文件名忽略jar、war文件中的class文件，使用string.equalsAny函数判断文件名是否与指定的一个或多个文件名完全相等
     *
     * @param fileNames 一个或多个文件名，如"test.class"
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder ignoreClassInJarWarByFileName(String... fileNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_CLASS_IN_JAR_WAR, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_MF_FILE_NAME,
                fileNames);
    }

    /**
     * 按文件路径忽略目录中的jar文件，使用string.containsAny函数判断文件路径是否包含指定的一个或多个关键字
     *
     * @param pathKeywords 一个或多个文件路径关键字，如"/lib/bcel-"
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder ignoreJarInDirByFilePath(String... pathKeywords) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_JAR_IN_DIR, ElStringAnyFunctionEnum.CONTAINS_ANY,
                JavaCG2ElAllowedVariableEnum.EAVE_MF_ABSOLUTE_FILE_PATH_IN_DIR, pathKeywords);
    }

    /**
     * 按文件路径忽略目录中的war文件，使用string.containsAny函数判断文件路径是否包含指定的一个或多个关键字
     *
     * @param pathKeywords 一个或多个文件路径关键字
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder ignoreWarInDirByFilePath(String... pathKeywords) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_WAR_IN_DIR, ElStringAnyFunctionEnum.CONTAINS_ANY,
                JavaCG2ElAllowedVariableEnum.EAVE_MF_ABSOLUTE_FILE_PATH_IN_DIR, pathKeywords);
    }

    /**
     * 按文件路径忽略目录中的class文件，使用string.containsAny函数判断文件路径是否包含指定的一个或多个关键字
     *
     * @param pathKeywords 一个或多个文件路径关键字
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder ignoreClassInDirByFilePath(String... pathKeywords) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_CLASS_IN_DIR, ElStringAnyFunctionEnum.CONTAINS_ANY,
                JavaCG2ElAllowedVariableEnum.EAVE_MF_ABSOLUTE_FILE_PATH_IN_DIR, pathKeywords);
    }

    /**
     * 按文件路径忽略jar、war文件中的jar文件，使用string.containsAny函数判断文件相对路径是否包含指定的一个或多个关键字
     *
     * @param pathKeywords 一个或多个文件相对路径关键字，如"lib/"
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder ignoreJarInJarWarByFilePath(String... pathKeywords) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_JAR_IN_JAR_WAR, ElStringAnyFunctionEnum.CONTAINS_ANY,
                JavaCG2ElAllowedVariableEnum.EAVE_MF_FILE_PATH_IN_JAR_WAR, pathKeywords);
    }

    /**
     * 按文件路径忽略jar、war文件中的class文件，使用string.containsAny函数判断class文件相对路径是否包含指定的一个或多个关键字
     *
     * @param pathKeywords 一个或多个class文件相对路径关键字，如"BOOT-INF/"
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder ignoreClassInJarWarByFilePath(String... pathKeywords) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_CLASS_IN_JAR_WAR, ElStringAnyFunctionEnum.CONTAINS_ANY,
                JavaCG2ElAllowedVariableEnum.EAVE_MF_CLASS_FILE_PATH_IN_JAR_WAR, pathKeywords);
    }

    /**
     * 按文件后缀忽略目录中的非jar、war、class文件，使用string.equalsAny函数判断文件后缀是否与指定的一个或多个文件后缀完全相等
     *
     * @param fileExts 一个或多个文件后缀，需要以“.”开头，如".xml"
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder ignoreOtherInDirByFileExt(String... fileExts) {
        checkFileExt(fileExts);
        return putStringAny(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_OTHER_IN_DIR, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_MF_OTHER_FILE_EXT,
                fileExts);
    }

    /**
     * 按文件后缀忽略jar、war文件中的非jar、war、class文件，使用string.equalsAny函数判断文件后缀是否与指定的一个或多个文件后缀完全相等
     *
     * @param fileExts 一个或多个文件后缀，需要以“.”开头，如".xml"
     * @return
     */
    public JavaCG2MergeFileSwitchBuilder ignoreOtherInJarWarByFileExt(String... fileExts) {
        checkFileExt(fileExts);
        return putStringAny(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_OTHER_IN_JAR_WAR, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_MF_OTHER_FILE_EXT,
                fileExts);
    }

    /**
     * 通过原始表达式文本设置“通过class文件对应指定层级的目录路径判断是否跳过合并当前jar、war文件”的表达式
     * 该表达式使用的变量class_dir_prefix_level_N为Set类型，难以提供语义化方法，因此仅提供原始表达式入口
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

    // 记录使用字符串比较函数生成的表达式配置，字符串参数自动以单引号包裹
    private JavaCG2MergeFileSwitchBuilder putStringAny(ElConfigInterface elConfig, ElStringAnyFunctionEnum stringAnyFunctionEnum, ElAllowedVariableInterface elVariable,
                                                       String... args) {
        elSwitchConfigHolder.putStringAny(elConfig, stringAnyFunctionEnum, elVariable, args);
        return this;
    }

    // 检查文件后缀格式
    private void checkFileExt(String... fileExts) {
        for (String fileExt : fileExts) {
            if (StringUtils.isBlank(fileExt)) {
                throw new JavaCG2ConfigException("文件后缀不允许为空");
            }
            if (!StringUtils.startsWith(fileExt, ".")) {
                throw new JavaCG2ConfigException("文件后缀需要以“.”开头 " + fileExt);
            }
        }
    }
}
