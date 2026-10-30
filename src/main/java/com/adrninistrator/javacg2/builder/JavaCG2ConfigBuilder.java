package com.adrninistrator.javacg2.builder;

import com.adrninistrator.javacg2.common.enums.JavaCG2CalleeRawActualEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: 主要配置参数对应的Builder，通过代码以链式方式设置主要配置参数
 * 未设置参数的配置参数使用默认值
 * 当前类的实例只能构建一个JavaCG2ConfigureWrapper对象，假如需要构建多个，请创建当前类的多个实例
 */
public class JavaCG2ConfigBuilder {

    private final JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();

    public static JavaCG2ConfigBuilder builder() {
        return new JavaCG2ConfigBuilder();
    }

    private JavaCG2ConfigBuilder() {
    }

    /**
     * 设置处理方法调用时是否解析被调用对象和参数可能的类型与值
     *
     * @param value true: 解析 false: 不解析
     * @return
     */
    public JavaCG2ConfigBuilder setParseMethodCallTypeValue(boolean value) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_METHOD_CALL_TYPE_VALUE, String.valueOf(value));
        return this;
    }

    /**
     * 设置处理类的方法前是否需要先解析构造函数以获取非静态字段可能的类型，仅当parse.method.call.type.value参数为true时才可以生效
     *
     * @param value true: 解析 false: 不解析
     * @return
     */
    public JavaCG2ConfigBuilder setFirstParseInitMethodType(boolean value) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_FIRST_PARSE_INIT_METHOD_TYPE, String.valueOf(value));
        return this;
    }

    /**
     * 设置是否需要分析dto的字段之间通过get/set方法的关联关系，仅当parse.method.call.type.value参数为true时才可以生效
     *
     * @param value true: 分析 false: 不分析
     * @return
     */
    public JavaCG2ConfigBuilder setAnalyseFieldRelationship(boolean value) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_ANALYSE_FIELD_RELATIONSHIP, String.valueOf(value));
        return this;
    }

    /**
     * 设置解析方法出现异常时，是否要继续
     *
     * @param value true: 继续执行 false: 抛出异常终止处理
     * @return
     */
    public JavaCG2ConfigBuilder setContinueWhenError(boolean value) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_CONTINUE_WHEN_ERROR, String.valueOf(value));
        return this;
    }

    /**
     * 设置是否在输出目录生成记录方法分析耗时的文件
     *
     * @param value true: 生成 false: 不生成
     * @return
     */
    public JavaCG2ConfigBuilder setLogMethodSpendTime(boolean value) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_LOG_METHOD_SPEND_TIME, String.valueOf(value));
        return this;
    }

    /**
     * 设置生成文件的根目录，假如未设置，则生成到需要解析的jar文件所在目录
     * 假如需要解析jmod文件，则当前参数必须设置
     *
     * @param path 生成文件的根目录路径，分隔符支持使用/或\，末尾是否为分隔符不影响
     * @return
     */
    public JavaCG2ConfigBuilder setOutputRootPath(String path) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, path);
        return this;
    }

    /**
     * 设置生成文件后缀名
     *
     * @param ext 生成文件后缀名，需要以“.”开头
     * @return
     */
    public JavaCG2ConfigBuilder setOutputFileExt(String ext) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_FILE_EXT, ext);
        return this;
    }

    /**
     * 设置是否开启表达式执行调试模式，若开启会在应用日志中输出表达式执行时的详细信息
     *
     * @param value true: 开启 false: 不开启
     * @return
     */
    public JavaCG2ConfigBuilder setElDebugMode(boolean value) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_EL_DEBUG_MODE, String.valueOf(value));
        return this;
    }

    /**
     * 设置解析方法调用时，通过new创建的被调用类型使用原始类型还是实际类型的开关
     *
     * @param value 原始类型/实际类型开关
     * @return
     */
    public JavaCG2ConfigBuilder setHandleCalleeNewRawActual(JavaCG2CalleeRawActualEnum value) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_HANDLE_CALLEE_NEW_RAW_ACTUAL, value.getType());
        return this;
    }

    /**
     * 设置解析方法调用时，被调用对象为Spring Bean时（支持字段注入），类型使用原始类型还是实际类型的开关
     *
     * @param value 原始类型/实际类型开关
     * @return
     */
    public JavaCG2ConfigBuilder setHandleCalleeSpringBeanRawActual(JavaCG2CalleeRawActualEnum value) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_HANDLE_CALLEE_SPRING_BEAN_RAW_ACTUAL, value.getType());
        return this;
    }

    /**
     * 设置在合并需要解析的jar文件时，是否合并出一个单独的fat jar。仅包含.class文件，且所有的jar文件都合并到从根目录开始
     *
     * @param value true: 合并 false: 不合并
     * @return
     */
    public JavaCG2ConfigBuilder setMergeSeparateFatJar(boolean value) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_MERGE_SEPARATE_FAT_JAR, String.valueOf(value));
        return this;
    }

    /**
     * 设置是否使用Jar兼容性检查模式，仅解析类、方法、字段、类的注解等基础信息
     *
     * @param value true: 使用 false: 不使用
     * @return
     */
    public JavaCG2ConfigBuilder setParseJarCompatibilityMode(boolean value) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_JAR_COMPATIBILITY_MODE, String.valueOf(value));
        return this;
    }

    /**
     * 设置是否仅解析类及类的注解信息，比parse.jar.compatibility.mode优先级高
     *
     * @param value true: 仅解析类及类的注解信息 false: 解析全部信息
     * @return
     */
    public JavaCG2ConfigBuilder setParseOnlyClassMode(boolean value) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_ONLY_CLASS_MODE, String.valueOf(value));
        return this;
    }

    /**
     * 设置是否使用内置的Spring XML文件中定义的Bean解析类
     * 通过ExtensionsManager的setSpringXmlBeanParser方法指定自定义实现时，自定义实现优先于内置实现
     *
     * @param value true: 使用内置实现 false: 不使用内置实现
     * @return
     */
    public JavaCG2ConfigBuilder setUseBuiltInSpringXmlBeanParser(boolean value) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_USE_BUILT_IN_SPRING_XML_BEAN_PARSER, String.valueOf(value));
        return this;
    }

    /**
     * 设置用于解析.jmod文件的jmod程序完整路径
     * 假如指定的.jmod文件在JDK的jmods目录中，可找到bin目录的jmod程序，则当前参数可不设置
     *
     * @param path jmod程序完整路径
     * @return
     */
    public JavaCG2ConfigBuilder setJmodProgramPath(String path) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_JMOD_PROGRAM_PATH, path);
        return this;
    }

    /**
     * 设置解析的jar文件在运行时使用的JDK主版本号，例如8、11、17、21等
     *
     * @param majorVersion JDK主版本号
     * @return
     */
    public JavaCG2ConfigBuilder setJdkRuntimeMajorVersion(int majorVersion) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_JDK_RUNTIME_MAJOR_VERSION, String.valueOf(majorVersion));
        return this;
    }

    /**
     * 设置EL表达式忽略数据文件的最大行数
     *
     * @param maxLineNum 最大行数
     * @return
     */
    public JavaCG2ConfigBuilder setElIgnoreDataMaxLineNum(int maxLineNum) {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_EL_IGNORE_DATA_MAX_LINE_NUM, String.valueOf(maxLineNum));
        return this;
    }

    /**
     * 生成主要配置参数对应的JavaCG2ConfigureWrapper对象
     * 未通过当前类设置的主要配置参数使用默认值
     *
     * @return
     */
    public JavaCG2ConfigureWrapper build() {
        return javaCG2ConfigureWrapper;
    }
}
