package test.builder;

import com.adrninistrator.javacg2.builder.JavaCG2ConfigBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2EntryBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2XmlSwitchBuilder;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: JavaCG2XmlSwitchBuilder的测试
 * 需要先在当前项目执行 mvn package -DskipTests -Ptest_gen_jar 命令生成对应jar文件
 */
@Slf4j
public class TestXmlSwitchBuilder extends TestBase {

    // 语义化方法生成的表达式文本正确
    @Test
    public void testGenExpressionText() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();
        JavaCG2XmlSwitchBuilder.builder()
                .ignoreSpringBeanByBeanName("testService1", "testService2")
                .applyTo(javaCG2ConfigureWrapper);
        Assert.assertEquals("string.equalsAny(bean_name,'testService1','testService2')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML));

        JavaCG2ConfigureWrapper wrapper2 = new JavaCG2ConfigureWrapper();
        JavaCG2XmlSwitchBuilder.builder()
                .ignoreSpringBeanByClassName("a.b.Class1", "a.b.Class2")
                .applyTo(wrapper2);
        Assert.assertEquals("string.equalsAny(class_name,'a.b.Class1','a.b.Class2')", wrapper2.getElConfigText(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML));

        JavaCG2ConfigureWrapper wrapper3 = new JavaCG2ConfigureWrapper();
        JavaCG2XmlSwitchBuilder.builder()
                .ignoreSpringBeanByProfile("dev")
                .applyTo(wrapper3);
        Assert.assertEquals("string.equalsAny(profile,'dev')", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML));

        JavaCG2ConfigureWrapper wrapper4 = new JavaCG2ConfigureWrapper();
        JavaCG2XmlSwitchBuilder.builder()
                .ignoreSpringBeanByProfilePrefix("dev", "test")
                .applyTo(wrapper4);
        Assert.assertEquals("string.startsWithAny(profile,'dev','test')", wrapper4.getElConfigText(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML));
    }

    // 同一个表达式配置枚举多次设置以后设置的为准
    @Test
    public void testLastSetWins() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();
        JavaCG2XmlSwitchBuilder.builder()
                .ignoreSpringBeanByBeanName("testService1")
                .ignoreSpringBeanByClassName("a.b.Class1")
                .applyTo(javaCG2ConfigureWrapper);
        Assert.assertEquals("string.equalsAny(class_name,'a.b.Class1')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML));

        // 语义化方法与原始表达式入口以后设置的为准
        JavaCG2ConfigureWrapper wrapper2 = new JavaCG2ConfigureWrapper();
        JavaCG2XmlSwitchBuilder.builder()
                .ignoreSpringBeanByBeanName("testService1")
                .setIgnoreSpringBeanInXmlExpression("profile != 'dev'")
                .applyTo(wrapper2);
        Assert.assertEquals("profile != 'dev'", wrapper2.getElConfigText(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML));
    }

    // 原始表达式入口按原样设置
    @Test
    public void testRawExpression() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();
        JavaCG2XmlSwitchBuilder.builder()
                .setIgnoreSpringBeanInXmlExpression("string.containsIC(class_name, 'test')")
                .applyTo(javaCG2ConfigureWrapper);
        Assert.assertEquals("string.containsIC(class_name, 'test')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML));
    }

    // 非法参数抛出异常
    @Test
    public void testInvalidArgs() {
        // 未指定Bean名称
        try {
            JavaCG2XmlSwitchBuilder.builder().ignoreSpringBeanByBeanName();
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        // 类名为null
        try {
            JavaCG2XmlSwitchBuilder.builder().ignoreSpringBeanByClassName(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        // profile为空白
        try {
            JavaCG2XmlSwitchBuilder.builder().ignoreSpringBeanByProfile(" ");
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        // profile前缀包含单引号
        try {
            JavaCG2XmlSwitchBuilder.builder().ignoreSpringBeanByProfilePrefix("de'v");
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        // 原始表达式文本为null
        try {
            JavaCG2XmlSwitchBuilder.builder().setIgnoreSpringBeanInXmlExpression(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        // applyTo方法传入null
        try {
            JavaCG2XmlSwitchBuilder.builder().applyTo(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        // EntryBuilder的setXmlSwitch方法传入null
        try {
            JavaCG2EntryBuilder.builder().setXmlSwitch(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
    }

    // 设置不匹配任何Spring Bean的忽略表达式后不影响正常执行
    @Test
    public void testRunNotMatch() {
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setXmlSwitch(JavaCG2XmlSwitchBuilder.builder()
                        .ignoreSpringBeanByBeanName("not_exists_bean"))
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());
    }

    // 内置默认对齐：未设置xml switch时表达式使用jar包中内置配置文件的默认内容（内置为空白）；设置后使用自定义内容
    @Test
    public void testElDefaultAlignment() {
        JavaCG2ConfigBuilder javaCG2ConfigBuilder = JavaCG2ConfigBuilder.builder();
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setConfig(javaCG2ConfigBuilder)
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());

        // 未设置xml switch时使用jar包中内置配置文件的默认内容（内置为空白，即不生效）
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = javaCG2ConfigBuilder.build();
        Assert.assertEquals("", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML));

        // 设置了xml switch的忽略表达式后使用自定义内容
        JavaCG2ConfigBuilder javaCG2ConfigBuilder2 = JavaCG2ConfigBuilder.builder();
        JavaCG2Entry javaCG2Entry2 = JavaCG2EntryBuilder.builder()
                .setConfig(javaCG2ConfigBuilder2)
                .setXmlSwitch(JavaCG2XmlSwitchBuilder.builder()
                        .ignoreSpringBeanByBeanName("not_exists_bean"))
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry2.run());
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper2 = javaCG2ConfigBuilder2.build();
        Assert.assertEquals("string.equalsAny(bean_name,'not_exists_bean')", javaCG2ConfigureWrapper2.getElConfigText(JavaCG2ElConfigEnum
                .ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML));
    }
}
