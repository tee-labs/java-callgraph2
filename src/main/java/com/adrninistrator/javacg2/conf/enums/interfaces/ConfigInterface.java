package com.adrninistrator.javacg2.conf.enums.interfaces;

import com.adrninistrator.javacg2.el.enums.interfaces.ElConfigInterface;
import com.adrninistrator.javacg2.util.JavaCG2ClassMethodUtil;

/**
 * @author adrninistrator
 * @date 2022/11/14
 * @description: 配置枚举继承的接口
 */
public interface ConfigInterface {

    // 获取枚举常量名称
    String getEnumConstantName();

    // 获取key
    String getKey();

    // 获取描述
    String[] getDescriptions();

    // 获取配置用于打印的信息
    String getConfigPrintInfo();

    /**
     * 生成配置参数的使用说明
     *
     * @return
     */
    String genConfigUsage();

    /**
     * 生成配置参数的使用说明
     *
     * @param configClass 通过代码设置配置参数的类
     * @return
     */
    default String doGenConfigUsage(Class<?> configClass) {
        String objName = JavaCG2ClassMethodUtil.getFirstLetterLowerClassName(configClass.getSimpleName());
        String enumSimpleClassName = this.getClass().getSimpleName();
        String enumConstantName = getEnumConstantName();
        String configMethodName;
        if (this instanceof ElConfigInterface) {
            configMethodName = "setElConfigText";
        } else if (this instanceof MainConfigInterface) {
            configMethodName = "setMainConfig";
        } else if (this instanceof OtherConfigInterface) {
            configMethodName = ((OtherConfigInterface) this).isSetOrList() ? "setOtherConfigSet" : "setOtherConfigList";
        } else {
            return "不支持的配置参数类型 " + this.getClass().getName();
        }
        return "当前参数可通过代码进行配置: " + objName + "." + configMethodName + "(" + enumSimpleClassName
                + "." + enumConstantName + ", ...);";
    }
}
