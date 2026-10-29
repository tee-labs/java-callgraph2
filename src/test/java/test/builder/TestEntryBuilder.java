package test.builder;

import com.adrninistrator.javacg2.builder.JavaCG2ConfigBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2EntryBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2JarDirBuilder;
import com.adrninistrator.javacg2.common.enums.JavaCG2CalleeRawActualEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: 通过Builder生成JavaCG2Entry的测试
 * 需要先在当前项目执行 mvn package -DskipTests -Ptest_gen_jar 命令生成对应jar文件
 */
@Slf4j
public class TestEntryBuilder extends TestBase {

    // 通过Builder正常生成入口类并执行
    @Test
    public void testEntryBuilder() {
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setConfig(JavaCG2ConfigBuilder.builder()
                        .setOutputRootPath(OUT_TEST_DIR)
                        .setOutputFileExt(".md")
                        .setParseMethodCallTypeValue(true)
                        .setHandleCalleeNewRawActual(JavaCG2CalleeRawActualEnum.CRAE_RAW_ACTUAL))
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());
    }

    // 未指定config对应的Builder时使用默认的配置参数
    @Test
    public void testEntryBuilderDefaultConfig() {
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .addJarOrDir(JAR_TEST_JAR)
                .build();
        Assert.assertTrue(javaCG2Entry.run());
    }

    // 使用jar_dir.properties对应的Builder
    @Test
    public void testEntryBuilderUseJarDirBuilder() {
        JavaCG2JarDirBuilder javaCG2JarDirBuilder = JavaCG2JarDirBuilder.builder().addJarOrDir(JAR_TEST_JAR);
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder().setJarDir(javaCG2JarDirBuilder).build();
        Assert.assertTrue(javaCG2Entry.run());
    }

    // 未指定需要解析的jar、war、jmod文件或目录时出现异常
    @Test
    public void testEntryBuilderNoJarDir() {
        try {
            JavaCG2EntryBuilder.builder().build();
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
    }

    // 指定的需要解析的文件不存在时出现异常
    @Test
    public void testEntryBuilderJarNotExists() {
        String notExistsJarPath = "target/not_exists.jar";
        try {
            JavaCG2EntryBuilder.builder().addJarOrDir(notExistsJarPath).build();
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
    }

    // 指定的路径为空时出现异常
    @Test
    public void testEntryBuilderJarPathBlank() {
        try {
            JavaCG2JarDirBuilder.builder().addJarOrDir(" ");
            Assert.fail();
        } catch (JavaCG2ConfigException e) {
            log.info("出现预期的异常 {}", e.getMessage());
        }
    }
}
