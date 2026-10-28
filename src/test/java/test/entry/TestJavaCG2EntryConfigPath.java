package test.entry;

import com.adrninistrator.javacg2.builder.JavaCG2ConfigBuilder;
import com.adrninistrator.javacg2.builder.JavaCG2EntryBuilder;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;

/**
 * @author adrninistrator
 * @date 2026/2/16
 * @description: 手工运行示例：通过JavaCG2ConfigBuilder在代码中设置个别配置参数后执行
 * 原先通过new JavaCG2ConfigureWrapper(false, "src/main/resources")以文件模式指定配置文件根目录的用法已废弃：
 * 配置文件不再随工程提供，配置参数统一通过JavaCG2ConfigBuilder等Builder在代码中指定，未设置的参数使用默认值
 * 需要先通过test_gen_jar profile生成target/test.jar（mvn package -DskipTests -Ptest_gen_jar）
 */
public class TestJavaCG2EntryConfigPath {

    public static void main(String[] args) {
        JavaCG2ConfigBuilder javaCG2ConfigBuilder = JavaCG2ConfigBuilder.builder()
                .setParseMethodCallTypeValue(true);
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .setConfig(javaCG2ConfigBuilder)
                .addJarOrDir("target/test.jar")
                .build();
        javaCG2Entry.run();
    }
}
