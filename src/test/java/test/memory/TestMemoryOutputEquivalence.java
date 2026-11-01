package test.memory;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.compatibility.JavaCG2CompatibilityChecker;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.jardiff.JavaCG2JarDiffRunner;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import com.adrninistrator.javacg2.writer.JavaCG2MemoryOutputAdapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import test.base.TestBase;

import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 输出与读回和文件路径解耦的等价性测试
 * 同一输入分别以文件适配器+文件RecordSource、内存适配器+内存RecordSource执行，验证两条路径产生相同的分析结果：
 * 1 Jar兼容性检查：文件路径由JavaCG2Entry在解析后执行检查并生成报告文件，内存路径由JavaCG2CompatibilityChecker从内存适配器读回数据执行检查，
 * 两者生成的检查报告行内容一致（在表头开关关闭、开启两种状态下分别验证）
 * 2 JarDiff比较：文件路径通过输出目录构造JavaCG2JarDiffRunner，内存路径通过内存输出适配器构造，两者生成的比较结果行内容一致
 * 输出目录使用target目录，避免影响out/test目录中的文件
 */
@Slf4j
public class TestMemoryOutputEquivalence extends TestBase {

    private static final String OUT_ROOT = "target/memory_equiv_test";

    // 构造的仅包含部分class文件的jar文件路径（Jar兼容性检查使用）
    private static final String PARTIAL_JAR_PATH = OUT_ROOT + "/test_partial.jar";

    // Jar兼容性检查：文件路径的输出根目录，目录名中附加表头开关状态
    private static final String OUT_ROOT_CHECKER_FILE = OUT_ROOT + "/checker/file";

    // Jar兼容性检查：内存路径的输出根目录，目录名中附加表头开关状态
    private static final String OUT_ROOT_CHECKER_MEMORY = OUT_ROOT + "/checker/memory";

    // JarDiff比较：构造的新旧版本jar文件路径
    private static final String DIFF_JAR_OLD_PATH = OUT_ROOT + "/jardiff/diffjar-1.0.jar";
    private static final String DIFF_JAR_NEW_PATH = OUT_ROOT + "/jardiff/diffjar-2.0.jar";

    // JarDiff比较：文件路径的输出目录
    private static final String OUT_DIR_JARDIFF_FILE_OLD = OUT_ROOT + "/jardiff/file/old";
    private static final String OUT_DIR_JARDIFF_FILE_NEW = OUT_ROOT + "/jardiff/file/new";
    private static final String DIFF_DIR_JARDIFF_FILE = OUT_ROOT + "/jardiff/file/diff";

    // JarDiff比较：内存路径的输出根目录
    private static final String OUT_ROOT_JARDIFF_MEMORY_OLD = OUT_ROOT + "/jardiff/memory/old";
    private static final String OUT_ROOT_JARDIFF_MEMORY_NEW = OUT_ROOT + "/jardiff/memory/new";

    // 引用了CompatService的类（仅打包进部分class文件的jar文件）
    private static final String USE_SERVICE_CLASS_NAME = "test.compatibility.compat1.UseService";

    // JarDiff比较使用的夹具类，新旧版本jar文件中均存在/仅新版本jar文件中存在
    private static final String SAME_CLASS_NAME = "test.jardiff.diffjar.DiffJarService";
    private static final String NEW_CLASS_NAME = "test.jardiff.diffjar.DiffJarService2";

    // Jar兼容性检查的检查报告结果文件类型
    private static final JavaCG2OutPutFileTypeEnum[] REPORT_FILE_TYPES = new JavaCG2OutPutFileTypeEnum[]{
            JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_CLASS_REFERENCE_MISSING,
            JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_METHOD_CALL_MISSING,
            JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_FIELD_USAGE_MISSING,
            JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_DUP_CLASS_CONFLICT
    };

    @Before
    public void init() throws IOException {
        // 删除上次生成的目录
        FileUtils.deleteDirectory(new File(OUT_ROOT));
        Files.createDirectories(new File(OUT_ROOT).toPath());
    }

    /**
     * 验证Jar兼容性检查：内存路径与文件路径产生相同的检查报告
     * 在表头开关关闭、开启两种状态下分别验证
     */
    @Test
    public void testCompatibilityCheckerMemoryEqualsFile() throws IOException {
        genPartialJar();

        for (boolean headerEnable : new boolean[]{false, true}) {
            log.info("验证Jar兼容性检查的内存路径与文件路径等价性 表头开关: {}", headerEnable);

            // 文件路径：解析后由JavaCG2Entry执行检查，检查报告写入文件
            JavaCG2ConfigureWrapper wrapperFile = genJavaCG2ConfigureWrapper4JavaCg2(PARTIAL_JAR_PATH);
            wrapperFile.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_ROOT_CHECKER_FILE + "/" + headerEnable);
            wrapperFile.setMainConfig(JavaCG2ConfigKeyEnum.CKE_CHECK_JAR_COMPATIBILITY_ENABLE, Boolean.TRUE.toString());
            wrapperFile.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_FILE_HEADER_ENABLE, Boolean.toString(headerEnable));
            JavaCG2Entry entryFile = new JavaCG2Entry(wrapperFile);
            Assert.assertTrue("文件路径解析失败 表头开关: " + headerEnable, entryFile.run());

            // 内存路径：解析结果写入内存适配器，检查时从内存适配器读回数据，检查报告也写入内存适配器
            JavaCG2MemoryOutputAdapter memoryOutputAdapter = new JavaCG2MemoryOutputAdapter(headerEnable);
            JavaCG2ConfigureWrapper wrapperMemory = genJavaCG2ConfigureWrapper4JavaCg2(PARTIAL_JAR_PATH);
            wrapperMemory.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_ROOT_CHECKER_MEMORY + "/" + headerEnable);
            // 内存路径中检查通过JavaCG2CompatibilityChecker手动执行，关闭Entry内部的检查（其读回数据来源为文件）
            wrapperMemory.setMainConfig(JavaCG2ConfigKeyEnum.CKE_CHECK_JAR_COMPATIBILITY_ENABLE, Boolean.FALSE.toString());
            wrapperMemory.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_FILE_HEADER_ENABLE, Boolean.toString(headerEnable));
            JavaCG2Entry entryMemory = new JavaCG2Entry(wrapperMemory);
            entryMemory.setOutputAdapter(memoryOutputAdapter);
            Assert.assertTrue("内存路径解析失败 表头开关: " + headerEnable, entryMemory.run());
            JavaCG2CompatibilityChecker compatibilityChecker = new JavaCG2CompatibilityChecker(memoryOutputAdapter);
            Assert.assertTrue("内存路径Jar兼容性检查失败 表头开关: " + headerEnable, compatibilityChecker.check());

            // 两条路径的检查报告行内容一致
            for (JavaCG2OutPutFileTypeEnum reportFileTypeEnum : REPORT_FILE_TYPES) {
                String reportFilePath = entryFile.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getMainFilePath(reportFileTypeEnum);
                Assert.assertTrue("文件路径应生成检查报告文件 " + reportFilePath, new File(reportFilePath).exists());
                List<String> fileLineList = JavaCG2FileUtil.readFile2List(reportFilePath);
                List<String> memoryLineList = readLines(memoryOutputAdapter.getMainRecordContent(reportFileTypeEnum));
                Assert.assertEquals("检查报告行内容应与文件路径一致 " + reportFileTypeEnum.getFileName() + " 表头开关: " + headerEnable, fileLineList, memoryLineList);
            }

            // 保证比对有意义：被引用类不存在的报告中应有记录（CompatService不在部分class文件的jar文件中）
            List<String> classReferenceMissingList = JavaCG2FileUtil.readFile2List(
                    entryFile.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getMainFilePath(JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_CLASS_REFERENCE_MISSING));
            Assert.assertEquals("被引用类不存在的报告中应有1条记录 表头开关: " + headerEnable, headerEnable ? 2 : 1, classReferenceMissingList.size());
        }
    }

    /**
     * 验证JarDiff比较：内存路径与文件路径产生相同的比较结果
     * 解析使用表头开关开启的状态，同时验证读回时表头行被正确跳过
     */
    @Test
    public void testJarDiffMemoryEqualsFile() throws IOException {
        // 构造新旧两个版本的jar文件，新版本jar文件中多一个类
        genDiffJar(DIFF_JAR_OLD_PATH, SAME_CLASS_NAME);
        genDiffJar(DIFF_JAR_NEW_PATH, SAME_CLASS_NAME, NEW_CLASS_NAME);

        // 文件路径：解析到文件目录，通过输出目录构造JarDiffRunner
        String outputDirPathOld = parseJar2File(DIFF_JAR_OLD_PATH, OUT_DIR_JARDIFF_FILE_OLD);
        String outputDirPathNew = parseJar2File(DIFF_JAR_NEW_PATH, OUT_DIR_JARDIFF_FILE_NEW);
        JavaCG2JarDiffRunner jarDiffRunnerFile = new JavaCG2JarDiffRunner(outputDirPathOld, outputDirPathNew, DIFF_DIR_JARDIFF_FILE);
        jarDiffRunnerFile.setOutputFileExt(JavaCG2Constants.EXT_MD);
        Assert.assertTrue("文件路径JarDiff比较失败", jarDiffRunnerFile.run());

        // 内存路径：解析结果写入内存适配器，通过内存输出适配器构造JarDiffRunner
        JavaCG2MemoryOutputAdapter oldMemoryOutputAdapter = parseJar2Memory(DIFF_JAR_OLD_PATH, OUT_ROOT_JARDIFF_MEMORY_OLD);
        JavaCG2MemoryOutputAdapter newMemoryOutputAdapter = parseJar2Memory(DIFF_JAR_NEW_PATH, OUT_ROOT_JARDIFF_MEMORY_NEW);
        JavaCG2MemoryOutputAdapter diffMemoryOutputAdapter = new JavaCG2MemoryOutputAdapter();
        JavaCG2JarDiffRunner jarDiffRunnerMemory = new JavaCG2JarDiffRunner(oldMemoryOutputAdapter, newMemoryOutputAdapter, diffMemoryOutputAdapter);
        Assert.assertTrue("内存路径JarDiff比较失败", jarDiffRunnerMemory.run());

        // 两条路径的比较结果行内容一致
        List<String> fileClassLineList = readFile(DIFF_DIR_JARDIFF_FILE, JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_CLASSES);
        List<String> memoryClassLineList = readLines(diffMemoryOutputAdapter.getOtherRecordContent(JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_CLASSES));
        Assert.assertEquals("发生变化的类的比较结果行内容应与文件路径一致", fileClassLineList, memoryClassLineList);

        List<String> fileMethodLineList = readFile(DIFF_DIR_JARDIFF_FILE, JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_METHODS);
        List<String> memoryMethodLineList = readLines(diffMemoryOutputAdapter.getOtherRecordContent(JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_METHODS));
        Assert.assertEquals("发生变化的方法的比较结果行内容应与文件路径一致", fileMethodLineList, memoryMethodLineList);

        // 保证比对有意义：发生变化的类应为新版本jar文件中新增的类
        Assert.assertEquals("发生变化的类应为1行", 1, fileClassLineList.size());
        Assert.assertEquals("发生变化的类应为新版本jar文件中新增的类", NEW_CLASS_NAME, fileClassLineList.get(0).split("\t", -1)[3]);
        Assert.assertTrue("发生变化的方法不应为空", fileMethodLineList.size() >= 2);
        // 两条路径在内存中记录的发生变化的类、方法信息也应一致
        Assert.assertEquals("发生变化的类的数量应与文件路径一致", jarDiffRunnerFile.getJarModifiedClassInfoMap().size(),
                jarDiffRunnerMemory.getJarModifiedClassInfoMap().size());
        Assert.assertEquals("发生变化的方法的数量应与文件路径一致", jarDiffRunnerFile.getJarModifiedMethodInfoMap().size(),
                jarDiffRunnerMemory.getJarModifiedMethodInfoMap().size());
    }

    // 解析指定的jar文件，结果写入文件目录，返回输出目录路径
    private String parseJar2File(String jarPath, String outputRootPath) {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(jarPath);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, outputRootPath);
        // 表头开关开启，验证读回时表头行被正确跳过
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_FILE_HEADER_ENABLE, Boolean.TRUE.toString());
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue("解析失败 " + jarPath, javaCG2Entry.run());
        return javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getOutputDirPath();
    }

    // 解析指定的jar文件，结果写入内存输出适配器
    private JavaCG2MemoryOutputAdapter parseJar2Memory(String jarPath, String outputRootPath) {
        JavaCG2MemoryOutputAdapter memoryOutputAdapter = new JavaCG2MemoryOutputAdapter(true);
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(jarPath);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, outputRootPath);
        // 表头开关开启，验证读回时表头行被正确跳过
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_FILE_HEADER_ENABLE, Boolean.TRUE.toString());
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        javaCG2Entry.setOutputAdapter(memoryOutputAdapter);
        Assert.assertTrue("解析失败 " + jarPath, javaCG2Entry.run());
        return memoryOutputAdapter;
    }

    // 构造仅包含test.compatibility.compat1.UseService的jar文件，其中引用的CompatService不在jar文件中
    private void genPartialJar() throws IOException {
        File classFile = new File("target/test-classes/" + USE_SERVICE_CLASS_NAME.replace(".", "/") + ".class");
        Assume.assumeTrue("编译后的class文件不存在，跳过对应测试 " + JavaCG2FileUtil.getCanonicalPath(classFile), classFile.exists());

        File partialJarFile = new File(PARTIAL_JAR_PATH);
        File parentDirFile = partialJarFile.getParentFile();
        Assert.assertTrue("创建目录失败 " + JavaCG2FileUtil.getCanonicalPath(parentDirFile), parentDirFile.exists() || parentDirFile.mkdirs());

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(partialJarFile)))) {
            zipOutputStream.putNextEntry(new ZipEntry(USE_SERVICE_CLASS_NAME.replace(".", "/") + ".class"));
            zipOutputStream.write(Files.readAllBytes(classFile.toPath()));
            zipOutputStream.closeEntry();
        }
    }

    // 将编译后的多个class文件写入一个jar文件
    private void genDiffJar(String jarPath, String... classNames) throws IOException {
        File jarFile = new File(jarPath);
        File parentDirFile = jarFile.getParentFile();
        Assert.assertTrue("创建目录失败 " + JavaCG2FileUtil.getCanonicalPath(parentDirFile), parentDirFile.exists() || parentDirFile.mkdirs());

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(jarFile)))) {
            for (String className : classNames) {
                File classFile = new File("target/test-classes/" + className.replace(".", "/") + ".class");
                Assume.assumeTrue("编译后的class文件不存在，跳过对应测试 " + JavaCG2FileUtil.getCanonicalPath(classFile), classFile.exists());
                zipOutputStream.putNextEntry(new ZipEntry(className.replace(".", "/") + ".class"));
                zipOutputStream.write(Files.readAllBytes(classFile.toPath()));
                zipOutputStream.closeEntry();
            }
        }
    }

    // 读取生成的比较结果文件内容
    private List<String> readFile(String dirPath, String fileName) {
        String filePath = JavaCG2FileUtil.addSeparator4FilePath(dirPath) + fileName + JavaCG2Constants.EXT_MD;
        Assert.assertTrue("结果文件应存在: " + filePath, new File(filePath).exists());
        return JavaCG2FileUtil.readFile2List(filePath);
    }

    // 读取内存适配器收集的全部行内容
    private List<String> readLines(String content) throws IOException {
        try (BufferedReader br = new BufferedReader(new StringReader(content))) {
            List<String> lineList = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lineList.add(line);
            }
            return lineList;
        }
    }
}
