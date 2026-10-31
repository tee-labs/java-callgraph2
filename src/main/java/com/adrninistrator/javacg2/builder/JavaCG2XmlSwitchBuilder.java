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
 * @description: 处理XML文件中定义的Spring Bean的忽略表达式对应的Builder，通过代码以链式方式指定处理XML文件中定义的Spring Bean时使用的忽略表达式
 * 每个表达式配置枚举对应一个设置方法，通过原始Aviator表达式文本指定（字符串参数需要以单引号包裹，不以分号结尾），表达式执行结果为true时跳过处理对应的Spring Bean
 * 同一个表达式配置枚举对应的方法多次调用时，以后调用的为准（表达式文本整体替换），多种判断条件需要在同一个表达式中通过||等逻辑运算符组合
 * 未通过当前类设置的表达式配置使用枚举中定义的默认值（默认为空白，即不生效）
 * 该开关仅当待分析工程通过XML文件定义Spring Bean时相关
 * 当前类的实例只能通过applyTo方法应用到一个JavaCG2ConfigureWrapper对象，假如需要应用多个，请创建当前类的多个实例
 */
@Slf4j
public class JavaCG2XmlSwitchBuilder {

    // 表达式开关配置的公共逻辑
    private final ElSwitchConfigHolder elSwitchConfigHolder = new ElSwitchConfigHolder();

    public static JavaCG2XmlSwitchBuilder builder() {
        return new JavaCG2XmlSwitchBuilder();
    }

    private JavaCG2XmlSwitchBuilder() {
    }

    /**
     * 通过原始表达式文本设置跳过处理XML文件中定义的Spring Bean的表达式
     * 允许使用的变量为JavaCG2ElAllowedVariableEnum中以EAVE_SPB_开头的枚举常量：bean_name、class_name、profile
     *
     * @param elText 原始表达式文本，如"string.equalsAny(bean_name, 'testService1')"、"string.startsWithAny(profile, 'dev') || string.containsIC(class_name, 'test')"，null不允许，空白表示不生效
     * @return
     */
    public JavaCG2XmlSwitchBuilder setIgnoreSpringBeanInXmlExpression(String elText) {
        elSwitchConfigHolder.putRaw(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, elText);
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
