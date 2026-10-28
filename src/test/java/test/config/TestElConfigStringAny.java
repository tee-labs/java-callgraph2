package test.config;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.el.enums.ElStringAnyFunctionEnum;
import com.adrninistrator.javacg2.el.enums.ElStringFunctionTwoArgsEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElAllowedVariableEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import com.adrninistrator.javacg2.exceptions.JavaCG2RuntimeException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.base.TestBase;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: BaseConfigureWrapper.setElConfigStringAny方法的测试，验证生成的表达式文本不以分号结尾、字符串参数自动以单引号包裹、非法参数抛出异常
 */
public class TestElConfigStringAny extends TestBase {

    private static final Logger logger = LoggerFactory.getLogger(TestElConfigStringAny.class);

    // ElStringAnyFunctionEnum版本生成的表达式文本正确：不以分号结尾，字符串参数自动以单引号包裹
    @Test
    public void testStringAnyElText() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper(true);
        javaCG2ConfigureWrapper.setElConfigStringAny(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, ElStringAnyFunctionEnum.EQUALS_ANY,
                JavaCG2ElAllowedVariableEnum.EAVE_SPB_BEAN_NAME, "testService1", "testService2");
        String elText = javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML);
        logger.info("生成的表达式文本 [{}]", elText);
        Assert.assertEquals("string.equalsAny(bean_name,'testService1','testService2')", elText);
        // 不以分号结尾
        Assert.assertFalse(elText.endsWith(";"));
        // 字符串参数以单引号包裹
        Assert.assertTrue(elText.contains("'testService1'"));
        Assert.assertTrue(elText.contains("'testService2'"));
    }

    // ElStringFunctionTwoArgsEnum版本生成的表达式文本正确：不以分号结尾，字符串参数自动以单引号包裹
    @Test
    public void testTwoArgsElText() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper(true);
        javaCG2ConfigureWrapper.setElConfigStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_CLASS, ElStringFunctionTwoArgsEnum.EQUALS_IGNORE_CASE,
                JavaCG2ElAllowedVariableEnum.EAVE_PARSE_CLASS_NAME, "a.b.Class1");
        String elText = javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_CLASS);
        logger.info("生成的表达式文本 [{}]", elText);
        Assert.assertEquals("string.equalsIC(class_name,'a.b.Class1')", elText);
        // 不以分号结尾
        Assert.assertFalse(elText.endsWith(";"));
        // 字符串参数以单引号包裹
        Assert.assertTrue(elText.contains("'a.b.Class1'"));
    }

    // 表达式变量非法时出现异常
    @Test
    public void testInvalidVariable() {
        try {
            // bean_name变量不允许在跳过解析类的表达式中使用
            new JavaCG2ConfigureWrapper(true).setElConfigStringAny(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_CLASS, ElStringAnyFunctionEnum.EQUALS_ANY,
                    JavaCG2ElAllowedVariableEnum.EAVE_SPB_BEAN_NAME, "a.b.Class1");
            Assert.fail();
        } catch (JavaCG2RuntimeException e) {
            logger.info("出现预期的异常 {}", e.getMessage());
        }
    }

    // 非法参数抛出异常
    @Test
    public void testInvalidArgs() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper(true);

        // 未指定需要比较的字符串值
        try {
            javaCG2ConfigureWrapper.setElConfigStringAny(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, ElStringAnyFunctionEnum.EQUALS_ANY,
                    JavaCG2ElAllowedVariableEnum.EAVE_SPB_BEAN_NAME);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            logger.info("出现预期的异常 {}", e.getMessage());
        }
        // 字符串值为null
        try {
            javaCG2ConfigureWrapper.setElConfigStringAny(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, ElStringAnyFunctionEnum.EQUALS_ANY,
                    JavaCG2ElAllowedVariableEnum.EAVE_SPB_BEAN_NAME, null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            logger.info("出现预期的异常 {}", e.getMessage());
        }
        // 字符串值为空白
        try {
            javaCG2ConfigureWrapper.setElConfigStringAny(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, ElStringAnyFunctionEnum.EQUALS_ANY,
                    JavaCG2ElAllowedVariableEnum.EAVE_SPB_BEAN_NAME, " ");
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            logger.info("出现预期的异常 {}", e.getMessage());
        }
        // 字符串值包含单引号
        try {
            javaCG2ConfigureWrapper.setElConfigStringAny(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, ElStringAnyFunctionEnum.EQUALS_ANY,
                    JavaCG2ElAllowedVariableEnum.EAVE_SPB_BEAN_NAME, "te'st");
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            logger.info("出现预期的异常 {}", e.getMessage());
        }
        // 两个参数的版本传入null
        try {
            javaCG2ConfigureWrapper.setElConfigStringAny(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, ElStringFunctionTwoArgsEnum.EQUALS_IGNORE_CASE,
                    JavaCG2ElAllowedVariableEnum.EAVE_SPB_BEAN_NAME, null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            logger.info("出现预期的异常 {}", e.getMessage());
        }
        // 两个参数的版本传入空白
        try {
            javaCG2ConfigureWrapper.setElConfigStringAny(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, ElStringFunctionTwoArgsEnum.EQUALS_IGNORE_CASE,
                    JavaCG2ElAllowedVariableEnum.EAVE_SPB_BEAN_NAME, " ");
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            logger.info("出现预期的异常 {}", e.getMessage());
        }
        // 两个参数的版本传入包含单引号的字符串
        try {
            javaCG2ConfigureWrapper.setElConfigStringAny(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML, ElStringFunctionTwoArgsEnum.EQUALS_IGNORE_CASE,
                    JavaCG2ElAllowedVariableEnum.EAVE_SPB_BEAN_NAME, "te'st");
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            logger.info("出现预期的异常 {}", e.getMessage());
        }
    }
}
