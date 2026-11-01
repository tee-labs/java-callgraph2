package test.schema;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchema;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 结果文件列结构与JavaCG2OutPutFileTypeEnum中定义的列结构的一致性测试
 * 在一次真实解析后，对所有定义了列结构的输出文件逐行校验列数与列结构一致（表头行除外）
 * 未来任何输出文件的列数与列结构定义不一致（schema漂移）都会被当前测试发现
 * 输出目录使用target目录，避免影响out/test目录中的文件
 */
@Slf4j
public class TestOutputFileColumnSchema extends TestBase {

    // 输出根目录，表头开关使用默认值（关闭）
    private static final String OUTPUT_ROOT_PATH = "target/schema_check";

    @Test
    public void testColumnNumMatchSchema() throws IOException {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUTPUT_ROOT_PATH);

        // 执行前清理输出目录，避免残留文件影响校验
        deleteDir(new File(OUTPUT_ROOT_PATH));

        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());

        File outputDir = new File(OUTPUT_ROOT_PATH, new File(JAR_TEST_JAR).getName() + "-output_javacg2");
        Assert.assertTrue("未找到输出目录 " + JavaCG2FileUtil.getCanonicalPath(outputDir), outputDir.isDirectory());

        int existsFileNum = 0;
        int checkLineNum = 0;
        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : JavaCG2OutPutFileTypeEnum.values()) {
            JavaCG2OutputFileSchema schema = fileTypeEnum.getSchema();
            if (schema == null) {
                // 未定义列结构的文件类型不校验
                continue;
            }
            File outputFile = new File(outputDir, fileTypeEnum.getFileName() + JavaCG2Constants.EXT_MD);
            if (!outputFile.exists()) {
                // 当前解析未生成对应文件时跳过
                continue;
            }
            existsFileNum++;

            try (BufferedReader br = JavaCG2FileUtil.genBufferedReader(outputFile)) {
                String line;
                int lineNum = 0;
                while ((line = br.readLine()) != null) {
                    lineNum++;
                    if (StringUtils.isBlank(line)) {
                        continue;
                    }
                    // 表头关闭时不允许出现表头行
                    Assert.assertFalse("表头关闭时输出文件中出现表头行 " + outputFile.getPath() + " 第" + lineNum + "行", schema.isHeaderLine(line));

                    String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                    Assert.assertTrue("输出文件列数与列结构定义不一致，文件: " + outputFile.getPath() + " 第" + lineNum + "行，实际列数: " + columns.length
                                    + "，允许的列数范围: " + schema.getMinColumnNum() + "-" + schema.getColumnNum() + "，行内容: " + line,
                            schema.checkColumnNumValid(columns.length));
                }
                checkLineNum += lineNum;
            }
        }
        log.info("校验了定义列结构的输出文件数量: {} 总行数: {}", existsFileNum, checkLineNum);
        Assert.assertTrue("未找到定义了列结构的输出文件，解析可能未正常执行", existsFileNum > 0);
        // 存在数据行才有效
        Assert.assertTrue("定义了列结构的输出文件中没有数据行，解析可能未正常执行", checkLineNum > 0);
    }

    // 删除文件或目录
    private void deleteDir(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles != null) {
                for (File childFile : listFiles) {
                    deleteDir(childFile);
                }
            }
        }
        Assert.assertTrue("删除文件失败 " + JavaCG2FileUtil.getCanonicalPath(file), JavaCG2FileUtil.deleteFile(file));
    }
}
