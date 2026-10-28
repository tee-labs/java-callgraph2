package com.adrninistrator.javacg2.builder;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.el.enums.interfaces.ElConfigInterface;
import com.adrninistrator.javacg2.el.util.JavaCG2ElUtil;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: JavaCG2Entry对应的Builder，通过代码指定配置参数生成入口类实例，不依赖磁盘上的配置文件
 * 使用示例：
 *     JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
 *             .setConfig(JavaCG2ConfigBuilder.builder()
 *                     .setOutputRootPath("D:/output")
 *                     .setParseMethodCallTypeValue(true))
 *             .setFrEqConversionMethod(JavaCG2FrEqConversionMethodBuilder.builder()
 *                     .addFrEqConversionMethod("test.callgraph.dto.Dto1", "copy", 1))
 *             .setMergeFileSwitch(JavaCG2MergeFileSwitchBuilder.builder()
 *                     .ignoreJarInDirByFileName("not_needed.jar"))
 *             .setParseSwitch(JavaCG2ParseSwitchBuilder.builder()
 *                     .ignoreClassByPackageName("com.business.not_needed"))
 *             .addJarOrDir("D:/tmp/test.jar")
 *             .build();
 *     boolean success = javaCG2Entry.run();
 * config与jar_dir对应的Builder均可不设置，config未设置时主要配置参数使用默认值
 * 未通过Builder设置的其他类型配置参数（如fr_eq_conversion_method.properties）使用jar包中内置配置文件的默认内容
 * 未通过Builder设置的表达式配置使用jar包中内置配置文件的默认内容（parse_ignore_method.av内置默认表达式忽略hashCode与equals方法，其余内置默认为空白，即不生效）
 * 当前类的实例只能通过build方法生成一个JavaCG2Entry对象，假如需要生成多个，请创建当前类的多个实例
 * 生成的JavaCG2Entry对象只能执行一次run方法，假如需要再次执行，请通过当前类的新的实例生成新的JavaCG2Entry对象
 */
public class JavaCG2EntryBuilder {

    private static final Logger logger = LoggerFactory.getLogger(JavaCG2EntryBuilder.class);

    private JavaCG2ConfigBuilder javaCG2ConfigBuilder;

    private JavaCG2JarDirBuilder javaCG2JarDirBuilder;

    private JavaCG2FrEqConversionMethodBuilder javaCG2FrEqConversionMethodBuilder;

    private JavaCG2MergeFileSwitchBuilder javaCG2MergeFileSwitchBuilder;

    private JavaCG2ParseSwitchBuilder javaCG2ParseSwitchBuilder;

    public static JavaCG2EntryBuilder builder() {
        return new JavaCG2EntryBuilder();
    }

    private JavaCG2EntryBuilder() {
    }

    /**
     * 设置config.properties对应的Builder，未设置时主要配置参数使用默认值
     *
     * @param javaCG2ConfigBuilder
     * @return
     */
    public JavaCG2EntryBuilder setConfig(JavaCG2ConfigBuilder javaCG2ConfigBuilder) {
        if (javaCG2ConfigBuilder == null) {
            throw new JavaCG2ConfigException("不允许传入null " + JavaCG2ConfigBuilder.class.getSimpleName());
        }
        this.javaCG2ConfigBuilder = javaCG2ConfigBuilder;
        return this;
    }

    /**
     * 设置jar_dir.properties对应的Builder，与addJarOrDir方法的效果叠加
     *
     * @param javaCG2JarDirBuilder
     * @return
     */
    public JavaCG2EntryBuilder setJarDir(JavaCG2JarDirBuilder javaCG2JarDirBuilder) {
        if (javaCG2JarDirBuilder == null) {
            throw new JavaCG2ConfigException("不允许传入null " + JavaCG2JarDirBuilder.class.getSimpleName());
        }
        this.javaCG2JarDirBuilder = javaCG2JarDirBuilder;
        return this;
    }

    /**
     * 添加需要解析的jar、war、jmod文件路径，或保存class、jar、war、jmod文件的目录路径，与setJarDir方法的效果叠加
     *
     * @param jarOrDirPaths 一个或多个文件/目录路径，路径中的分隔符支持使用/或\，目录最后指定或不指定分隔符均可
     * @return
     */
    public JavaCG2EntryBuilder addJarOrDir(String... jarOrDirPaths) {
        genJarDirBuilder().addJarOrDir(jarOrDirPaths);
        return this;
    }

    /**
     * 添加需要解析的jar、war、jmod文件路径，或保存class、jar、war、jmod文件的目录路径，与setJarDir方法的效果叠加
     *
     * @param jarOrDirPaths 文件/目录路径集合
     * @return
     */
    public JavaCG2EntryBuilder addJarOrDir(Collection<String> jarOrDirPaths) {
        genJarDirBuilder().addJarOrDir(jarOrDirPaths);
        return this;
    }

    /**
     * 设置fr_eq_conversion_method.properties对应的Builder，未设置时等值转换方法使用jar包中内置配置文件的默认内容
     *
     * @param javaCG2FrEqConversionMethodBuilder
     * @return
     */
    public JavaCG2EntryBuilder setFrEqConversionMethod(JavaCG2FrEqConversionMethodBuilder javaCG2FrEqConversionMethodBuilder) {
        if (javaCG2FrEqConversionMethodBuilder == null) {
            throw new JavaCG2ConfigException("不允许传入null " + JavaCG2FrEqConversionMethodBuilder.class.getSimpleName());
        }
        this.javaCG2FrEqConversionMethodBuilder = javaCG2FrEqConversionMethodBuilder;
        return this;
    }

    /**
     * 设置_javacg2_merge_file_switch/目录下表达式配置文件对应的Builder，未设置时表达式使用jar包中内置配置文件的默认内容（默认为空白，即不生效）
     *
     * @param javaCG2MergeFileSwitchBuilder
     * @return
     */
    public JavaCG2EntryBuilder setMergeFileSwitch(JavaCG2MergeFileSwitchBuilder javaCG2MergeFileSwitchBuilder) {
        if (javaCG2MergeFileSwitchBuilder == null) {
            throw new JavaCG2ConfigException("不允许传入null " + JavaCG2MergeFileSwitchBuilder.class.getSimpleName());
        }
        this.javaCG2MergeFileSwitchBuilder = javaCG2MergeFileSwitchBuilder;
        return this;
    }

    /**
     * 设置_javacg2_parse_class_method_switch/与_javacg2_parse_method_call_switch/目录下表达式配置文件对应的Builder，未设置时表达式使用jar包中内置配置文件的默认内容
     * （parse_ignore_method.av内置默认表达式忽略hashCode与equals方法，其余内置默认为空白，即不生效）
     *
     * @param javaCG2ParseSwitchBuilder
     * @return
     */
    public JavaCG2EntryBuilder setParseSwitch(JavaCG2ParseSwitchBuilder javaCG2ParseSwitchBuilder) {
        if (javaCG2ParseSwitchBuilder == null) {
            throw new JavaCG2ConfigException("不允许传入null " + JavaCG2ParseSwitchBuilder.class.getSimpleName());
        }
        this.javaCG2ParseSwitchBuilder = javaCG2ParseSwitchBuilder;
        return this;
    }

    private JavaCG2JarDirBuilder genJarDirBuilder() {
        if (javaCG2JarDirBuilder == null) {
            javaCG2JarDirBuilder = JavaCG2JarDirBuilder.builder();
        }
        return javaCG2JarDirBuilder;
    }

    /**
     * 生成JavaCG2Entry对象
     *
     * @return
     */
    public JavaCG2Entry build() {
        List<String> jarOrDirPathList = genJarDirBuilder().build();
        if (jarOrDirPathList.isEmpty()) {
            String errorMsg = "未指定需要解析的jar、war、jmod文件或目录 " + JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR.genConfigUsage();
            throw new JavaCG2ConfigException(errorMsg);
        }

        // 提前检查需要解析的文件/目录是否存在，尽快发现问题
        for (String jarOrDirPath : jarOrDirPathList) {
            if (!new File(jarOrDirPath).exists()) {
                String errorMsg = "指定的需要解析的文件或目录不存在 " + jarOrDirPath + " " + JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR.genConfigUsage();
                throw new JavaCG2ConfigException(errorMsg);
            }
        }

        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper;
        if (javaCG2ConfigBuilder == null) {
            // 未设置config对应的Builder时，主要配置参数使用默认值
            javaCG2ConfigureWrapper = JavaCG2ConfigBuilder.builder().build();
        } else {
            javaCG2ConfigureWrapper = javaCG2ConfigBuilder.build();
        }

        if (javaCG2FrEqConversionMethodBuilder != null) {
            // 追加语义时，需要在生成的wrapper对象上执行先读取jar包中内置默认条目再合并的逻辑
            javaCG2FrEqConversionMethodBuilder.applyTo(javaCG2ConfigureWrapper);
        }

        if (javaCG2MergeFileSwitchBuilder != null) {
            javaCG2MergeFileSwitchBuilder.applyTo(javaCG2ConfigureWrapper);
        }

        if (javaCG2ParseSwitchBuilder != null) {
            javaCG2ParseSwitchBuilder.applyTo(javaCG2ConfigureWrapper);
        }

        // 对未被任何Builder设置过的表达式配置，使用jar包中内置配置文件的默认内容，保持与通过配置文件指定参数时的行为一致
        applyElDefaultText(javaCG2ConfigureWrapper);

        javaCG2ConfigureWrapper.setOtherConfigList(JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR, jarOrDirPathList);
        return new JavaCG2Entry(javaCG2ConfigureWrapper);
    }

    // 对未被任何Builder设置过的表达式配置，读取jar包中内置配置文件的默认内容并通过setElConfigText方法应用，读取方式参照BaseConfigureWrapper.getElConfigText方法
    private void applyElDefaultText(JavaCG2ConfigureWrapper javaCG2ConfigureWrapper) {
        for (JavaCG2ElConfigEnum elConfig : JavaCG2ElConfigEnum.values()) {
            if (JavaCG2ElUtil.checkElExample(elConfig)) {
                // 跳过示例表达式配置文件
                continue;
            }
            if (checkElConfigSetByBuilder(elConfig)) {
                // 已通过Builder设置的表达式配置使用设置的值
                continue;
            }
            // 读取jar包中内置配置文件的默认内容
            String elText = StringUtils.join(JavaCG2FileUtil.readFile2List(elConfig.getKey(), "##"), "");
            if (StringUtils.isBlank(elText)) {
                // 内置默认内容为空白时不设置，保持表达式不生效的现状
                continue;
            }
            logger.info("使用jar包中内置配置文件的默认表达式 {}", elConfig.getConfigPrintInfo());
            javaCG2ConfigureWrapper.setElConfigText(elConfig, elText);
        }
    }

    // 检查表达式配置是否已通过Builder设置
    private boolean checkElConfigSetByBuilder(ElConfigInterface elConfig) {
        if (javaCG2ParseSwitchBuilder != null && javaCG2ParseSwitchBuilder.getSetElConfigSet().contains(elConfig)) {
            return true;
        }
        return javaCG2MergeFileSwitchBuilder != null && javaCG2MergeFileSwitchBuilder.getSetElConfigSet().contains(elConfig);
    }
}
