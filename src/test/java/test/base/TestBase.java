package test.base;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.exceptions.JavaCG2RuntimeException;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author adrninistrator
 * @date 2025/2/1
 * @description: 测试基类
 * 对java-all-call-graph项目中的测试jar文件按以下顺序解析，避免对环境的强依赖：
 * 1 通过系统属性 javacg2.test.jacg.dir 指定的java-all-call-graph项目根目录
 * 2 当前工程的兄弟目录中的java-all-call-graph项目
 * 3 以上均未找到时，请求标准测试jar文件target/test.jar的测试回退使用当前工程生成的同名jar文件（mvn test时由maven-antrun-plugin自动生成）
 * 4 其他无法替代的文件（如Spring Boot jar），抛出AssumptionViolatedException跳过对应测试，而不是报错
 */
public abstract class TestBase {

    private static final Logger logger = LoggerFactory.getLogger(TestBase.class);

    public static final String JAR_OUTPUT_DIR = "jar_output_dir";
    public static final String JAR_OUTPUT_JAR = "target/jar_output.jar";
    public static final String JAR_OUTPUT_WAR = "target/jar_output.war";
    public static final String OUT_TEST_DIR = "out/test";
    public static final String JAR_TEST_JAR = "target/test.jar";

    /**
     * 指定java-all-call-graph项目根目录的系统属性
     */
    public static final String SYS_PROP_JACG_DIR = "javacg2.test.jacg.dir";

    private static final String TEST_CLASSES_DIR = "target/test-classes";

    /**
     * 获取 java-all-call-graph 项目中的 test.jar 文件路径，解析顺序见类说明
     *
     * @return
     */
    protected String getJarInJacgDirPath(String jarPath) {
        String jacgDirPath = findJacgDir();
        if (jacgDirPath != null) {
            String jacgJarPath = jacgDirPath + "/" + jarPath;
            if (new File(jacgJarPath).exists()) {
                return jacgJarPath;
            }
        }

        if (JAR_TEST_JAR.equals(jarPath)) {
            // 标准测试jar文件回退为当前工程生成的同名jar文件
            genLocalTestJarIfAbsent();
            logger.warn("未找到java-all-call-graph项目中的jar文件 {}，回退使用当前工程生成的同名jar文件", jarPath);
            return jarPath;
        }

        logger.error("未找到java-all-call-graph项目中的jar文件 {}，跳过对应测试。启用方式：在java-all-call-graph项目中生成对应的jar文件，" +
                "并通过系统属性 {} 指定该项目根目录，或将其克隆为当前工程的兄弟目录", jarPath, SYS_PROP_JACG_DIR);
        throw new AssumptionViolatedException("在java-all-call-graph项目中未找到指定的jar文件 " + jarPath);
    }

    /**
     * 判断java-all-call-graph项目中的标准测试jar文件是否可用（通过系统属性或兄弟目录查找）
     * 断言依赖该jar文件中特定内容的测试，在执行前需要先调用当前方法，缺少时跳过测试
     */
    protected void assumeJacgTestJarExists() {
        String jacgDirPath = findJacgDir();
        if (jacgDirPath != null && new File(jacgDirPath + "/" + JAR_TEST_JAR).exists()) {
            return;
        }

        logger.error("当前测试依赖java-all-call-graph项目中的 {}，未找到，跳过。可通过系统属性 {} 指定该项目根目录", JAR_TEST_JAR, SYS_PROP_JACG_DIR);
        throw new AssumptionViolatedException("缺少java-all-call-graph项目中的jar文件 " + JAR_TEST_JAR);
    }

    /**
     * 假如当前工程的 target/test.jar 不存在，则将编译后的测试类打包生成，等价于maven的test_gen_jar profile
     */
    protected void genLocalTestJarIfAbsent() {
        File testJarFile = new File(JAR_TEST_JAR);
        if (testJarFile.exists()) {
            return;
        }

        File testClassDir = new File(TEST_CLASSES_DIR);
        if (!testClassDir.isDirectory()) {
            throw new AssumptionViolatedException("未找到编译后的测试类目录 " + JavaCG2FileUtil.getCanonicalPath(testClassDir));
        }

        String testClassDirPath = JavaCG2FileUtil.getCanonicalPath(testClassDir);
        List<String> classFilePathList = new ArrayList<>();
        JavaCG2FileUtil.searchDir(testClassDirPath, null, classFilePathList, JavaCG2Constants.EXT_CLASS);
        if (classFilePathList.isEmpty()) {
            throw new AssumptionViolatedException("编译后的测试类目录中没有class文件 " + testClassDirPath);
        }

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(testJarFile)))) {
            byte[] buffer = new byte[8192];
            for (String classFilePath : classFilePathList) {
                String entryName = classFilePath.substring(testClassDirPath.length() + 1).replace("\\", "/");
                zipOutputStream.putNextEntry(new ZipEntry(entryName));
                try (InputStream inputStream = new BufferedInputStream(new FileInputStream(classFilePath))) {
                    int readLen;
                    while ((readLen = inputStream.read(buffer)) != -1) {
                        zipOutputStream.write(buffer, 0, readLen);
                    }
                }
                zipOutputStream.closeEntry();
            }
        } catch (IOException e) {
            throw new JavaCG2RuntimeException("生成 " + JAR_TEST_JAR + " 失败 " + e.getMessage());
        }
        logger.info("已将当前工程的测试类打包生成 {}", JAR_TEST_JAR);
    }

    // 解析java-all-call-graph项目根目录：优先使用系统属性指定的目录，其次使用当前工程的兄弟目录，未找到时返回null
    private String findJacgDir() {
        String jacgDirFromSysProp = System.getProperty(SYS_PROP_JACG_DIR);
        if (StringUtils.isNotBlank(jacgDirFromSysProp)) {
            File jacgDirFromSysPropFile = new File(jacgDirFromSysProp);
            if (jacgDirFromSysPropFile.isDirectory()) {
                return JavaCG2FileUtil.getCanonicalPath(jacgDirFromSysPropFile);
            }
            logger.warn("系统属性 {} 指定的目录不存在 {}", SYS_PROP_JACG_DIR, jacgDirFromSysProp);
        }

        String parentDirPath = JavaCG2FileUtil.getCanonicalPath(new File("..")) + "/";
        for (String jacgDirName : new String[]{"java-all-call-graph", "java-all-call-graph/java-all-call-graph"}) {
            String candidateDirPath = parentDirPath + jacgDirName;
            if (new File(candidateDirPath).isDirectory()) {
                return candidateDirPath;
            }
        }
        return null;
    }

    protected JavaCG2ConfigureWrapper genJavaCG2ConfigureWrapper4JACGTest() {
        return genJavaCG2ConfigureWrapper(true);
    }

    protected JavaCG2ConfigureWrapper genJavaCG2ConfigureWrapper4JavaCg2(String... inputFiles) {
        return genJavaCG2ConfigureWrapper(false, inputFiles);
    }

    protected JavaCG2ConfigureWrapper genJavaCG2ConfigureWrapper(boolean findInJacgDir, String... inputFiles) {
        // 确保本地标准测试jar文件存在，作为java-all-call-graph项目测试jar文件的回退
        genLocalTestJarIfAbsent();
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();
        List<String> jarList = genJarList(findInJacgDir, inputFiles);
        logger.info("需要解析的jar或目录 {}", StringUtils.join(jarList, " "));
        javaCG2ConfigureWrapper.setOtherConfigList(JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR, jarList);

        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_METHOD_CALL_TYPE_VALUE, Boolean.TRUE.toString());
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_FIRST_PARSE_INIT_METHOD_TYPE, Boolean.TRUE.toString());
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_ANALYSE_FIELD_RELATIONSHIP, Boolean.TRUE.toString());
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_CONTINUE_WHEN_ERROR, Boolean.FALSE.toString());
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_LOG_METHOD_SPEND_TIME, Boolean.TRUE.toString());
//        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH,"");
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_FILE_EXT, ".md");
        return javaCG2ConfigureWrapper;
    }

    private List<String> genJarList(boolean findInJacgDir, String... inputFiles) {
        List<String> inputFileList = genInputFileList(inputFiles);
        if (!findInJacgDir) {
            return inputFileList;
        }

        List<String> jarInJacgDirPathList = new ArrayList<>(inputFileList.size());
        for (String inputFile : inputFileList) {
            String jarInJacgDirPath = getJarInJacgDirPath(inputFile);
            jarInJacgDirPathList.add(jarInJacgDirPath);
        }
        return jarInJacgDirPathList;
    }

    // 生成输入文件路径列表，未指定时使用标准测试jar文件
    private List<String> genInputFileList(String... inputFiles) {
        List<String> inputFileList = new ArrayList<>();
        if (ArrayUtils.isEmpty(inputFiles)) {
            inputFileList.add(JAR_TEST_JAR);
        } else {
            inputFileList.addAll(Arrays.asList(inputFiles));
        }
        return inputFileList;
    }

    protected void run(boolean success, boolean findInJacgDir, String... inputFiles) {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper(findInJacgDir, inputFiles);
        if (success && !findInJacgDir) {
            // 期望执行成功时，假如通过profile等生成的输入文件不存在，则跳过对应测试，避免对环境产生硬依赖
            for (String inputFile : genInputFileList(inputFiles)) {
                Assume.assumeTrue("指定的输入文件或目录不存在，跳过对应测试 " + inputFile, new File(inputFile).exists());
            }
        }
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        boolean result = javaCG2Entry.run();
        Assert.assertEquals(success, result);
    }
}
