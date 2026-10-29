package com.adrninistrator.javacg2.conf.enums.interfaces;

/**
 * @author adrninistrator
 * @date 2023/3/27
 * @description: 主要配置参数继承的接口
 */
public interface MainConfigInterface extends ConfigInterface {
    // 配置参数所属分组的标识名
    String getFileName();

    // 字段类型
    Class<?> getType();

    // 是否不允许为空
    boolean isNotBlank();

    // 默认值
    String getDefaultValue();
}
