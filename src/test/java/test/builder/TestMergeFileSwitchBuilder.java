package test.builder;

import com.adrninistrator.javacg2.builder.JavaCG2ConfigBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2EntryBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2MergeFileSwitchBuilder;
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
import java.util.ArrayList;
import java.util.List;
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

    // 用于端到端验证include（白名单）表达式的目录名称
    private static final String MERGE_INCLUDE_DIR_NAME = "test_merge_include_dir";

    // 用于端到端验证include（白名单）表达式的目录，每次使用前会清空
    private static final String MERGE_INCLUDE_DIR = "out/" + MERGE_INCLUDE_DIR_NAME;

    // 端到端验证使用的输出根目录
    private static final String OUT_DIR_INCLUDE_CONTROL = "out/test_merge_include_control";
    private static final String OUT_DIR_INCLUDE_ONLY = "out/test_merge_include_include_only";
    private static final String OUT_DIR_INCLUDE_WHITE_INNER_BLACK = "out/test_merge_include_white_inner_black";
    private static final String OUT_DIR_INCLUDE_GATES_INDEPENDENT = "out/test_merge_include_gates_independent";

    // 端到端验证使用的目录中的两个jar文件名，内容相同（均为test.jar的拷贝）
    private static final String AAA_BUSINESS_JAR = "aaa_business.jar";
    private static final String BBB_OTHER_JAR = "bbb_other.jar";

    // 仅合并（白名单）目录中jar文件的表达式，仅命中AAA_BUSINESS_JAR
    private static final String INCLUDE_JAR_IN_DIR_EL_TEXT = "string.equalsAny(file_name, '" + AAA_BUSINESS_JAR + "')";

    // 合并生成的jar文件中的目录名前缀分隔符，格式为"{序号}@{原jar文件名}"，参考JavaCG2JarUtil.genDirNameInJar
    private static final String DIR_NAME_IN_JAR_FLAG = "@";
    private static final String MERGED_JAR_SUFFIX = "-javacg2_merged.jar";

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

    // 原始include（白名单）表达式文本按原样设置，同一个枚举多次设置以后设置的为准；未设置时8个include枚举默认为空白（不生效，全部合并）
    @Test
    public void testSetIncludeExpressionText() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();
        JavaCG2MergeFileSwitchBuilder.builder()
                .setIncludeJarInDirExpression("string.equalsAny(file_name, 'aaa.jar')")
                .setIncludeWarInDirExpression("string.equalsAny(file_name, 'bbb.war')")
                .setIncludeClassInDirExpression("string.startsWithAny(file_dir_path, '/data/classes/com/business')")
                .setIncludeOtherInDirExpression("string.equalsAny(file_ext, '.xml', '.properties')")
                .setIncludeJarInJarWarExpression("string.startsWithAny(file_path, 'lib/business/')")
                .setIncludeClassInJarWarExpression("string.startsWithAny(class_file_path, 'com/business/')")
                .setIncludeOtherInJarWarExpression("string.equalsAny(file_ext, '.xml')")
                .setIncludeJarWarByClassDirPrefixExpression("include(class_dir_prefix_level_2, 'com/adrninistrator')")
                .applyTo(javaCG2ConfigureWrapper);
        Assert.assertEquals("string.equalsAny(file_name, 'aaa.jar')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_MERGE_FILE_INCLUDE_JAR_IN_DIR));
        Assert.assertEquals("string.equalsAny(file_name, 'bbb.war')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_MERGE_FILE_INCLUDE_WAR_IN_DIR));
        Assert.assertEquals("string.startsWithAny(file_dir_path, '/data/classes/com/business')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_MERGE_FILE_INCLUDE_CLASS_IN_DIR));
        Assert.assertEquals("string.equalsAny(file_ext, '.xml', '.properties')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_MERGE_FILE_INCLUDE_OTHER_IN_DIR));
        Assert.assertEquals("string.startsWithAny(file_path, 'lib/business/')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_MERGE_FILE_INCLUDE_JAR_IN_JAR_WAR));
        Assert.assertEquals("string.startsWithAny(class_file_path, 'com/business/')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_MERGE_FILE_INCLUDE_CLASS_IN_JAR_WAR));
        Assert.assertEquals("string.equalsAny(file_ext, '.xml')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_MERGE_FILE_INCLUDE_OTHER_IN_JAR_WAR));
        Assert.assertEquals("include(class_dir_prefix_level_2, 'com/adrninistrator')", javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum
                .ECE_MERGE_FILE_INCLUDE_JAR_WAR_BY_CLASS_DIR_PREFIX));

        // 同一个枚举多次设置以后设置的为准
        JavaCG2ConfigureWrapper wrapper2 = new JavaCG2ConfigureWrapper();
        JavaCG2MergeFileSwitchBuilder.builder()
                .setIncludeJarInDirExpression("string.equalsAny(file_name, 'a.jar')")
                .setIncludeJarInDirExpression("string.startsWithAny(file_path, '/data/lib/business')")
                .applyTo(wrapper2);
        Assert.assertEquals("string.startsWithAny(file_path, '/data/lib/business')", wrapper2.getElConfigText(JavaCG2ElConfigEnum
                .ECE_MERGE_FILE_INCLUDE_JAR_IN_DIR));

        // 未设置时include表达式默认为空白，即不生效（全部合并）
        JavaCG2ConfigureWrapper wrapper3 = new JavaCG2ConfigureWrapper();
        Assert.assertEquals("", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_MERGE_FILE_INCLUDE_JAR_IN_DIR));
        Assert.assertEquals("", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_MERGE_FILE_INCLUDE_WAR_IN_DIR));
        Assert.assertEquals("", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_MERGE_FILE_INCLUDE_CLASS_IN_DIR));
        Assert.assertEquals("", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_MERGE_FILE_INCLUDE_OTHER_IN_DIR));
        Assert.assertEquals("", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_MERGE_FILE_INCLUDE_JAR_IN_JAR_WAR));
        Assert.assertEquals("", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_MERGE_FILE_INCLUDE_CLASS_IN_JAR_WAR));
        Assert.assertEquals("", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_MERGE_FILE_INCLUDE_OTHER_IN_JAR_WAR));
        Assert.assertEquals("", wrapper3.getElConfigText(JavaCG2ElConfigEnum.ECE_MERGE_FILE_INCLUDE_JAR_WAR_BY_CLASS_DIR_PREFIX));
    }

    // 端到端验证include（白名单）jar表达式与ignore（黑名单）表达式两道判断串联的行为，将test.jar拷贝为目录中的两个jar文件后指定该目录触发合并，验证合并产物中两个jar文件的包含情况，决策表：
    // 1 对照组：不设置include表达式，全部合并，两个jar文件对应的目录前缀都出现
    // 2 白名单命中组：仅设置include表达式，命中的aaa_business.jar合并，未命中的bbb_other.jar被跳过（include未命中直接跳过，方向与ignore相反）
    // 3 白内黑组：include命中aaa_business.jar + ignore命中aaa_business.jar，先include命中后再判断ignore，ignore命中跳过，合并产物中两个jar文件都不出现
    // 4 两道门独立组：include命中aaa_business.jar + ignore命中bbb_other.jar，include未命中的bbb_other.jar被跳过且不再判断ignore，命中的aaa_business.jar正常合并
    @Test
    public void testIncludeJarInDirEndToEnd() throws Exception {
        // 清空并重建用于测试的目录，将test.jar拷贝为目录中两个不同名的jar文件
        File mergeIncludeDir = new File(MERGE_INCLUDE_DIR);
        if (mergeIncludeDir.exists()) {
            FileUtils.deleteDirectory(mergeIncludeDir);
        }
        FileUtils.forceMkdir(mergeIncludeDir);
        File testJarFile = new File(JAR_TEST_JAR);
        FileUtils.copyFile(testJarFile, new File(mergeIncludeDir, AAA_BUSINESS_JAR));
        FileUtils.copyFile(testJarFile, new File(mergeIncludeDir, BBB_OTHER_JAR));

        // 对照组：不设置include表达式，两个jar文件对应的目录前缀都出现
        List<String> entryPathList4Control = runMergeAndGetEntryPathList(OUT_DIR_INCLUDE_CONTROL, JavaCG2MergeFileSwitchBuilder.builder());
        log.info("对照组条目数量 {} 是否出现 {} {} {}", entryPathList4Control.size(), AAA_BUSINESS_JAR, checkJarInMergedJar(entryPathList4Control, AAA_BUSINESS_JAR),
                BBB_OTHER_JAR, checkJarInMergedJar(entryPathList4Control, BBB_OTHER_JAR));
        Assert.assertTrue(checkJarInMergedJar(entryPathList4Control, AAA_BUSINESS_JAR));
        Assert.assertTrue(checkJarInMergedJar(entryPathList4Control, BBB_OTHER_JAR));

        // 白名单命中组：include表达式命中aaa_business.jar，aaa_business.jar合并，bbb_other.jar未命中被跳过
        List<String> entryPathList4Include = runMergeAndGetEntryPathList(OUT_DIR_INCLUDE_ONLY, JavaCG2MergeFileSwitchBuilder.builder()
                .setIncludeJarInDirExpression(INCLUDE_JAR_IN_DIR_EL_TEXT));
        log.info("白名单命中组是否出现 {} {} {}", AAA_BUSINESS_JAR, checkJarInMergedJar(entryPathList4Include, AAA_BUSINESS_JAR), BBB_OTHER_JAR,
                checkJarInMergedJar(entryPathList4Include, BBB_OTHER_JAR));
        Assert.assertTrue(checkJarInMergedJar(entryPathList4Include, AAA_BUSINESS_JAR));
        Assert.assertFalse(checkJarInMergedJar(entryPathList4Include, BBB_OTHER_JAR));

        // 白内黑组：include命中aaa_business.jar后再判断ignore表达式，ignore命中跳过，两个jar文件都不出现
        List<String> entryPathList4WhiteInnerBlack = runMergeAndGetEntryPathList(OUT_DIR_INCLUDE_WHITE_INNER_BLACK, JavaCG2MergeFileSwitchBuilder.builder()
                .setIncludeJarInDirExpression(INCLUDE_JAR_IN_DIR_EL_TEXT)
                .setIgnoreJarInDirExpression("string.equalsAny(file_name, '" + AAA_BUSINESS_JAR + "')"));
        log.info("白内黑组是否出现 {} {} {}", AAA_BUSINESS_JAR, checkJarInMergedJar(entryPathList4WhiteInnerBlack, AAA_BUSINESS_JAR), BBB_OTHER_JAR,
                checkJarInMergedJar(entryPathList4WhiteInnerBlack, BBB_OTHER_JAR));
        Assert.assertFalse(checkJarInMergedJar(entryPathList4WhiteInnerBlack, AAA_BUSINESS_JAR));
        Assert.assertFalse(checkJarInMergedJar(entryPathList4WhiteInnerBlack, BBB_OTHER_JAR));

        // 两道门独立组：include命中的aaa_business.jar正常合并，include未命中的bbb_other.jar被跳过（ignore表达式虽命中bbb_other.jar但未被判断到）
        List<String> entryPathList4GatesIndependent = runMergeAndGetEntryPathList(OUT_DIR_INCLUDE_GATES_INDEPENDENT, JavaCG2MergeFileSwitchBuilder.builder()
                .setIncludeJarInDirExpression(INCLUDE_JAR_IN_DIR_EL_TEXT)
                .setIgnoreJarInDirExpression("string.equalsAny(file_name, '" + BBB_OTHER_JAR + "')"));
        log.info("两道门独立组是否出现 {} {} {}", AAA_BUSINESS_JAR, checkJarInMergedJar(entryPathList4GatesIndependent, AAA_BUSINESS_JAR), BBB_OTHER_JAR,
                checkJarInMergedJar(entryPathList4GatesIndependent, BBB_OTHER_JAR));
        Assert.assertTrue(checkJarInMergedJar(entryPathList4GatesIndependent, AAA_BUSINESS_JAR));
        Assert.assertFalse(checkJarInMergedJar(entryPathList4GatesIndependent, BBB_OTHER_JAR));
    }

    // 在指定输出根目录中执行合并，返回合并生成的jar文件中的条目路径列表
    private List<String> runMergeAndGetEntryPathList(String outputRootPath, JavaCG2MergeFileSwitchBuilder mergeFileSwitchBuilder) throws Exception {
        File outputRootDir = new File(outputRootPath);
        if (outputRootDir.exists()) {
            // 清理输出目录，避免上次执行的结果造成干扰
            FileUtils.deleteDirectory(outputRootDir);
        }

        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setConfig(JavaCG2ConfigBuilder.builder()
                        .setOutputRootPath(outputRootPath)
                        .setOutputFileExt(".md"))
                .setMergeFileSwitch(mergeFileSwitchBuilder)
                .addJarOrDir(MERGE_INCLUDE_DIR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());

        // 设置了output.root.path参数，合并生成的jar文件生成在输出根目录中，文件名为目录名加后缀
        File mergedJarFile = new File(outputRootPath, MERGE_INCLUDE_DIR_NAME + MERGED_JAR_SUFFIX);
        Assert.assertTrue(mergedJarFile.exists());
        return getJarEntryPathList(mergedJarFile);
    }

    // 获取jar文件中的非目录条目路径列表
    private List<String> getJarEntryPathList(File jarFile) throws Exception {
        List<String> entryPathList = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(jarFile)) {
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (!zipEntry.isDirectory()) {
                    entryPathList.add(zipEntry.getName());
                }
            }
        }
        return entryPathList;
    }

    // 判断合并生成的jar文件的条目路径中是否存在来自指定jar文件的条目，目录中的jar文件合并后条目路径以"{序号}@{jar文件名}/"开头
    private boolean checkJarInMergedJar(List<String> entryPathList, String jarFileName) {
        String dirNamePrefix = DIR_NAME_IN_JAR_FLAG + jarFileName + "/";
        for (String entryPath : entryPathList) {
            if (entryPath.contains(dirNamePrefix)) {
                return true;
            }
        }
        return false;
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
        // include（白名单）表达式的原始表达式文本为null
        try {
            JavaCG2MergeFileSwitchBuilder.builder().setIncludeJarInDirExpression(null);
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
