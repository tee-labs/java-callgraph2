package test.entry;

import com.adrninistrator.javacg2.builder.JavaCG2EntryBuilder;
import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;

/**
 * @author adrninistrator
 * @date 2026/2/16
 * @description: 手工运行示例：通过JVM属性指定输入根目录，并通过JavaCG2EntryBuilder指定需要解析的jar文件后执行
 * 需要先通过test_gen_jar profile生成target/test.jar（mvn package -DskipTests -Ptest_gen_jar）
 */
public class TestJavaCG2EntryJVM {

    public static void main(String[] args) {
        System.setProperty(JavaCG2Constants.JVM_PROP_KEY_INPUT_ROOT_PATH, ".");
        JavaCG2Entry javaCG2Entry = JavaCG2EntryBuilder.builder()
                .addJarOrDir("target/test.jar")
                .build();
        javaCG2Entry.run();
    }
}
