package test.compatibility;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 测试Jar兼容性检查功能
 * 解析输入为target/test.jar（由mvn test时的maven-antrun-plugin自动生成），以及测试时构造的仅包含部分class文件的jar文件
 */
public class TestJarCompatibilityCheck extends TestBase {

    // 默认输出根目录
    private static final String OUT_DIR_DEFAULT = "out/test_jar_compatibility_default";

    // 开启Jar兼容性检查时的输出根目录
    private static final String OUT_DIR_ENABLED = "out/test_jar_compatibility_enabled";

    // 解析仅包含部分class文件的jar文件时的输出根目录
    private static final String OUT_DIR_PARTIAL_JAR = "out/test_jar_compatibility_partial_jar";

    // 使用Jar兼容性检查模式解析仅包含部分class文件的jar文件时的输出根目录
    private static final String OUT_DIR_PARTIAL_JAR_COMPAT_MODE = "out/test_jar_compatibility_partial_jar_compat_mode";

    // 构造的仅包含部分class文件的jar文件路径
    private static final String PARTIAL_JAR_PATH = "out/test_jar_compatibility/test_partial.jar";

    // 引用了CompatService的类
    private static final String USE_SERVICE_CLASS_NAME = "test.compatibility.compat1.UseService";

    // 在部分测试场景中不会打包进需要解析的jar文件的类
    private static final String MISSING_CLASS_NAME = "test.compatibility.compat1.CompatService";

    private static final String[] JDK_CLASS_NAME_PREFIXES = new String[]{"java.", "javax.", "javafx.", "sun.", "com.sun.", "jdk.", "org.omg.", "org.w3c.dom.",
            "org.xml.sax."};

    /**
     * 验证a：默认不执行Jar兼容性检查，不生成检查报告结果文件
     */
    @Test
    public void testCheckDisabledByDefault() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_DIR_DEFAULT);
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());

        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : genCompatibilityFileTypeArray()) {
            Assert.assertFalse("默认不执行Jar兼容性检查，不应生成报告文件 " + fileTypeEnum.getFileName(),
                    new File(javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getMainFilePath(fileTypeEnum)).exists());
        }
    }

    /**
     * 验证b：通过配置参数开启Jar兼容性检查后，生成全部检查报告结果文件
     * target/test.jar只包含当前工程的测试类，其中引用的当前工程主代码类、依赖库类均不在解析范围内，会在被引用类不存在的报告中记录
     */
    @Test
    public void testCheckEnabled() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_DIR_ENABLED);
        // 开启Jar兼容性检查
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_CHECK_JAR_COMPATIBILITY_ENABLE, Boolean.TRUE.toString());
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());

        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : genCompatibilityFileTypeArray()) {
            String filePath = javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getMainFilePath(fileTypeEnum);
            Assert.assertTrue("开启Jar兼容性检查后应生成报告文件 " + fileTypeEnum.getFileName(), new File(filePath).exists());
        }

        // 验证被引用类不存在的报告中不存在误报：不存在的类不应是JDK中的类，也不应是test包中的类（test包中的类都在解析范围内）
        List<String> classReferenceMissingList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_CLASS_REFERENCE_MISSING);
        Assert.assertFalse("target/test.jar中引用的当前工程主代码类、依赖库类不在解析范围内，被引用类不存在的报告中应有记录", classReferenceMissingList.isEmpty());
        for (String line : classReferenceMissingList) {
            String[] columns = line.split("\t", -1);
            Assert.assertEquals("被引用类不存在的报告应为5列", 5, columns.length);
            for (String jdkClassNamePrefix : JDK_CLASS_NAME_PREFIXES) {
                Assert.assertFalse("不存在的被引用类不应为JDK中的类 " + columns[1], columns[1].startsWith(jdkClassNamePrefix));
            }
            Assert.assertFalse("不存在的被引用类不应为test包中的类 " + columns[1], columns[1].startsWith("test."));
        }

        // 验证方法调用问题的报告中不存在误报：test包中的类都在解析范围内，被调用类不应缺失
        List<String> methodCallMissingList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_METHOD_CALL_MISSING);
        for (String line : methodCallMissingList) {
            String[] columns = line.split("\t", -1);
            Assert.assertEquals("方法调用问题的报告应为10列", 10, columns.length);
            String calleeClassName = columns[4].substring(0, columns[4].indexOf(':'));
            Assert.assertFalse("方法调用中被调用类不应缺失 " + columns[4], "方法调用中的被调用类未找到".equals(columns[0]) && calleeClassName.startsWith("test."));
            Assert.assertFalse("test包中类的方法调用不应记录方法未找到 " + columns[4], "未找到被调用方法".equals(columns[0]) && calleeClassName.startsWith("test."));
        }

        // target/test.jar中的class文件均只出现一次，不存在重复同名类，重复同名类冲突的报告应为空
        List<String> dupClassConflictList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_DUP_CLASS_CONFLICT);
        Assert.assertTrue("target/test.jar中不存在重复同名类，重复同名类冲突的报告应为空", dupClassConflictList.isEmpty());
    }

    /**
     * 验证c：解析仅包含部分class文件的jar文件时，被引用类、方法、字段不存在的问题应被记录
     * 构造仅包含test.compatibility.compat1.UseService的jar文件，其引用的test.compatibility.compat1.CompatService不在jar文件中
     */
    @Test
    public void testPartialJar() throws IOException {
        genPartialJar();
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(PARTIAL_JAR_PATH);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_DIR_PARTIAL_JAR);
        // 开启Jar兼容性检查
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_CHECK_JAR_COMPATIBILITY_ENABLE, Boolean.TRUE.toString());
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());

        // 验证被引用类不存在的报告中记录了CompatService
        List<String> classReferenceMissingList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_CLASS_REFERENCE_MISSING);
        Assert.assertEquals("被引用类不存在的报告中应有1条记录", 1, classReferenceMissingList.size());
        String[] classReferenceColumns = classReferenceMissingList.get(0).split("\t", -1);
        Assert.assertEquals("引用方类名", USE_SERVICE_CLASS_NAME, classReferenceColumns[0]);
        Assert.assertEquals("不存在的被引用类名", MISSING_CLASS_NAME, classReferenceColumns[1]);

        // 验证方法调用问题的报告中记录了CompatService中被调用类未找到
        List<String> methodCallMissingList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_METHOD_CALL_MISSING);
        int missingClassRowCount = 0;
        for (String line : methodCallMissingList) {
            String[] columns = line.split("\t", -1);
            Assert.assertEquals("方法调用问题的报告应为10列", 10, columns.length);
            Assert.assertEquals("问题描述", "方法调用中的被调用类未找到", columns[0]);
            Assert.assertTrue("被调用完整方法 " + columns[4], columns[4].startsWith(MISSING_CLASS_NAME + ":"));
            missingClassRowCount++;
        }
        Assert.assertEquals("方法调用问题的报告中被调用类未找到的记录应为3条（构造函数、实例方法、静态方法）", 3, missingClassRowCount);

        // 验证字段使用问题的报告中记录了CompatService中被使用字段所在的类未找到
        List<String> fieldUsageMissingList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_FIELD_USAGE_MISSING);
        Assert.assertEquals("字段使用问题的报告中应有1条记录", 1, fieldUsageMissingList.size());
        String[] fieldUsageColumns = fieldUsageMissingList.get(0).split("\t", -1);
        Assert.assertEquals("问题描述", "被使用字段所在的类未找到", fieldUsageColumns[0]);
        Assert.assertEquals("被使用的字段所在的类名", MISSING_CLASS_NAME, fieldUsageColumns[3]);
        Assert.assertEquals("被使用的字段名称", "PUBLIC_STATIC_FIELD", fieldUsageColumns[4]);

        // 仅包含一个class文件的jar文件中不存在重复同名类，重复同名类冲突的报告应为空
        List<String> dupClassConflictList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_DUP_CLASS_CONFLICT);
        Assert.assertTrue("重复同名类冲突的报告应为空", dupClassConflictList.isEmpty());
    }

    /**
     * 验证d：使用Jar兼容性检查模式时自动执行Jar兼容性检查；该模式下不生成方法调用、字段使用数据，对应报告为空
     */
    @Test
    public void testPartialJarCompatibilityMode() throws IOException {
        genPartialJar();
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(PARTIAL_JAR_PATH);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_DIR_PARTIAL_JAR_COMPAT_MODE);
        // 使用Jar兼容性检查模式，自动执行Jar兼容性检查
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_JAR_COMPATIBILITY_MODE, Boolean.TRUE.toString());
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());

        // 验证被引用类不存在的报告中记录了CompatService
        List<String> classReferenceMissingList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_CLASS_REFERENCE_MISSING);
        Assert.assertEquals("被引用类不存在的报告中应有1条记录", 1, classReferenceMissingList.size());
        String[] classReferenceColumns = classReferenceMissingList.get(0).split("\t", -1);
        Assert.assertEquals("不存在的被引用类名", MISSING_CLASS_NAME, classReferenceColumns[1]);

        // Jar兼容性检查模式下不生成方法调用、字段使用数据，对应报告应为空
        Assert.assertTrue("Jar兼容性检查模式下方法调用问题的报告应为空",
                readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_METHOD_CALL_MISSING).isEmpty());
        Assert.assertTrue("Jar兼容性检查模式下字段使用问题的报告应为空",
                readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_FIELD_USAGE_MISSING).isEmpty());
        Assert.assertTrue("重复同名类冲突的报告应为空",
                readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_DUP_CLASS_CONFLICT).isEmpty());
    }

    // 生成Jar兼容性检查报告结果文件类型数组
    private JavaCG2OutPutFileTypeEnum[] genCompatibilityFileTypeArray() {
        return new JavaCG2OutPutFileTypeEnum[]{
                JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_CLASS_REFERENCE_MISSING,
                JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_METHOD_CALL_MISSING,
                JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_FIELD_USAGE_MISSING,
                JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_DUP_CLASS_CONFLICT
        };
    }

    // 构造仅包含test.compatibility.compat1.UseService的jar文件，其中引用的CompatService不在jar文件中
    private void genPartialJar() throws IOException {
        File classFile = new File("target/test-classes/" + USE_SERVICE_CLASS_NAME.replace(".", "/") + ".class");
        Assert.assertTrue("编译后的class文件应存在 " + JavaCG2FileUtil.getCanonicalPath(classFile), classFile.exists());

        File partialJarFile = new File(PARTIAL_JAR_PATH);
        File parentDirFile = partialJarFile.getParentFile();
        if (!parentDirFile.exists() && !parentDirFile.mkdirs()) {
            Assert.fail("创建目录失败 " + JavaCG2FileUtil.getCanonicalPath(parentDirFile));
        }

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(partialJarFile)))) {
            zipOutputStream.putNextEntry(new ZipEntry(USE_SERVICE_CLASS_NAME.replace(".", "/") + ".class"));
            zipOutputStream.write(Files.readAllBytes(classFile.toPath()));
            zipOutputStream.closeEntry();
        }
    }

    // 读取生成的报告文件内容
    private List<String> readFile4Output(JavaCG2Entry javaCG2Entry, JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        String filePath = javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getMainFilePath(fileTypeEnum);
        Assert.assertTrue("结果文件应存在: " + filePath, new File(filePath).exists());
        return JavaCG2FileUtil.readFile2List(filePath);
    }
}
