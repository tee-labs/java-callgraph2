package test.builder;

import com.adrninistrator.javacg2.builder.JavaCG2ConfigBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2EntryBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2FrEqConversionMethodBuilder;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseSetEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

import java.util.Arrays;
import java.util.Set;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: JavaCG2FrEqConversionMethodBuilder的测试
 */
@Slf4j
public class TestFrEqConversionMethodBuilder extends TestBase {

    // jar包中内置fr_eq_conversion_method.properties的默认条目
    private static final String DEFAULT_ENTRY = "java.lang.Boolean:valueOf=1";

    // 自定义条目
    private static final String CUSTOM_ENTRY = "test.callgraph.dto.TestDto1:copy=1";

    // add方法为追加语义，保留jar包中内置的默认条目
    @Test
    public void testAddKeepDefault() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = JavaCG2ConfigBuilder.builder().build();
        JavaCG2FrEqConversionMethodBuilder.builder()
                .addFrEqConversionMethod("test.callgraph.dto.TestDto1", "copy", 1)
                .addFrEqConversionMethod("test.callgraph.dto.TestDto2", "", 0)
                .applyTo(javaCG2ConfigureWrapper);
        Set<String> frEqConversionMethodSet = javaCG2ConfigureWrapper.getOtherConfigSet(JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD);
        log.info("追加后的条目 {}", frEqConversionMethodSet);
        Assert.assertTrue(frEqConversionMethodSet.contains(DEFAULT_ENTRY));
        Assert.assertTrue(frEqConversionMethodSet.contains(CUSTOM_ENTRY));
        Assert.assertTrue(frEqConversionMethodSet.contains("test.callgraph.dto.TestDto2:=0"));
    }

    // 通过JavaCG2EntryBuilder接线使用add方法，追加语义不影响正常执行
    @Test
    public void testAddViaEntryBuilder() {
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setFrEqConversionMethod(JavaCG2FrEqConversionMethodBuilder.builder()
                        .addFrEqConversionMethod("test.not.exists.Class1", "method1", 1))
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());
    }

    // set方法为整体覆盖语义，不保留jar包中内置的默认条目
    @Test
    public void testSetOverride() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = JavaCG2ConfigBuilder.builder().build();
        JavaCG2FrEqConversionMethodBuilder.builder()
                .setFrEqConversionMethod(CUSTOM_ENTRY, "java.lang.String:trim=0")
                .applyTo(javaCG2ConfigureWrapper);
        Set<String> frEqConversionMethodSet = javaCG2ConfigureWrapper.getOtherConfigSet(JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD);
        log.info("覆盖后的条目 {}", frEqConversionMethodSet);
        Assert.assertEquals(2, frEqConversionMethodSet.size());
        Assert.assertTrue(frEqConversionMethodSet.contains(CUSTOM_ENTRY));
        Assert.assertTrue(frEqConversionMethodSet.contains("java.lang.String:trim=0"));
        Assert.assertFalse(frEqConversionMethodSet.contains(DEFAULT_ENTRY));
    }

    // set方法以集合方式整体覆盖
    @Test
    public void testSetOverrideCollection() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = JavaCG2ConfigBuilder.builder().build();
        JavaCG2FrEqConversionMethodBuilder.builder()
                .addFrEqConversionMethod("test.callgraph.before.Class1", "method1", 1)
                .setFrEqConversionMethod(Arrays.asList("java.lang.String:trim=0"))
                .applyTo(javaCG2ConfigureWrapper);
        Set<String> frEqConversionMethodSet = javaCG2ConfigureWrapper.getOtherConfigSet(JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD);
        log.info("覆盖后的条目 {}", frEqConversionMethodSet);
        Assert.assertEquals(1, frEqConversionMethodSet.size());
        Assert.assertTrue(frEqConversionMethodSet.contains("java.lang.String:trim=0"));
    }

    // 方法名允许为空（内置条目java.math.BigDecimal:=0即方法名为空）
    @Test
    public void testEmptyMethodName() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = JavaCG2ConfigBuilder.builder().build();
        JavaCG2FrEqConversionMethodBuilder.builder()
                .addFrEqConversionMethod("java.math.BigInteger", "", 0)
                .applyTo(javaCG2ConfigureWrapper);
        Set<String> frEqConversionMethodSet = javaCG2ConfigureWrapper.getOtherConfigSet(JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD);
        Assert.assertTrue(frEqConversionMethodSet.contains("java.math.BigInteger:=0"));
        Assert.assertTrue(frEqConversionMethodSet.contains(DEFAULT_ENTRY));
    }

    // 序号为负数时出现异常
    @Test
    public void testInvalidArgObjSeq() {
        try {
            JavaCG2FrEqConversionMethodBuilder.builder().addFrEqConversionMethod("a.b.Class1", "method1", -1);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        try {
            JavaCG2FrEqConversionMethodBuilder.builder().setFrEqConversionMethod("a.b.Class1", "method1", -1);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
    }

    // 非法的完整格式条目出现异常
    @Test
    public void testInvalidEntry() {
        String[] invalidEntries = new String[]{
                "a.b.Class1:method1=x", // 序号不是数字
                "a.b.Class1:method1=-1", // 序号为负数
                "a.b.Class1:method1=1=2", // 等号超过一个
                "a.b.Class1:method1", // 缺少等号
                "a.b.Class1=1", // 缺少冒号
                " ", // 空白
        };
        for (String invalidEntry : invalidEntries) {
            try {
                JavaCG2FrEqConversionMethodBuilder.builder().addFrEqConversionMethod(invalidEntry);
                Assert.fail();
            } catch (JavaCG2ConfigException e) {
                log.info("出现预期的异常 {}", e.getMessage());
            }
        }
    }

    // 类名为空或未指定条目时出现异常
    @Test
    public void testBlankArgs() {
        try {
            JavaCG2FrEqConversionMethodBuilder.builder().addFrEqConversionMethod(" ", "method1", 1);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        try {
            JavaCG2FrEqConversionMethodBuilder.builder().addFrEqConversionMethod((String[]) null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        try {
            JavaCG2FrEqConversionMethodBuilder.builder().addFrEqConversionMethod(new String[0]);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        try {
            JavaCG2FrEqConversionMethodBuilder.builder().applyTo(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
    }
}
