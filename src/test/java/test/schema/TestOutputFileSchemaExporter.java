package test.schema;

import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchemaExporter;
import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchemaInfo;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2026/11/2
 * @description: 输出文件列结构（schema）导出类的测试
 */
public class TestOutputFileSchemaExporter {

    @Test
    public void testGenSchemaInfoList() {
        JavaCG2OutputFileSchemaExporter exporter = new JavaCG2OutputFileSchemaExporter();
        List<JavaCG2OutputFileSchemaInfo> schemaInfoList = exporter.genSchemaInfoList();

        // 数量与枚举中定义了列结构的常量数量一致
        int expectedNum = 0;
        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : JavaCG2OutPutFileTypeEnum.values()) {
            if (fileTypeEnum.getSchema() != null) {
                expectedNum++;
            }
        }
        Assert.assertEquals(expectedNum, schemaInfoList.size());
        Assert.assertTrue("定义了列结构的文件类型数量异常 " + expectedNum, expectedNum > 0);

        for (JavaCG2OutputFileSchemaInfo schemaInfo : schemaInfoList) {
            Assert.assertTrue("文件名不允许为空", StringUtils.isNotBlank(schemaInfo.getFileName()));
            Assert.assertTrue("文件描述不允许为空", StringUtils.isNotBlank(schemaInfo.getDesc()));
            Assert.assertTrue("列数异常 " + schemaInfo.getFileName() + " " + schemaInfo.getColumnNum(), schemaInfo.getColumnNum() > 0);
            Assert.assertTrue("最小列数异常 " + schemaInfo.getFileName() + " " + schemaInfo.getMinColumnNum(),
                    schemaInfo.getMinColumnNum() >= 1 && schemaInfo.getMinColumnNum() <= schemaInfo.getColumnNum());
            Assert.assertEquals("列名数量与列数不一致 " + schemaInfo.getFileName(), schemaInfo.getColumnNum(), schemaInfo.getColumns().size());
        }
    }

    @Test
    public void testGenMarkdown() {
        JavaCG2OutputFileSchemaExporter exporter = new JavaCG2OutputFileSchemaExporter();

        // 未定义列结构的文件类型返回null
        Assert.assertNull(exporter.genMarkdown(JavaCG2OutPutFileTypeEnum.OPFTE_LOG_METHOD_SPEND_TIME));
        Assert.assertNull(exporter.genMarkdown(JavaCG2OutPutFileTypeEnum.OPFTE_ILLEGAL));

        // 注解类文件：最小列数小于列数
        String classAnnotationMarkdown = exporter.genMarkdown(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION);
        Assert.assertTrue(classAnnotationMarkdown.contains(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION.getFileName()));
        Assert.assertTrue(classAnnotationMarkdown.contains("列数: 4"));
        Assert.assertTrue(classAnnotationMarkdown.contains("最小列数: 2"));
        Assert.assertTrue(classAnnotationMarkdown.contains("|0|完整类名|"));
        Assert.assertTrue(classAnnotationMarkdown.contains("|3|注解属性值|"));

        // jar_info：定义了列结构但不允许输出表头
        String jarInfoMarkdown = exporter.genMarkdown(JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO);
        Assert.assertTrue(jarInfoMarkdown.contains("是否允许表头: 否"));
        Assert.assertFalse(jarInfoMarkdown.contains("表头行内容"));

        // 允许表头的文件：表头行内容中各列名称以tab分隔
        String classInfoMarkdown = exporter.genMarkdown(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO);
        Assert.assertTrue(classInfoMarkdown.contains("是否允许表头: 是"));
        Assert.assertTrue(classInfoMarkdown.contains("完整类名\t类的access_flags\t类文件的HASH值\t类所在的jar文件序号\t类在jar包中的路径"));

        // 全部markdown内容包含每个定义了列结构的文件名
        String allMarkdown = exporter.genAllMarkdown();
        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : JavaCG2OutPutFileTypeEnum.values()) {
            if (fileTypeEnum.getSchema() == null) {
                Assert.assertFalse(allMarkdown.contains(fileTypeEnum.getFileName()));
                continue;
            }
            Assert.assertTrue("全部markdown内容中缺少文件 " + fileTypeEnum.getFileName(), allMarkdown.contains(fileTypeEnum.getFileName()));
        }
    }

    @Test
    public void testWriteToFile() throws IOException {
        JavaCG2OutputFileSchemaExporter exporter = new JavaCG2OutputFileSchemaExporter();
        File mdFile = new File("target/output_file_schema.md");
        Assert.assertTrue(mdFile.delete() || !mdFile.exists());

        // 写出到文件，内容与genAllMarkdown()一致
        exporter.writeToFile(JavaCG2FileUtil.getCanonicalPath(mdFile));
        Assert.assertTrue("未生成markdown文件 " + JavaCG2FileUtil.getCanonicalPath(mdFile), mdFile.isFile());
        String fileContent = new String(Files.readAllBytes(mdFile.toPath()), StandardCharsets.UTF_8);
        Assert.assertEquals(exporter.genAllMarkdown(), fileContent);
    }
}
