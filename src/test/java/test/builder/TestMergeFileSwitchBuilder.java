package test.builder;

import com.adrninistrator.javacg2.builder.JavaCG2ConfigBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2EntryBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2MergeFileSwitchBuilder;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: JavaCG2MergeFileSwitchBuilder的测试
 * 需要先在当前项目执行 mvn package -DskipTests -Ptest_gen_jar 命令生成对应jar文件
 */
@Slf4j
public class TestMergeFileSwitchBuilder extends TestBase {

    // 用于测试按文件名忽略的目录，每次使用前会清空
    private static final String MERGE_SWITCH_DIR = "target/merge_switch_dir";

    // 设置一个不匹配任何文件的忽略表达式，不影响正常执行
    @Test
    public void testIgnoreNotMatch() {
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setMergeFileSwitch(JavaCG2MergeFileSwitchBuilder.builder()
                        .setIgnoreJarInDirExpression("string.equalsAny(file_name, 'not_exists_1.jar', 'not_exists_2.jar')"))
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());
    }

    // 设置一个不匹配任何文件的按文件后缀忽略表达式，不影响正常执行
    @Test
    public void testIgnoreNotMatchByFileExt() {
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setMergeFileSwitch(JavaCG2MergeFileSwitchBuilder.builder()
                        .setIgnoreOtherInDirExpression("string.equalsAny(file_ext, '.not_exists_ext')"))
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());
    }

    // 仅按文件名忽略test.jar：当前只指定了一个jar文件，不需要合并，忽略目录中文件的表达式不会被执行，处理正常完成
    @Test
    public void testIgnoreTestJarDirectly() {
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setMergeFileSwitch(JavaCG2MergeFileSwitchBuilder.builder()
                        .setIgnoreJarInDirExpression("string.equalsAny(file_name, 'test.jar')"))
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());
    }

    // 端到端验证按文件名忽略表达式生效：将test.jar拷贝到指定目录中并指定该目录，合并时test.jar被表达式跳过，合并生成的jar文件中没有任何条目
    @Test
    public void testIgnoreTestJarInDir() throws Exception {
        // 清空并重建用于测试的目录，将test.jar拷贝到该目录中
        File mergeSwitchDir = new File(MERGE_SWITCH_DIR);
        if (mergeSwitchDir.exists()) {
            FileUtils.deleteDirectory(mergeSwitchDir);
        }
        FileUtils.forceMkdir(mergeSwitchDir);
        FileUtils.copyFileToDirectory(new File(JAR_TEST_JAR), mergeSwitchDir);

        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setConfig(JavaCG2ConfigBuilder.builder()
                        .setOutputRootPath(OUT_TEST_DIR)
                        .setOutputFileExt(".md"))
                .setMergeFileSwitch(JavaCG2MergeFileSwitchBuilder.builder()
                        .setIgnoreJarInDirExpression("string.equalsAny(file_name, 'test.jar')"))
                .addJarOrDir(MERGE_SWITCH_DIR)
                .build();
        log.info("执行结果 {}", javaCG2Entry.run());

        // 设置了output.root.path参数，检查在输出根目录中生成的合并jar文件
        File mergedJarFile = new File(OUT_TEST_DIR, "merge_switch_dir-javacg2_merged.jar");
        Assert.assertTrue(mergedJarFile.exists());
        int entryNum = countJarEntry(mergedJarFile);
        log.info("合并生成的jar文件中的条目数量 {}", entryNum);
        // 表达式跳过了目录中唯一的jar文件，合并生成的jar文件中没有任何条目
        Assert.assertEquals(0, entryNum);
    }

    // 统计jar文件中的条目数量
    private int countJarEntry(File jarFile) throws Exception {
        try (ZipFile zipFile = new ZipFile(jarFile)) {
            int entryNum = 0;
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (!zipEntry.isDirectory()) {
                    entryNum++;
                }
            }
            return entryNum;
        }
    }

    // 未设置MergeFileSwitch对应的Builder时不影响原行为，同时也验证多个不同枚举的原始表达式一起使用
    @Test
    public void testNotSetMergeFileSwitch() {
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setMergeFileSwitch(JavaCG2MergeFileSwitchBuilder.builder()
                        .setIgnoreJarInJarWarExpression("string.equalsAny(file_name, 'not_exists_inner.jar')")
                        .setIgnoreOtherInJarWarExpression("string.equalsAny(file_ext, '.not_exists_ext')"))
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());
    }

    // 非法参数抛出异常
    @Test
    public void testInvalidArgs() {
        // 原始表达式文本为null
        try {
            JavaCG2MergeFileSwitchBuilder.builder().setIgnoreJarWarByClassDirPrefixExpression(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        // applyTo方法传入null
        try {
            JavaCG2MergeFileSwitchBuilder.builder().applyTo(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
    }

    // EntryBuilder的setMergeFileSwitch方法传入null时出现异常
    @Test
    public void testEntryBuilderNullBuilder() {
        try {
            JavaCG2EntryBuilder.builder().setFrEqConversionMethod(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
        try {
            JavaCG2EntryBuilder.builder().setMergeFileSwitch(null);
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
    }
}
