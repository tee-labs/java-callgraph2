package com.adrninistrator.javacg2.builder;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.el.enums.ElStringAnyFunctionEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElAllowedVariableEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.el.enums.interfaces.ElAllowedVariableInterface;
import com.adrninistrator.javacg2.el.enums.interfaces.ElConfigInterface;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

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

    private static final Logger logger = LoggerFactory.getLogger(JavaCG2MergeFileSwitchBuilder.class);

    // 待应用的表达式配置，key为表达式配置枚举，同一个枚举以后设置的为准
    private final Map<ElConfigInterface, PendingElText> pendingElTextMap = new LinkedHashMap<>();

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
        if (elText == null) {
            throw new JavaCG2ConfigException("表达式文本不允许传入null " + JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_JAR_WAR_BY_CLASS_DIR_PREFIX.getKey());
        }
        return putPendingElText(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_JAR_WAR_BY_CLASS_DIR_PREFIX, PendingElText.genRaw(JavaCG2ElConfigEnum
                .ECE_MERGE_FILE_IGNORE_JAR_WAR_BY_CLASS_DIR_PREFIX, elText));
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
        for (Map.Entry<ElConfigInterface, PendingElText> entry : pendingElTextMap.entrySet()) {
            entry.getValue().applyTo(javaCG2ConfigureWrapper);
        }
    }

    // 记录使用字符串比较函数生成的表达式配置，字符串参数自动以单引号包裹
    private JavaCG2MergeFileSwitchBuilder putStringAny(ElConfigInterface elConfig, ElStringAnyFunctionEnum stringAnyFunctionEnum, ElAllowedVariableInterface elVariable,
                                                       String... args) {
        // 检查当前表达式是否允许使用该变量
        checkElVariableAllowed(elConfig, elVariable);
        if (ArrayUtils.isEmpty(args)) {
            throw new JavaCG2ConfigException("不允许传入null或空数组 " + elConfig.getKey());
        }
        String[] quotedArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.isBlank(arg)) {
                throw new JavaCG2ConfigException("参数不允许为空 " + elConfig.getKey());
            }
            if (arg.contains("'")) {
                throw new JavaCG2ConfigException("参数不允许包含单引号 " + elConfig.getKey() + " " + arg);
            }
            // 语义化API自动为字符串参数添加单引号，保证生成的表达式是合法的Aviator表达式
            quotedArgs[i] = "'" + arg + "'";
        }
        return putPendingElText(elConfig, PendingElText.genStringAny(elConfig, stringAnyFunctionEnum, elVariable, quotedArgs));
    }

    // 记录待应用的表达式配置，同一个表达式配置枚举以后设置的为准
    private JavaCG2MergeFileSwitchBuilder putPendingElText(ElConfigInterface elConfig, PendingElText pendingElText) {
        logger.info("设置合并文件开关表达式 {} {}", elConfig.getKey(), pendingElText.genPrintInfo());
        pendingElTextMap.put(elConfig, pendingElText);
        return this;
    }

    // 检查当前表达式配置是否允许使用该变量
    private void checkElVariableAllowed(ElConfigInterface elConfig, ElAllowedVariableInterface elVariable) {
        for (ElAllowedVariableInterface allowedVariable : elConfig.getElAllowedVariableEnums()) {
            if (allowedVariable.getVariableName().equals(elVariable.getVariableName())) {
                return;
            }
        }
        throw new JavaCG2ConfigException("当前表达式不允许使用该变量 " + elConfig.getKey() + " " + elVariable.getVariableName());
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

    // 待应用的表达式文本
    private static class PendingElText {

        // 对应的表达式配置枚举
        private final ElConfigInterface elConfig;

        // 字符串比较方式枚举，为null时使用原始表达式文本
        private final ElStringAnyFunctionEnum stringAnyFunctionEnum;

        private final ElAllowedVariableInterface elVariable;

        // 以单引号包裹的字符串参数
        private final String[] stringAnyArgs;

        // 原始表达式文本
        private final String rawElText;

        private PendingElText(ElConfigInterface elConfig, ElStringAnyFunctionEnum stringAnyFunctionEnum, ElAllowedVariableInterface elVariable,
                              String[] stringAnyArgs, String rawElText) {
            this.elConfig = elConfig;
            this.stringAnyFunctionEnum = stringAnyFunctionEnum;
            this.elVariable = elVariable;
            this.stringAnyArgs = stringAnyArgs;
            this.rawElText = rawElText;
        }

        private static PendingElText genStringAny(ElConfigInterface elConfig, ElStringAnyFunctionEnum stringAnyFunctionEnum, ElAllowedVariableInterface elVariable,
                                                  String... stringAnyArgs) {
            return new PendingElText(elConfig, stringAnyFunctionEnum, elVariable, stringAnyArgs, null);
        }

        private static PendingElText genRaw(ElConfigInterface elConfig, String rawElText) {
            return new PendingElText(elConfig, null, null, null, rawElText);
        }

        private void applyTo(JavaCG2ConfigureWrapper javaCG2ConfigureWrapper) {
            if (stringAnyFunctionEnum != null) {
                /*
                    不使用BaseConfigureWrapper.setElConfigStringAny方法：其生成的表达式文本以分号结尾，
                    Aviator执行带分号的表达式时返回null而非Boolean，会导致表达式检查失败
                 */
                javaCG2ConfigureWrapper.setElConfigText(elConfig, genElText());
            } else {
                javaCG2ConfigureWrapper.setElConfigText(elConfig, rawElText);
            }
        }

        // 生成使用字符串比较函数的表达式文本，字符串参数已带单引号，不以分号结尾
        private String genElText() {
            return stringAnyFunctionEnum.getName() + "(" + elVariable.getVariableName() + "," + StringUtils.join(stringAnyArgs, ",") + ")";
        }

        private String genPrintInfo() {
            if (stringAnyFunctionEnum != null) {
                return stringAnyFunctionEnum.getName() + " " + elVariable.getVariableName() + " " + StringUtils.join(stringAnyArgs, ",");
            }
            return rawElText;
        }
    }
}
