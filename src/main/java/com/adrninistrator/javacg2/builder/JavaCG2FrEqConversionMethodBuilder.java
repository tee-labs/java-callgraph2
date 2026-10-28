package com.adrninistrator.javacg2.builder;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseSetEnum;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: fr_eq_conversion_method.properties对应的Builder，通过代码以链式方式指定等值转换方法，不依赖配置文件
 * 每个条目的格式为 {完整类名}:{方法名}={被调用对象或方法参数序号}，其中方法名允许为空（例如 java.math.BigDecimal:=0）
 * addFrEqConversionMethod方法为追加语义，保留jar包中内置配置文件的默认条目
 * setFrEqConversionMethod方法为整体覆盖语义，不保留jar包中内置配置文件的默认条目
 * 当前类的实例只能通过applyTo方法应用到一个JavaCG2ConfigureWrapper对象，假如需要应用多个，请创建当前类的多个实例
 */
public class JavaCG2FrEqConversionMethodBuilder {

    private static final Logger logger = LoggerFactory.getLogger(JavaCG2FrEqConversionMethodBuilder.class);

    // 通过addFrEqConversionMethod或setFrEqConversionMethod方法收集的等值转换方法条目
    private final Set<String> frEqConversionMethodSet = new LinkedHashSet<>();

    // 是否使用整体覆盖语义
    private boolean override = false;

    public static JavaCG2FrEqConversionMethodBuilder builder() {
        return new JavaCG2FrEqConversionMethodBuilder();
    }

    private JavaCG2FrEqConversionMethodBuilder() {
    }

    /**
     * 添加等值转换方法，追加语义，保留jar包中内置配置文件的默认条目，与setFrEqConversionMethod方法的效果互斥，以后调用的方法为准
     *
     * @param className  完整类名，不允许为空
     * @param methodName 方法名，允许为空
     * @param argObjSeq  与方法返回值等值的被调用对象（使用0表示）或方法参数（从1开始）序号，需要大于等于0
     * @return
     */
    public JavaCG2FrEqConversionMethodBuilder addFrEqConversionMethod(String className, String methodName, int argObjSeq) {
        if (StringUtils.isBlank(className)) {
            throw new JavaCG2ConfigException("类名不允许为空 " + JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD.genConfigUsage());
        }
        checkArgObjSeq(argObjSeq, className, methodName);
        // 将类名、方法名与序号拼接为完整格式的条目
        String entry = className + JavaCG2Constants.FLAG_COLON + StringUtils.trimToEmpty(methodName) + JavaCG2Constants.FLAG_EQUAL + argObjSeq;
        return addFrEqConversionMethod(entry);
    }

    /**
     * 添加完整格式的等值转换方法条目，追加语义，保留jar包中内置配置文件的默认条目
     *
     * @param entries 一个或多个完整格式的条目：{完整类名}:{方法名}={被调用对象或方法参数序号}，方法名允许为空
     * @return
     */
    public JavaCG2FrEqConversionMethodBuilder addFrEqConversionMethod(String... entries) {
        if (entries == null || entries.length == 0) {
            throw new JavaCG2ConfigException("不允许传入null或空数组 " + JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD.genConfigUsage());
        }
        for (String entry : entries) {
            checkEntry(entry);
            frEqConversionMethodSet.add(entry);
        }
        return this;
    }

    /**
     * 添加完整格式的等值转换方法条目集合，追加语义，保留jar包中内置配置文件的默认条目
     *
     * @param entryCollection 完整格式的条目集合：{完整类名}:{方法名}={被调用对象或方法参数序号}，方法名允许为空
     * @return
     */
    public JavaCG2FrEqConversionMethodBuilder addFrEqConversionMethod(Collection<String> entryCollection) {
        if (entryCollection == null || entryCollection.isEmpty()) {
            throw new JavaCG2ConfigException("不允许传入null或空集合 " + JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD.genConfigUsage());
        }
        return addFrEqConversionMethod(entryCollection.toArray(new String[0]));
    }

    /**
     * 设置等值转换方法，整体覆盖语义，不保留jar包中内置配置文件的默认条目，与addFrEqConversionMethod方法的效果互斥，以后调用的方法为准
     *
     * @param className  完整类名，不允许为空
     * @param methodName 方法名，允许为空
     * @param argObjSeq  与方法返回值等值的被调用对象（使用0表示）或方法参数（从1开始）序号，需要大于等于0
     * @return
     */
    public JavaCG2FrEqConversionMethodBuilder setFrEqConversionMethod(String className, String methodName, int argObjSeq) {
        if (StringUtils.isBlank(className)) {
            throw new JavaCG2ConfigException("类名不允许为空 " + JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD.genConfigUsage());
        }
        checkArgObjSeq(argObjSeq, className, methodName);
        String entry = className + JavaCG2Constants.FLAG_COLON + StringUtils.trimToEmpty(methodName) + JavaCG2Constants.FLAG_EQUAL + argObjSeq;
        return setFrEqConversionMethod(entry);
    }

    /**
     * 设置完整格式的等值转换方法条目，整体覆盖语义，不保留jar包中内置配置文件的默认条目
     *
     * @param entries 一个或多个完整格式的条目：{完整类名}:{方法名}={被调用对象或方法参数序号}，方法名允许为空
     * @return
     */
    public JavaCG2FrEqConversionMethodBuilder setFrEqConversionMethod(String... entries) {
        if (entries == null || entries.length == 0) {
            throw new JavaCG2ConfigException("不允许传入null或空数组 " + JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD.genConfigUsage());
        }
        // 整体覆盖语义，清空之前收集的条目
        frEqConversionMethodSet.clear();
        override = true;
        for (String entry : entries) {
            checkEntry(entry);
            frEqConversionMethodSet.add(entry);
        }
        return this;
    }

    /**
     * 设置完整格式的等值转换方法条目集合，整体覆盖语义，不保留jar包中内置配置文件的默认条目
     *
     * @param entryCollection 完整格式的条目集合：{完整类名}:{方法名}={被调用对象或方法参数序号}，方法名允许为空
     * @return
     */
    public JavaCG2FrEqConversionMethodBuilder setFrEqConversionMethod(Collection<String> entryCollection) {
        if (entryCollection == null || entryCollection.isEmpty()) {
            throw new JavaCG2ConfigException("不允许传入null或空集合 " + JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD.genConfigUsage());
        }
        return setFrEqConversionMethod(entryCollection.toArray(new String[0]));
    }

    /**
     * 将当前收集的等值转换方法条目应用到配置参数包装对象
     * 追加语义时先读取jar包中内置配置文件的默认条目，再合并当前收集的条目
     *
     * @param javaCG2ConfigureWrapper
     */
    public void applyTo(JavaCG2ConfigureWrapper javaCG2ConfigureWrapper) {
        if (javaCG2ConfigureWrapper == null) {
            throw new JavaCG2ConfigException("不允许传入null " + JavaCG2ConfigureWrapper.class.getSimpleName());
        }
        if (override) {
            // 整体覆盖语义，直接替换已有的条目
            logger.info("以整体覆盖方式设置等值转换方法，数量 {}", frEqConversionMethodSet.size());
            javaCG2ConfigureWrapper.setOtherConfigSet(JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD, frEqConversionMethodSet);
            return;
        }
        // 追加语义，先读取jar包中内置配置文件的默认条目并缓存，再合并当前收集的条目
        Set<String> existedSet = javaCG2ConfigureWrapper.getOtherConfigSet(JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD, false);
        logger.info("以追加方式设置等值转换方法，已有条目数量 {} 新增条目数量 {}", existedSet.size(), frEqConversionMethodSet.size());
        javaCG2ConfigureWrapper.addOtherConfigSet(JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD, frEqConversionMethodSet);
    }

    // 检查完整格式的等值转换方法条目，校验规则与 JavaCG2ConfManager.handleFrEqConversionMethod 中的解析规则一致
    private void checkEntry(String entry) {
        if (StringUtils.isBlank(entry)) {
            throw new JavaCG2ConfigException("等值转换方法条目不允许为空 " + JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD.genConfigUsage());
        }
        String[] data1 = StringUtils.splitPreserveAllTokens(entry, JavaCG2Constants.FLAG_EQUAL);
        if (data1.length != 2) {
            throw new JavaCG2ConfigException("等值转换方法条目不是合法的properties参数 " + entry);
        }
        int argObjSeq;
        try {
            argObjSeq = Integer.parseInt(data1[1]);
        } catch (NumberFormatException e) {
            throw new JavaCG2ConfigException("等值转换方法条目的序号不是合法的数字 " + entry);
        }
        if (argObjSeq < 0) {
            throw new JavaCG2ConfigException("等值转换方法条目的被调用对象（使用0表示）或方法参数（从1开始）序号非法 " + entry);
        }
        String[] data2 = StringUtils.splitPreserveAllTokens(data1[0], JavaCG2Constants.FLAG_COLON);
        if (data2.length != 2) {
            throw new JavaCG2ConfigException("等值转换方法条目不是合法的类名与方法名 " + entry);
        }
    }

    // 检查被调用对象或方法参数序号
    private void checkArgObjSeq(int argObjSeq, String className, String methodName) {
        if (argObjSeq < 0) {
            throw new JavaCG2ConfigException("被调用对象（使用0表示）或方法参数（从1开始）序号非法 " + argObjSeq + " " + className + JavaCG2Constants.FLAG_COLON + methodName);
        }
    }
}
