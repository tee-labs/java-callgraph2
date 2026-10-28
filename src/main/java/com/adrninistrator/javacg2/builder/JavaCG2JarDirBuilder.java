package com.adrninistrator.javacg2.builder;

import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import com.adrninistrator.javacg2.util.JavaCG2Util;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: jar_dir.properties对应的Builder，通过代码以链式方式指定需要解析的jar、war、jmod文件路径，或保存class、jar、war、jmod文件的目录路径
 * 路径的顺序与jar_dir.properties中每一行的顺序含义相同，会影响jar文件对应的序号
 * 当前类的实例只能通过build方法获取一次路径列表，假如需要获取多个，请创建当前类的多个实例
 */
public class JavaCG2JarDirBuilder {

    private final List<String> jarOrDirPathList = new ArrayList<>();

    public static JavaCG2JarDirBuilder builder() {
        return new JavaCG2JarDirBuilder();
    }

    private JavaCG2JarDirBuilder() {
    }

    /**
     * 添加需要解析的jar、war、jmod文件路径，或保存class、jar、war、jmod文件的目录路径，保留已有的路径
     *
     * @param jarOrDirPaths 一个或多个文件/目录路径，路径中的分隔符支持使用/或\，目录最后指定或不指定分隔符均可
     * @return
     */
    public JavaCG2JarDirBuilder addJarOrDir(String... jarOrDirPaths) {
        return addJarOrDir(Arrays.asList(jarOrDirPaths));
    }

    /**
     * 添加需要解析的jar、war、jmod文件路径，或保存class、jar、war、jmod文件的目录路径，保留已有的路径
     *
     * @param jarOrDirPaths 文件/目录路径集合
     * @return
     */
    public JavaCG2JarDirBuilder addJarOrDir(Collection<String> jarOrDirPaths) {
        if (jarOrDirPaths == null) {
            throw new JavaCG2ConfigException("不允许传入null " + JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR.genConfigUsage());
        }
        for (String jarOrDirPath : jarOrDirPaths) {
            if (StringUtils.isBlank(jarOrDirPath)) {
                throw new JavaCG2ConfigException("路径不允许为空 " + JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR.genConfigUsage());
            }
        }
        jarOrDirPathList.addAll(jarOrDirPaths);
        return this;
    }

    /**
     * 生成jar_dir.properties对应的路径列表
     *
     * @return 路径列表，顺序与添加时的顺序一致
     */
    public List<String> build() {
        return new ArrayList<>(jarOrDirPathList);
    }
}
