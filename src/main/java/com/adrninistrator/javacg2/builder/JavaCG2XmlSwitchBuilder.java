package com.adrninistrator.javacg2.builder;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.el.enums.ElStringAnyFunctionEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElAllowedVariableEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.el.enums.interfaces.ElAllowedVariableInterface;
import com.adrninistrator.javacg2.el.enums.interfaces.ElConfigInterface;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: _javacg2_handle_xml_switch/handle_ignore_spring_bean_in_xml.av表达式配置文件对应的Builder，通过代码以链式方式指定处理XML文件中定义的Spring Bean时使用的忽略表达式，不依赖配置文件
 * 语义化方法内部生成合法的Aviator表达式文本（字符串参数自动以单引号包裹，不以分号结尾），表达式执行结果为true时跳过处理对应的Spring Bean
 * 同一个表达式配置枚举对应的方法多次调用时，以后调用的为准（表达式文本整体替换）
 * 未通过当前类设置的表达式配置使用jar包中内置配置文件的默认内容（内置默认为空白，即不生效）
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
     * 按Spring Bean名称跳过处理XML文件中定义的Spring Bean，使用string.equalsAny函数判断Bean名称是否与指定的一个或多个Bean名称完全相等
     *
     * @param beanNames 一个或多个Spring Bean名称，如"testService1"
     * @return
     */
    public JavaCG2XmlSwitchBuilder ignoreSpringBeanByBeanName(String... beanNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_SPB_BEAN_NAME,
                beanNames);
    }

    /**
     * 按Spring Bean类名跳过处理XML文件中定义的Spring Bean，使用string.equalsAny函数判断Bean类名是否与指定的一个或多个类名完全相等
     *
     * @param classNames 一个或多个Spring Bean类名，如"a.b.Class1"
     * @return
     */
    public JavaCG2XmlSwitchBuilder ignoreSpringBeanByClassName(String... classNames) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_SPB_CLASS_NAME,
                classNames);
    }

    /**
     * 按Spring Bean profile跳过处理XML文件中定义的Spring Bean，使用string.equalsAny函数判断profile是否与指定的一个或多个profile完全相等
     * profile可能为空字符串，可能包含一级或多级，使用半角逗号拼接
     *
     * @param profiles 一个或多个Spring Bean profile，如"dev"
     * @return
     */
    public JavaCG2XmlSwitchBuilder ignoreSpringBeanByProfile(String... profiles) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, ElStringAnyFunctionEnum.EQUALS_ANY, JavaCG2ElAllowedVariableEnum.EAVE_SPB_PROFILE,
                profiles);
    }

    /**
     * 按Spring Bean profile前缀跳过处理XML文件中定义的Spring Bean，使用string.startsWithAny函数判断profile是否以指定的一个或多个前缀开头
     * profile可能包含一级或多级，使用半角逗号拼接，通过前缀判断可用于匹配多级profile
     *
     * @param profilePrefixes 一个或多个Spring Bean profile前缀，如"dev"
     * @return
     */
    public JavaCG2XmlSwitchBuilder ignoreSpringBeanByProfilePrefix(String... profilePrefixes) {
        return putStringAny(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, ElStringAnyFunctionEnum.STARTS_WITH_ANY, JavaCG2ElAllowedVariableEnum.EAVE_SPB_PROFILE,
                profilePrefixes);
    }

    /**
     * 通过原始表达式文本设置跳过处理XML文件中定义的Spring Bean的表达式
     * 可用于语义化方法未覆盖的用法，如使用string.containsIC等函数
     *
     * @param elText 原始表达式文本，如"bean_name != 'testService1'"，null不允许，空白表示不生效
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

    // 记录使用字符串比较函数生成的表达式配置，字符串参数自动以单引号包裹
    private JavaCG2XmlSwitchBuilder putStringAny(ElConfigInterface elConfig, ElStringAnyFunctionEnum stringAnyFunctionEnum, ElAllowedVariableInterface elVariable,
                                                 String... args) {
        elSwitchConfigHolder.putStringAny(elConfig, stringAnyFunctionEnum, elVariable, args);
        return this;
    }
}
