package test.builder;

import com.adrninistrator.javacg2.builder.JavaCG2ConfigBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2EntryBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2ParseSwitchBuilder;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: JavaCG2ParseSwitchBuilder的测试
 * 需要先在当前项目执行 mvn package -DskipTests -Ptest_gen_jar 命令生成对应jar文件
 */
@Slf4j
public class TestParseSwitchBuilder extends TestBase {

    // jar包中内置的parse_ignore_method.av默认表达式内容
    private static final String BUILT_IN_IGNORE_METHOD_EL_TEXT = "string.endsWithAny(full_method, 'hashCode()', 'equals(java.lang.Object)')";

    // 用于端到端验证过滤生效的类，在test.jar中真实存在
    private static final String CLASS_TO_IGNORE = "test.el.fixed.TestElFixedFalse";

    // 用于端到端验证白名单生效的类，在test.jar中真实存在
    private static final String CLASS_TO_INCLUDE = "test.el.fixed.TestElFixedTrue";

    // 仅解析（白名单）类表达式，仅命中CLASS_TO_INCLUDE
    private static final String INCLUDE_CLASS_EL_TEXT = "string.equalsAny(class_name, '" + CLASS_TO_INCLUDE + "')";

    // 端到端验证使用的输出根目录
    private static final String OUT_DIR_IGNORE = "out/test_parse_switch_ignore";
    private static final String OUT_DIR_CONTROL = "out/test_parse_switch_control";
    private static final String OUT_DIR_INCLUDE_CONTROL = "out/test_parse_switch_include_control";
    private static final String OUT_DIR_INCLUDE_ONLY = "out/test_parse_switch_include_only";
    private static final String OUT_DIR_INCLUDE_WHITE_INNER_BLACK = "out/test_parse_switch_include_white_inner_black";
    private static final String OUT_DIR_INCLUDE_GATES_INDEPENDENT = "out/test_parse_switch_include_gates_independent";

    // 原始表达式文本按原样设置，同一个枚举多次设置以后设置的为准
    @Test
    public void testSetExpressionText() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();
        JavaCG2ParseSwitchBuilder.builder()
                .setIgnoreClassExpression("string.equalsAny(class_name, 'a.b.Class1', 'a.b.Class2')")
                .setIgnoreMethodExpression("string.endsWithAny(full_method, 'hashCode()', 'equals(java.lang.Object)')")
                .setIgnoreMethodCallExpression("!string.containsIC(ee_class_name, 'test')")
                .setIgnoreTypeValueCallerExpression("er_method_arg_num != 4")
                .applyTo(javaCG2ConfigureWrapper);
        Assert.assertEquals("string.equalsAny(class_name, 'a.b.Class1', 'a.b.Class2')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_CLASS));
        Assert.assertEquals("string.endsWithAny(full_method, 'hashCode()', 'equals(java.lang.Object)')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_PARSE_IGNORE_METHOD));
        Assert.assertEquals("!string.containsIC(ee_class_name, 'test')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL));
        Assert.assertEquals("er_method_arg_num != 4", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD_CALL_TYPE_VALUE_CALLER));

        // 同一个枚举多次设置以后设置的为准
        JavaCG2ConfigureWrapper wrapper2 = new JavaCG2ConfigureWrapper();
        JavaCG2ParseSwitchBuilder.builder()
                .setIgnoreClassExpression("string.equalsAny(class_name, 'a.b.Class1')")
                .setIgnoreClassExpression("string.startsWithAny(package_name, 'a.b.')")
                .applyTo(wrapper2);
        Assert.assertEquals("string.startsWithAny(package_name, 'a.b.')", wrapper2.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_CLASS));

        // 多种判断条件可通过||组合在同一个表达式中
        JavaCG2ConfigureWrapper wrapper3 = new JavaCG2ConfigureWrapper();
        JavaCG2ParseSwitchBuilder.builder()
                .setIgnoreClassExpression("string.equalsAny(class_name, 'a.b.Class1') || string.startsWithAny(package_name, 'a.b.')")
                .applyTo(wrapper3);
        Assert.assertEquals("string.equalsAny(class_name, 'a.b.Class1') || string.startsWithAny(package_name, 'a.b.')", wrapper3.getElConfigText(JavaCG2ElConfigEnum
                .ECE_PARSE_IGNORE_CLASS));
    }

    // 原始include（白名单）表达式文本按原样设置，同一个枚举多次设置以后设置的为准；未设置时默认为空白（不生效，全部解析）
    @Test
    public void testSetIncludeExpressionText() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();
        JavaCG2ParseSwitchBuilder.builder()
                .setIncludeClassExpression("string.equalsAny(class_name, 'a.b.Class1', 'a.b.Class2')")
                .setIncludeMethodExpression("string.startsWithAny(package_name, 'com.business.')")
                .setIncludeMethodCallExpression("string.equalsAny(method_call_type, 'VIR', 'INT')")
                .setIncludeTypeValueCallerExpression("string.startsWithAny(er_package_name, 'com.business.')")
                .applyTo(javaCG2ConfigureWrapper);
        Assert.assertEquals("string.equalsAny(class_name, 'a.b.Class1', 'a.b.Class2')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_PARSE_INCLUDE_CLASS));
        Assert.assertEquals("string.startsWithAny(package_name, 'com.business.')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_PARSE_INCLUDE_METHOD));
        Assert.assertEquals("string.equalsAny(method_call_type, 'VIR', 'INT')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_PARSE_INCLUDE_METHOD_CALL));
        Assert.assertEquals("string.startsWithAny(er_package_name, 'com.business.')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_PARSE_INCLUDE_METHOD_CALL_TYPE_VALUE_CALLER));

        // 同一个枚举多次设置以后设置的为准
        JavaCG2ConfigureWrapper wrapper2 = new JavaCG2ConfigureWrapper();
        JavaCG2ParseSwitchBuilder.builder()
                .setIncludeClassExpression("string.equalsAny(class_name, 'a.b.Class1')")
                .setIncludeClassExpression("string.startsWithAny(package_name, 'a.b.')")
                .applyTo(wrapper2);
        Assert.assertEquals("string.startsWithAny(package_name, 'a.b.')", wrapper2.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_CLASS));

        // 未设置时include表达式默认为空白，即不生效（全部解析）
        JavaCG2ConfigureWrapper wrapper3 = new JavaCG2ConfigureWrapper();
        Assert.assertEquals("", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_CLASS));
        Assert.assertEquals("", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_METHOD));
        Assert.assertEquals("", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_METHOD_CALL));
        Assert.assertEquals("", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_METHOD_CALL_TYPE_VALUE_CALLER));
    }

    // 设置不匹配任何类/方法/方法调用的忽略表达式后不影响正常执行
    @Test
    public void testRunNotMatch() {
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setParseSwitch(JavaCG2ParseSwitchBuilder.builder()
                        .setIgnoreClassExpression("string.equalsAny(class_name, 'not.exists.Class1')")
                        .setIgnoreMethodExpression("string.endsWithAny(full_method, 'not_exists()')")
                        .setIgnoreMethodCallExpression("string.equalsAny(ee_class_name, 'not.exists.Class2')")
                        .setIgnoreTypeValueCallerExpression("string.equalsAny(er_full_method, 'not.exists.Class3:test1()')"))
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());
    }

    // 端到端验证通过ignore类表达式跳过test.jar中的类：表达式匹配时类不再出现在class_info.md中，对照组（不匹配的表达式）该类出现
    @Test
    public void testIgnoreClassEndToEnd() throws Exception {
        // 对照组：设置不匹配任何类的忽略表达式
        String classInfo4Control = runAndReadClassInfo(OUT_DIR_CONTROL, JavaCG2ParseSwitchBuilder.builder()
                .setIgnoreClassExpression("string.equalsAny(class_name, 'not.exists.Class1')"));
        log.info("对照组class_info.md中是否出现类 {} {}", CLASS_TO_IGNORE, classInfo4Control.contains(CLASS_TO_IGNORE));
        // 不匹配任何类的忽略表达式不会跳过类
        Assert.assertTrue(classInfo4Control.contains(CLASS_TO_IGNORE));

        // 过滤组：设置匹配的忽略表达式
        String classInfo4Ignore = runAndReadClassInfo(OUT_DIR_IGNORE, JavaCG2ParseSwitchBuilder.builder()
                .setIgnoreClassExpression("string.equalsAny(class_name, '" + CLASS_TO_IGNORE + "')"));
        log.info("过滤组class_info.md中是否出现类 {} {}", CLASS_TO_IGNORE, classInfo4Ignore.contains(CLASS_TO_IGNORE));
        // 匹配的忽略表达式跳过类
        Assert.assertFalse(classInfo4Ignore.contains(CLASS_TO_IGNORE));
    }

    // 端到端验证include（白名单）类表达式与ignore（黑名单）表达式两道判断串联的行为，决策表：
    // 1 对照组：不设置include表达式，全部解析，两个类都出现
    // 2 白名单命中组：仅设置include表达式，命中的类出现，未命中的类被跳过（include未命中直接跳过，方向与ignore相反）
    // 3 白内黑组：include命中CLASS_TO_INCLUDE + ignore命中CLASS_TO_INCLUDE，先include命中后再判断ignore，ignore命中跳过，类不出现
    // 4 两道门独立组：include命中CLASS_TO_INCLUDE + ignore命中CLASS_TO_IGNORE，include未命中的CLASS_TO_IGNORE被跳过且不再判断ignore，命中的CLASS_TO_INCLUDE正常出现
    @Test
    public void testIncludeClassEndToEnd() throws Exception {
        // 对照组：不设置include表达式，TestElFixedTrue与TestElFixedFalse都出现
        String classInfo4Control = runAndReadClassInfo(OUT_DIR_INCLUDE_CONTROL, JavaCG2ParseSwitchBuilder.builder());
        log.info("对照组class_info.md中是否出现类 {} {} {} {}", CLASS_TO_INCLUDE, classInfo4Control.contains(CLASS_TO_INCLUDE), CLASS_TO_IGNORE,
                classInfo4Control.contains(CLASS_TO_IGNORE));
        Assert.assertTrue(classInfo4Control.contains(CLASS_TO_INCLUDE));
        Assert.assertTrue(classInfo4Control.contains(CLASS_TO_IGNORE));

        // 白名单命中组：include表达式命中CLASS_TO_INCLUDE，CLASS_TO_INCLUDE出现，CLASS_TO_IGNORE未命中被跳过
        String classInfo4Include = runAndReadClassInfo(OUT_DIR_INCLUDE_ONLY, JavaCG2ParseSwitchBuilder.builder()
                .setIncludeClassExpression(INCLUDE_CLASS_EL_TEXT));
        log.info("白名单命中组class_info.md中是否出现类 {} {} {} {}", CLASS_TO_INCLUDE, classInfo4Include.contains(CLASS_TO_INCLUDE), CLASS_TO_IGNORE,
                classInfo4Include.contains(CLASS_TO_IGNORE));
        Assert.assertTrue(classInfo4Include.contains(CLASS_TO_INCLUDE));
        Assert.assertFalse(classInfo4Include.contains(CLASS_TO_IGNORE));

        // 白内黑组：include命中CLASS_TO_INCLUDE后再判断ignore表达式，ignore命中跳过，CLASS_TO_INCLUDE不出现
        String classInfo4WhiteInnerBlack = runAndReadClassInfo(OUT_DIR_INCLUDE_WHITE_INNER_BLACK, JavaCG2ParseSwitchBuilder.builder()
                .setIncludeClassExpression(INCLUDE_CLASS_EL_TEXT)
                .setIgnoreClassExpression("string.equalsAny(class_name, '" + CLASS_TO_INCLUDE + "')"));
        log.info("白内黑组class_info.md中是否出现类 {} {}", CLASS_TO_INCLUDE, classInfo4WhiteInnerBlack.contains(CLASS_TO_INCLUDE));
        Assert.assertFalse(classInfo4WhiteInnerBlack.contains(CLASS_TO_INCLUDE));

        // 两道门独立组：include命中的CLASS_TO_INCLUDE出现，include未命中的CLASS_TO_IGNORE被跳过（ignore表达式虽命中CLASS_TO_IGNORE但两道判断相互独立）
        String classInfo4GatesIndependent = runAndReadClassInfo(OUT_DIR_INCLUDE_GATES_INDEPENDENT, JavaCG2ParseSwitchBuilder.builder()
                .setIncludeClassExpression(INCLUDE_CLASS_EL_TEXT)
                .setIgnoreClassExpression("string.equalsAny(class_name, '" + CLASS_TO_IGNORE + "')"));
        log.info("两道门独立组class_info.md中是否出现类 {} {} {} {}", CLASS_TO_INCLUDE, classInfo4GatesIndependent.contains(CLASS_TO_INCLUDE), CLASS_TO_IGNORE,
                classInfo4GatesIndependent.contains(CLASS_TO_IGNORE));
        Assert.assertTrue(classInfo4GatesIndependent.contains(CLASS_TO_INCLUDE));
        Assert.assertFalse(classInfo4GatesIndependent.contains(CLASS_TO_IGNORE));
    }

    // 在指定输出根目录中执行解析并返回生成的class_info.md文件内容
    private String runAndReadClassInfo(String outputRootPath, JavaCG2ParseSwitchBuilder parseSwitchBuilder) throws IOException {
        File outputRootDir = new File(outputRootPath);
        if (outputRootDir.exists()) {
            // 清理输出目录，避免上次执行的结果造成干扰
            FileUtils.deleteDirectory(outputRootDir);
        }

        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setConfig(JavaCG2ConfigBuilder.builder()
                        .setOutputRootPath(outputRootPath)
                        .setOutputFileExt(".md"))
                .setParseSwitch(parseSwitchBuilder)
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());

        File classInfoFile = new File(outputRootPath, "test.jar-output_javacg2/class_info.md");
        Assert.assertTrue(classInfoFile.exists());
        return FileUtils.readFileToString(classInfoFile, StandardCharsets.UTF_8);
    }

    // 内置默认对齐：未设置parse switch时，ignore方法表达式使用jar包中内置配置文件的默认内容；设置后使用自定义内容
    @Test
    public void testElDefaultAlignment() {
        JavaCG2ConfigBuilder javaCG2ConfigBuilder = JavaCG2ConfigBuilder.builder();
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setConfig(javaCG2ConfigBuilder)
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());

        // 未设置parse switch时使用jar包中内置配置文件的默认内容
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = javaCG2ConfigBuilder.build();
        Assert.assertEquals(BUILT_IN_IGNORE_METHOD_EL_TEXT, javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD));

        // 未设置parse switch时include表达式使用枚举中定义的默认值，为空白（不生效，全部解析）
        Assert.assertEquals("", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_CLASS));
        Assert.assertEquals("", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_METHOD));
        Assert.assertEquals("", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_METHOD_CALL));
        Assert.assertEquals("", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_INCLUDE_METHOD_CALL_TYPE_VALUE_CALLER));

        // 设置了parse switch的ignore方法表达式后使用自定义内容
        JavaCG2ConfigBuilder javaCG2ConfigBuilder2 = JavaCG2ConfigBuilder.builder();
        JavaCG2Entry javaCG2Entry2 = JavaCG2EntryBuilder.builder()
                .setConfig(javaCG2ConfigBuilder2)
                .setParseSwitch(JavaCG2ParseSwitchBuilder.builder()
                        .setIgnoreMethodExpression("string.equalsAny(method_name, 'not_exists_method')"))
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry2.run());
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper2 = javaCG2ConfigBuilder2.build();
        Assert.assertEquals("string.equalsAny(method_name, 'not_exists_method')", javaCG2ConfigureWrapper2.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD));
    }

    // 内置默认对齐：merge switch未设置的枚举使用jar包中内置配置文件的默认内容（内置为空，维持空白）
    @Test
    public void testMergeSwitchDefaultBlank() {
        JavaCG2ConfigBuilder javaCG2ConfigBuilder = JavaCG2ConfigBuilder.builder();
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setConfig(javaCG2ConfigBuilder)
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());

        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = javaCG2ConfigBuilder.build();
        Assert.assertEquals("", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_JAR_IN_DIR));
        Assert.assertEquals("", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_JAR_WAR_BY_CLASS_DIR_PREFIX));
    }

    // 非法参数抛出异常
    @Test
    public void testInvalidArgs() {
        // 原始表达式文本为null
        try {
            JavaCG2ParseSwitchBuilder.builder().setIgnoreClassExpression(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        // include（白名单）表达式的原始表达式文本为null
        try {
            JavaCG2ParseSwitchBuilder.builder().setIncludeClassExpression(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        // applyTo方法传入null
        try {
            JavaCG2ParseSwitchBuilder.builder().applyTo(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
    }

    // EntryBuilder的setParseSwitch方法传入null时出现异常
    @Test
    public void testEntryBuilderNullBuilder() {
        try {
            JavaCG2EntryBuilder.builder().setParseSwitch(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
    }
}
