package test.entry;

import com.adrninistrator.javacg2.builder.JavaCG2EntryBuilder;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;

/**
 * @author adrninistrator
 * @date 2026/2/16
 * @description: 手工运行示例：通过JavaCG2EntryBuilder指定需要解析的jar文件并执行
 * 需要先通过test_gen_jar profile生成target/test.jar（mvn package -DskipTests -Ptest_gen_jar）
 */
public class TestJavaCG2Entry {

    public static void main(String[] args) {
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .addJarOrDir("target/test.jar")
                .build();
        javaCG2Entry.run();
    }
}
