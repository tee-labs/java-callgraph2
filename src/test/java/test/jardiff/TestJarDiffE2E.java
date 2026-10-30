package test.jardiff;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.jardiff.JavaCG2JarDiffRunner;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
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
 * @description: JarDiff比较功能的e2e级测试
 * 使用真实解析生成的输出目录进行验证：
 * 1 对target/test.jar解析后的输出目录与自身比较，验证零变化
 * 2 使用编译后的测试类现场构造新旧两个版本的jar文件（新版本jar文件中多一个类），分别解析后比较，验证新增的类与方法被记录，class文件HASH相同的类被跳过
 */
public class TestJarDiffE2E extends TestBase {

    private static final String OUT_ROOT = "out/test_jardiff_e2e";

    // 解析target/test.jar使用的输出根目录
    private static final String OUT_ROOT_PARSE_TEST_JAR = OUT_ROOT + "/parse_test_jar";

    // 解析新旧版本jar文件使用的输出根目录
    private static final String OUT_ROOT_PARSE_DIFF_JAR_OLD = OUT_ROOT + "/parse_diff_jar_old";

    private static final String OUT_ROOT_PARSE_DIFF_JAR_NEW = OUT_ROOT + "/parse_diff_jar_new";

    // 比较结果文件输出目录
    private static final String DIFF_DIR = OUT_ROOT + "/diff";

    // 构造的新旧版本jar文件路径
    private static final String DIFF_JAR_OLD_PATH = OUT_ROOT + "/diffjar-1.0.jar";

    private static final String DIFF_JAR_NEW_PATH = OUT_ROOT + "/diffjar-2.0.jar";

    // JarDiff e2e测试使用的夹具类，新旧版本jar文件中均存在
    private static final String SAME_CLASS_NAME = "test.jardiff.diffjar.DiffJarService";

    // 仅在新版本jar文件中存在的夹具类
    private static final String NEW_CLASS_NAME = "test.jardiff.diffjar.DiffJarService2";

    @Before
    public void init() throws IOException {
        // 删除上次生成的目录
        FileUtils.deleteDirectory(new File(OUT_ROOT));
        Files.createDirectories(new File(OUT_ROOT).toPath());
    }

    /**
     * 验证对target/test.jar解析后的输出目录与自身比较，结果文件存在且内容为空
     */
    @Test
    public void testSelfDiffZeroChange() {
        String testJarOutputDirPath = parseTestJar(OUT_ROOT_PARSE_TEST_JAR);

        JavaCG2JarDiffRunner jarDiffRunner = new JavaCG2JarDiffRunner(testJarOutputDirPath, testJarOutputDirPath, DIFF_DIR);
        jarDiffRunner.setOutputFileExt(".md");
        Assert.assertTrue(jarDiffRunner.run());

        Assert.assertTrue("输出目录与自身比较时不应发生变化，发生变化的类的结果文件应为空",
                readFile(DIFF_DIR, JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_CLASSES).isEmpty());
        Assert.assertTrue("输出目录与自身比较时不应发生变化，发生变化的方法的结果文件应为空",
                readFile(DIFF_DIR, JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_METHODS).isEmpty());
        Assert.assertTrue(jarDiffRunner.getJarModifiedMethodInfoMap().isEmpty());
    }

    /**
     * 验证使用真实解析输出目录比较新旧版本的jar文件
     * 新版本jar文件中多一个类，该类应记录为新增的类，其中的方法应记录为新增的方法；两个版本中相同的类（class文件HASH相同）不应记录
     */
    @Test
    public void testDiffOldAndNewJar() throws IOException {
        // 构造新旧两个版本的jar文件并分别解析
        genDiffJar(DIFF_JAR_OLD_PATH, false);
        genDiffJar(DIFF_JAR_NEW_PATH, true);
        String outputDirPathOld = parseJar(DIFF_JAR_OLD_PATH, OUT_ROOT_PARSE_DIFF_JAR_OLD);
        String outputDirPathNew = parseJar(DIFF_JAR_NEW_PATH, OUT_ROOT_PARSE_DIFF_JAR_NEW);

        JavaCG2JarDiffRunner jarDiffRunner = new JavaCG2JarDiffRunner(outputDirPathOld, outputDirPathNew, DIFF_DIR);
        jarDiffRunner.setOutputFileExt(".md");
        Assert.assertTrue(jarDiffRunner.run());

        // 验证发生变化的类：仅新版本jar文件中存在的类被记录为新增的类
        List<String> modifiedClassList = readFile(DIFF_DIR, JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_CLASSES);
        Assert.assertEquals("发生变化的类应为1行", 1, modifiedClassList.size());
        String[] modifiedClassColumns = modifiedClassList.get(0).split("\t", -1);
        Assert.assertEquals("jar文件名称前缀", "diffjar", modifiedClassColumns[0]);
        Assert.assertEquals("新jar文件名称", "diffjar-2.0.jar", modifiedClassColumns[1]);
        Assert.assertEquals("旧jar文件名称", "diffjar-1.0.jar", modifiedClassColumns[2]);
        Assert.assertEquals("新增的类名", NEW_CLASS_NAME, modifiedClassColumns[3]);
        Assert.assertEquals("新增的类的变更类型", "N", modifiedClassColumns[4]);

        // 验证发生变化的方法：均为新增的方法，且不存在新旧版本中均存在的类的方法
        List<String> modifiedMethodList = readFile(DIFF_DIR, JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_METHODS);
        Assert.assertTrue("发生变化的方法不应为空", modifiedMethodList.size() >= 2);
        boolean existsNewMethod = false;
        for (String row : modifiedMethodList) {
            String[] columns = row.split("\t", -1);
            Assert.assertEquals("发生变化的方法的报告应为6列", 6, columns.length);
            Assert.assertEquals("jar文件名称前缀", "diffjar", columns[0]);
            Assert.assertTrue("发生变化的方法应仅属于新版本jar文件中新增的类 " + columns[3], columns[3].startsWith(NEW_CLASS_NAME + ":"));
            Assert.assertEquals("新增jar文件对比时方法应记录为新增", "false", columns[5]);
            if (columns[3].equals(NEW_CLASS_NAME + ":newMethod(java.lang.String)")) {
                existsNewMethod = true;
            }
        }
        Assert.assertTrue("新增的方法应被记录 " + NEW_CLASS_NAME + ":newMethod(java.lang.String)", existsNewMethod);

        // 验证两个版本中均存在的类（class文件HASH相同）未被记录
        for (String row : modifiedClassList) {
            Assert.assertEquals("class文件HASH相同的类不应记录，仅应记录新增的类 " + SAME_CLASS_NAME, NEW_CLASS_NAME, row.split("\t", -1)[3]);
        }
        for (String row : modifiedMethodList) {
            String[] columns = row.split("\t", -1);
            Assert.assertFalse("class文件HASH相同的类的方法不应记录 " + SAME_CLASS_NAME, columns[3].startsWith(SAME_CLASS_NAME + ":"));
        }
    }

    // 解析target/test.jar，返回输出目录路径
    private String parseTestJar(String outputRootPath) {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, outputRootPath);
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());
        return javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getOutputDirPath();
    }

    // 解析指定的jar文件，返回输出目录路径
    private String parseJar(String jarPath, String outputRootPath) {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(jarPath);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, outputRootPath);
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());
        return javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getOutputDirPath();
    }

    // 使用编译后的测试类构造JarDiff比较使用的jar文件，新版本jar文件中多一个类
    private void genDiffJar(String jarPath, boolean withNewClass) throws IOException {
        if (withNewClass) {
            genDiffJar(jarPath, SAME_CLASS_NAME, NEW_CLASS_NAME);
        } else {
            genDiffJar(jarPath, SAME_CLASS_NAME);
        }
    }

    // 将编译后的多个class文件写入一个jar文件
    private void genDiffJar(String jarPath, String... classNames) throws IOException {
        File jarFile = new File(jarPath);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(jarFile)))) {
            for (String className : classNames) {
                File classFile = new File("target/test-classes/" + className.replace(".", "/") + ".class");
                Assert.assertTrue("编译后的class文件应存在 " + JavaCG2FileUtil.getCanonicalPath(classFile), classFile.exists());
                zipOutputStream.putNextEntry(new ZipEntry(className.replace(".", "/") + ".class"));
                zipOutputStream.write(Files.readAllBytes(classFile.toPath()));
                zipOutputStream.closeEntry();
            }
        }
    }

    // 读取生成的比较结果文件内容
    private List<String> readFile(String dirPath, String fileName) {
        String filePath = JavaCG2FileUtil.addSeparator4FilePath(dirPath) + fileName + ".md";
        Assert.assertTrue("结果文件应存在: " + filePath, new File(filePath).exists());
        return JavaCG2FileUtil.readFile2List(filePath);
    }
}
