package test.parse;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import test.base.TestBase;

import java.io.File;

/**
 * @author adrninistrator
 * @date 2025/7/6
 * @description:
 */
public class TestMergeFatJar extends TestBase {

    /*
        合并fat jar，普通jar文件
        优先使用java-all-call-graph项目通过 gradlew test_gen_jar 生成的对应jar文件，未提供时回退为当前工程的target/test.jar
     */
    @Test
    public void testMergeFatJarNormal() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JACGTest();
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_MERGE_SEPARATE_FAT_JAR, Boolean.TRUE.toString());
        Assert.assertTrue(new JavaCG2Entry(javaCG2ConfigureWrapper).run());
    }

    /*
        合并fat jar，包含jar文件的jar文件
        target/jar_output.jar需要先执行 mvn package -DskipTests -Pgen_run_jar,gen_jar_in_jar 命令生成，未提供时跳过测试
     */
    @Test
    public void testMergeFatJarInJar() {
        Assume.assumeTrue("指定的输入文件或目录不存在，跳过对应测试 " + JAR_OUTPUT_JAR, new File(JAR_OUTPUT_JAR).exists());
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper(false, JAR_OUTPUT_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_MERGE_SEPARATE_FAT_JAR, Boolean.TRUE.toString());
        Assert.assertTrue(new JavaCG2Entry(javaCG2ConfigureWrapper).run());
    }

    /*
        合并fat jar，war文件
        target/jar_output.war需要先执行 mvn package -DskipTests -Pgen_run_jar,gen_jar_in_war 命令生成，未提供时跳过测试
     */
    @Test
    public void testMergeFatJarWar() {
        Assume.assumeTrue("指定的输入文件或目录不存在，跳过对应测试 " + JAR_OUTPUT_WAR, new File(JAR_OUTPUT_WAR).exists());
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper(false, JAR_OUTPUT_WAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_MERGE_SEPARATE_FAT_JAR, Boolean.TRUE.toString());
        Assert.assertTrue(new JavaCG2Entry(javaCG2ConfigureWrapper).run());
    }

    /*
        合并fat jar，Spring Boot jar文件
        需要先在java-all-call-graph项目执行 gradlew bootJar -b spring_boot.gradle 命令生成对应jar文件，无法用当前工程的文件替代，未提供时跳过测试
     */
    @Test
    public void testMergeFatJarSPBJar() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper(true, "build/spring-boot/java-all-call-graph.jar");
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_MERGE_SEPARATE_FAT_JAR, Boolean.TRUE.toString());
        Assert.assertTrue(new JavaCG2Entry(javaCG2ConfigureWrapper).run());
    }
}
