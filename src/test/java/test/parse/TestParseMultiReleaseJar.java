package test.parse;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.dto.counter.JavaCG2Counter;
import com.adrninistrator.javacg2.dto.jar.OuterInnerJarPath;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.exceptions.JavaCG2RuntimeException;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import test.base.TestBase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 测试解析multi-release jar文件
 * 在测试执行时通过java.util.zip动态生成一个几KB的合成multi-release jar文件，写入target目录（每次运行重新生成，不提交二进制文件），替代之前使用的体积较大的bcprov jar文件
 * 合成jar文件中的条目：
 * 1 META-INF/MANIFEST.MF：含 Multi-Release: true 属性，用于通过JavaCG2JarUtil.checkMultiReleaseJar方法的判定
 * 2 根目录：JavaCG2Counter.class（会被版本化目录中的同名类替代），OuterInnerJarPath.class（仅存在于根目录）
 * 3 META-INF/versions/9、11、15目录：JavaCG2Counter.class
 * 配置不同的jdk.runtime.major.version参数时对应的版本选择分支（参考MergeJarHandler.getMultiReleaseJarLoadClasses方法）：
 * 1 8：只使用根目录中的class文件
 * 2 9/11/15：使用对应版本化目录中的JavaCG2Counter.class替代根目录中的同名类，仅存在于根目录的OuterInnerJarPath.class仍会使用
 * 3 21：不存在META-INF/versions/21目录，回退使用根目录中的class文件
 *
 * @author adrninistrator
 * @date 2026/10/29
 */
@Slf4j
public class TestParseMultiReleaseJar extends TestBase {

    // 动态生成的合成multi-release jar文件路径，命名与maven产物区分
    private static final String SYNTHETIC_JAR_PATH = "target/test-multi-release-synthetic.jar";

    // 合成jar文件中存在版本化class条目的版本号列表
    private static final int[] VERSIONED_DIR_VERSIONS = new int[]{9, 11, 15};

    // 合成jar文件中根目录与版本化目录共用的class文件
    private static final Class<?> CLASS_IN_VERSIONED_DIR = JavaCG2Counter.class;

    // 合成jar文件中仅存在于根目录的class文件
    private static final Class<?> CLASS_IN_ROOT_ONLY = OuterInnerJarPath.class;

    private String syntheticJarFilePath;

    @Before
    public void init() {
        syntheticJarFilePath = genSyntheticMultiReleaseJar();
        log.info("syntheticJarFilePath {}", syntheticJarFilePath);
    }

    @Test
    public void testMultiReleaseJarVersion8() {
        doTest(8);
    }

    @Test
    public void testMultiReleaseJarVersion9() {
        doTest(9);
    }

    @Test
    public void testMultiReleaseJarVersion11() {
        doTest(11);
    }

    @Test
    public void testMultiReleaseJarVersion15() {
        doTest(15);
    }

    @Test
    public void testMultiReleaseJarVersion21() {
        doTest(21);
    }

    private void doTest(int version) {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(syntheticJarFilePath);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_JDK_RUNTIME_MAJOR_VERSION, String.valueOf(version));
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, syntheticJarFilePath + "@version_" + version);
        Assert.assertTrue(new JavaCG2Entry(javaCG2ConfigureWrapper).run());
    }

    // 生成用于测试的合成multi-release jar文件，每次执行时重新生成
    private String genSyntheticMultiReleaseJar() {
        File jarFile = new File(SYNTHETIC_JAR_PATH);
        if (!JavaCG2FileUtil.isDirectoryExists(jarFile.getAbsoluteFile().getParentFile(), true)) {
            throw new JavaCG2RuntimeException("创建合成jar文件所在目录失败 " + JavaCG2FileUtil.getCanonicalPath(jarFile));
        }

        byte[] classInVersionedDirBytes = readClassBytes(CLASS_IN_VERSIONED_DIR);
        byte[] classInRootOnlyBytes = readClassBytes(CLASS_IN_ROOT_ONLY);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(jarFile))) {
            // META-INF/MANIFEST.MF条目需要写在最前面
            addJarEntry(zipOutputStream, JavaCG2Constants.META_INF_MANIFEST, genManifestBytes());
            // 根目录中的class文件条目
            addJarEntry(zipOutputStream, getClassEntryPath(CLASS_IN_VERSIONED_DIR), classInVersionedDirBytes);
            addJarEntry(zipOutputStream, getClassEntryPath(CLASS_IN_ROOT_ONLY), classInRootOnlyBytes);
            // 版本化目录中的class文件条目，与根目录中的同名class文件对应
            for (int version : VERSIONED_DIR_VERSIONS) {
                addJarEntry(zipOutputStream, JavaCG2Constants.META_INF_VERSIONS + version + JavaCG2Constants.FLAG_SLASH +
                        getClassEntryPath(CLASS_IN_VERSIONED_DIR), classInVersionedDirBytes);
            }
        } catch (IOException e) {
            throw new JavaCG2RuntimeException("生成合成multi-release jar文件失败 " + JavaCG2FileUtil.getCanonicalPath(jarFile), e);
        }
        return JavaCG2FileUtil.getCanonicalPath(jarFile);
    }

    // 生成META-INF/MANIFEST.MF条目的内容，需要符合Manifest文件格式以通过checkMultiReleaseJar方法的判定
    private byte[] genManifestBytes() {
        StringBuilder stringBuilder = new StringBuilder();
        // Manifest文件格式要求Manifest-Version为第一个属性
        stringBuilder.append("Manifest-Version: 1.0").append(JavaCG2Constants.NEW_LINE_WINDOWS);
        // 通过Multi-Release属性判断为multi-release jar文件
        stringBuilder.append("Multi-Release: true").append(JavaCG2Constants.NEW_LINE_WINDOWS);
        // Manifest文件格式要求主属性区最后以空行结束
        stringBuilder.append(JavaCG2Constants.NEW_LINE_WINDOWS);
        return stringBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 通过classloader读取class文件的字节数组
    private byte[] readClassBytes(Class<?> clazz) {
        String classEntryPath = getClassEntryPath(clazz);
        try (InputStream inputStream = clazz.getClassLoader().getResourceAsStream(classEntryPath);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                throw new JavaCG2RuntimeException("class文件不存在 " + classEntryPath);
            }
            byte[] buffer = new byte[8192];
            int readLen;
            while ((readLen = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, readLen);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new JavaCG2RuntimeException("读取class文件失败 " + classEntryPath, e);
        }
    }

    // 获取class文件在jar文件中的路径
    private String getClassEntryPath(Class<?> clazz) {
        return clazz.getName().replace('.', JavaCG2Constants.CHAR_SLASH) + JavaCG2Constants.EXT_CLASS;
    }

    // 向jar文件中写入一个条目
    private void addJarEntry(ZipOutputStream zipOutputStream, String entryName, byte[] bytes) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(bytes);
        zipOutputStream.closeEntry();
    }
}
