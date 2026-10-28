package test.parse;

import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import test.base.TestBase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author adrninistrator
 * @date 2025/2/3
 * @description: 验证不合法的输入：直接指定.class文件不允许解析
 * 通过TemporaryFolder在每次测试独立的临时目录中生成真实存在的.class文件作为输入，
 * 使测试不依赖编译输出目录的位置（maven为target/test-classes、IDE可能为其他目录），也不受用例执行顺序与残留文件的影响
 */
public class TestNotAllowed extends TestBase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testSingleClass() throws IOException {
        run(false, false, genClassFile("TestNotAllowedSingle.class"));
    }

    @Test
    public void testMultiClass() throws IOException {
        run(false, false,
                genClassFile("TestNotAllowedMulti1.class"),
                genClassFile("TestNotAllowedMulti2.class")
        );
    }

    // 在临时目录中生成以合法的class文件头开始的.class文件，工具在校验文件后缀时即会拒绝，不会读取文件内容
    private String genClassFile(String fileName) throws IOException {
        File classFile = temporaryFolder.newFile(fileName);
        Files.write(classFile.toPath(), new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
        return JavaCG2FileUtil.getCanonicalPath(classFile);
    }
}
