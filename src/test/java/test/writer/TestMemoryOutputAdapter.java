package test.writer;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchema;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.writer.JavaCG2MemoryOutputAdapter;
import com.adrninistrator.javacg2.writer.JavaCG2MemoryRecordSink;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSink;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 内存输出适配器的单元级测试
 * 验证写行、表头开关、headerSupported=false的文件类型（jar_info）不输出表头、close后写入报错、经内存RecordSource读回逐行一致
 */
public class TestMemoryOutputAdapter {

    /**
     * 验证写行与经内存RecordSource读回逐行一致（表头开关关闭）
     */
    @Test
    public void testWriteRowAndReadBack() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter();
        try (JavaCG2RecordSink recordSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO)) {
            recordSink.writeRow("a.b.Class1", "33", "hash1", "1", "a/b/Class1.class");
            recordSink.writeRow("a.b.Class2", "33", "hash2", "1", "a/b/Class2.class");
        }

        // 收集的全部内容与写入的行一致
        String[] expectLines = new String[]{
                "a.b.Class1\t33\thash1\t1\ta/b/Class1.class",
                "a.b.Class2\t33\thash2\t1\ta/b/Class2.class"
        };
        Assert.assertEquals("收集的内容应与写入的行一致", String.join(JavaCG2Constants.NEW_LINE, Arrays.asList(expectLines)) + JavaCG2Constants.NEW_LINE,
                outputAdapter.getMainRecordContent(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO));

        // 经内存RecordSource读回逐行一致
        JavaCG2RecordSource recordSource = outputAdapter.genRecordSource(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO);
        Assert.assertNotNull("已生成的主结果文件应能获得读回数据来源", recordSource);
        Assert.assertEquals("经内存RecordSource读回的行应与写入的行一致", Arrays.asList(expectLines), readLines4Source(recordSource));
    }

    /**
     * 验证writeRaw写入的内容可经内存RecordSource逐行读回
     */
    @Test
    public void testWriteRawAndReadBack() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter();
        try (JavaCG2RecordSink recordSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_INNER_CLASS)) {
            recordSink.writeRaw("a.b.Outer$a.b.Inner\t1\n");
            recordSink.writeRaw("a.b.Outer$a.b.Inner2\t1\n");
        }

        JavaCG2RecordSource recordSource = outputAdapter.genRecordSource(JavaCG2OutPutFileTypeEnum.OPFTE_INNER_CLASS);
        Assert.assertEquals("经内存RecordSource读回的行应与writeRaw写入的行一致",
                Arrays.asList("a.b.Outer$a.b.Inner\t1", "a.b.Outer$a.b.Inner2\t1"), readLines4Source(recordSource));
    }

    /**
     * 验证表头开关开启时，定义了列结构且允许表头的主结果文件首行写入表头，经内存RecordSource读回时表头行被跳过
     */
    @Test
    public void testHeaderEnableAndSkipInReadBack() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter(true);
        try (JavaCG2RecordSink recordSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO)) {
            recordSink.writeRow("a.b.Class1", "33", "hash1", "1", "a/b/Class1.class");
        }

        JavaCG2OutputFileSchema schema = JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO.getSchema();
        // 收集的内容首行为表头行
        List<String> allLines = readLines(outputAdapter.getMainRecordContent(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO));
        Assert.assertEquals("开启表头开关时收集的内容应有两行（表头行+数据行）", 2, allLines.size());
        Assert.assertEquals("开启表头开关时首行应为表头行", JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO.getSchema().genHeaderLine(), allLines.get(0));

        // 经内存RecordSource读回时表头行被跳过，首行为数据行
        JavaCG2RecordSource recordSource = outputAdapter.genRecordSource(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO);
        Assert.assertEquals("经内存RecordSource读回时应跳过表头行，仅返回数据行",
                Arrays.asList("a.b.Class1\t33\thash1\t1\ta/b/Class1.class"), readLines4Source(recordSource));
    }

    /**
     * 验证表头开关关闭时不输出表头
     */
    @Test
    public void testHeaderDisable() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter(false);
        try (JavaCG2RecordSink recordSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO)) {
            recordSink.writeRow("a.b.Class1", "33", "hash1", "1", "a/b/Class1.class");
        }

        List<String> allLines = readLines(outputAdapter.getMainRecordContent(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO));
        Assert.assertEquals("关闭表头开关时收集的内容应只有数据行", 1, allLines.size());
        Assert.assertFalse("关闭表头开关时不应出现表头行",
                JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO.getSchema().isHeaderLine(allLines.get(0)));
    }

    /**
     * 验证headerSupported=false的文件类型（jar_info）在表头开关开启时也不输出表头
     */
    @Test
    public void testHeaderNotSupported4JarInfo() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter(true);
        Assert.assertFalse("jar_info文件类型不应允许输出表头", JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO.getSchema().isHeaderSupported());
        try (JavaCG2RecordSink recordSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO)) {
            recordSink.writeRow(JavaCG2Constants.FILE_KEY_JAR, "1", "D:/tmp/test.jar", "");
        }
        String expectDataLine = JavaCG2Constants.FILE_KEY_JAR + "\t1\tD:/tmp/test.jar\t";

        List<String> allLines = readLines(outputAdapter.getMainRecordContent(JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO));
        Assert.assertEquals("不允许输出表头的文件类型只应收集数据行", 1, allLines.size());
        Assert.assertEquals("不允许输出表头的文件类型收集的内容应为数据行", expectDataLine, allLines.get(0));

        // 经内存RecordSource读回同样只有数据行
        JavaCG2RecordSource recordSource = outputAdapter.genRecordSource(JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO);
        Assert.assertEquals("不允许输出表头的文件类型经内存RecordSource读回应只有数据行",
                Arrays.asList(expectDataLine), readLines4Source(recordSource));
    }

    /**
     * 验证close后写入报错，close前已收集的数据仍可读回
     */
    @Test
    public void testWriteAfterCloseThrows() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter();
        JavaCG2MemoryRecordSink recordSink = (JavaCG2MemoryRecordSink) outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO);
        recordSink.writeRow("a.b.Class1", "33", "hash1", "1", "a/b/Class1.class");
        recordSink.close();

        // close后写入报错
        try {
            recordSink.writeRow("a.b.Class2", "33", "hash2", "1", "a/b/Class2.class");
            Assert.fail("close后写入应抛出异常");
        } catch (IOException e) {
            Assert.assertTrue("close后写入的报错应说明不允许再写入 " + e.getMessage(), e.getMessage().contains("不允许再写入"));
        }
        try {
            recordSink.writeRaw("a.b.Class2\t33\n");
            Assert.fail("close后writeRaw应抛出异常");
        } catch (IOException e) {
            Assert.assertTrue("close后writeRaw的报错应说明不允许再写入 " + e.getMessage(), e.getMessage().contains("不允许再写入"));
        }

        // close前已收集的数据仍可读回
        Assert.assertEquals("close后已收集的数据仍可读回",
                Arrays.asList("a.b.Class1\t33\thash1\t1\ta/b/Class1.class"),
                readLines4Source(outputAdapter.genRecordSource(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO)));
    }

    /**
     * 验证genOtherRecordSink与genOtherRecordSource，文件名与文件类型枚举匹配时表头行为与主结果文件一致
     */
    @Test
    public void testOtherRecordSinkAndSource() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter(true);
        // 文件名与文件类型枚举匹配（spring_bean），表头开关开启时写入表头
        try (JavaCG2RecordSink recordSink = outputAdapter.genOtherRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_BEAN.getFileName())) {
            recordSink.writeRow("bean1", "0", "a.b.BeanClass", "", "define_in_java", "", "");
        }
        List<String> springBeanLines = readLines(outputAdapter.getOtherRecordContent(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_BEAN.getFileName()));
        Assert.assertEquals("文件名与文件类型枚举匹配时首行应为表头行", JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_BEAN.getSchema().genHeaderLine(), springBeanLines.get(0));

        // genOtherRecordSource读回时跳过表头行
        JavaCG2RecordSource recordSource = outputAdapter.genOtherRecordSource(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_BEAN.getFileName());
        Assert.assertNotNull("已生成的其他结果文件应能获得读回数据来源", recordSource);
        Assert.assertEquals("其他结果文件经内存RecordSource读回时应跳过表头行", 1, readLines4Source(recordSource).size());

        // 文件名与文件类型枚举不匹配时不写表头
        try (JavaCG2RecordSink recordSink = outputAdapter.genOtherRecordSink("jar_diff_modified_classes")) {
            recordSink.writeRow("jar-diff", "jar-diff-2.0.jar", "jar-diff-1.0.jar", "a.b.Class1", "N", "a/b/Class1.class", "2", "", "");
        }
        List<String> jarDiffLines = readLines(outputAdapter.getOtherRecordContent("jar_diff_modified_classes"));
        Assert.assertEquals("文件名与文件类型枚举不匹配时不应写表头，应只有数据行", 1, jarDiffLines.size());

        // 未生成对应的结果文件时返回null
        Assert.assertNull("未生成对应的主结果文件时genRecordSource应返回null",
                outputAdapter.genRecordSource(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL));
        Assert.assertNull("未生成对应的其他结果文件时genOtherRecordSource应返回null", outputAdapter.genOtherRecordSource("not_exists_file"));
    }

    /**
     * 验证同名的结果文件类型再次生成Sink时，数据被替换，与文件实现覆盖同名文件的行为一致
     */
    @Test
    public void testGenSinkAgainReplaceContent() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter();
        JavaCG2RecordSink recordSink1 = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO);
        recordSink1.writeRow("a.b.Class1", "33", "hash1", "1", "a/b/Class1.class");
        recordSink1.close();

        JavaCG2RecordSink recordSink2 = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO);
        recordSink2.writeRow("a.b.Class2", "33", "hash2", "1", "a/b/Class2.class");
        recordSink2.close();

        Assert.assertEquals("再次生成同名结果文件的Sink时应替换此前收集的数据",
                Arrays.asList("a.b.Class2\t33\thash2\t1\ta/b/Class2.class"),
                readLines4Source(outputAdapter.genRecordSource(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO)));
    }

    // 读取全部行内容
    private List<String> readLines(String content) throws IOException {
        try (BufferedReader br = new BufferedReader(new StringReader(content))) {
            List<String> lineList = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lineList.add(line);
            }
            return lineList;
        }
    }

    // 通过内存RecordSource读回全部数据行
    private List<String> readLines4Source(JavaCG2RecordSource recordSource) throws IOException {
        try (BufferedReader br = recordSource.genBufferedReader()) {
            List<String> lineList = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lineList.add(line);
            }
            return lineList;
        }
    }
}
