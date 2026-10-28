package com.adrninistrator.javacg2.builder;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;

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
 *             .addJarOrDir("D:/tmp/test.jar")
 *             .build();
 *     boolean success = javaCG2Entry.run();
 * config与jar_dir对应的Builder均可不设置，config未设置时主要配置参数使用默认值
 * 未通过Builder设置的其他类型配置参数（如fr_eq_conversion_method.properties）使用jar包中内置配置文件的默认内容
 * 当前类的实例只能通过build方法生成一个JavaCG2Entry对象，假如需要生成多个，请创建当前类的多个实例
 * 生成的JavaCG2Entry对象只能执行一次run方法，假如需要再次执行，请通过当前类的新的实例生成新的JavaCG2Entry对象
 */
public class JavaCG2EntryBuilder {

    private JavaCG2ConfigBuilder javaCG2ConfigBuilder;

    private JavaCG2JarDirBuilder javaCG2JarDirBuilder;

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

        javaCG2ConfigureWrapper.setOtherConfigList(JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR, jarOrDirPathList);
        return new JavaCG2Entry(javaCG2ConfigureWrapper);
    }
}
