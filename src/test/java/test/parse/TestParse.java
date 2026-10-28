package test.parse;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import test.base.TestBase;

import java.io.File;

/**
 * @author adrninistrator
 * @date 2025/2/2
 * @description:
 */
public class TestParse extends TestBase {

    /*
        正常解析
        target/test.jar在mvn test过程中由maven-antrun-plugin自动生成，也可以先在当前项目执行 mvn package -DskipTests -Ptest_gen_jar 命令生成
     */
    @Test
    public void testParseJavaCG2TestLib() {
        genLocalTestJarIfAbsent();
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();
        javaCG2ConfigureWrapper.setOtherConfigList(JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR, JAR_TEST_JAR);
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());
    }

    /*
        正常解析
        优先使用java-all-call-graph项目通过 gradlew test_gen_jar 生成的对应jar文件，未提供时回退为当前工程的target/test.jar
     */
    @Test
    public void testParseJacgTestLib() {
        run(true, true);
    }

    // 不解析被调用对象和参数可能的类型与值
    @Test
    public void testParseNoMCTypeValue() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JACGTest();
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_METHOD_CALL_TYPE_VALUE, Boolean.FALSE.toString());
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());
    }

    // 使用Jar兼容性检查模式，仅解析基础信息
    @Test
    public void testParseJarCompatibilityMode() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JACGTest();
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_JAR_COMPATIBILITY_MODE, Boolean.TRUE.toString());
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());
    }

    // 仅解析类信息
    @Test
    public void testParseOnlyClassMode() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JACGTest();
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_ONLY_CLASS_MODE, Boolean.TRUE.toString());
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());
    }

    /*
        解析包含jar文件的jar文件
        target/jar_output.jar需要先执行 mvn package -DskipTests -Pgen_run_jar,gen_jar_in_jar 命令生成，未提供时跳过测试
     */
    @Test
    public void testJarInJar() {
        Assume.assumeTrue("指定的输入文件或目录不存在，跳过对应测试 " + JAR_OUTPUT_JAR, new File(JAR_OUTPUT_JAR).exists());
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_OUTPUT_JAR);
        Assert.assertTrue(new JavaCG2Entry(javaCG2ConfigureWrapper).run());
    }
}
