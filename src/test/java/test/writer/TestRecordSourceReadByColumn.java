package test.writer;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchema;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.dto.output.JavaCG2OutputInfo;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import com.adrninistrator.javacg2.writer.JavaCG2FileOutputAdapter;
import com.adrninistrator.javacg2.writer.JavaCG2FileRecordSource;
import com.adrninistrator.javacg2.writer.JavaCG2MemoryOutputAdapter;
import com.adrninistrator.javacg2.writer.JavaCG2MemoryRecordSource;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSink;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2026/11/2
 * @description: RecordSource按列名读取的端到端测试
 * 验证文件（JavaCG2FileRecordSource）与内存（JavaCG2MemoryRecordSource）两个实现的getSchema()返回正确的列结构，
 * 并演示、断言“split数据行后按getSchema().getColumnIndex(列名)取列”的读回方式端到端可用：
 * 1 表头开关开启与关闭两种场景下，按列名取列的结果一致（文件模式生成表头开、关两组产物对比同一行数据）
 * 2 genBufferedReader读回时表头行由实现内部跳过，表头开关不影响数据行内容
 * 3 可变行宽文件（如class_annotation，注解没有属性值时省略最后两列）通过checkColumnNumValid校验列数
 * 4 扩展文件（持有文件名而非文件类型枚举）及未定义列结构的文件类型，getSchema()返回null
 * 5 自写裸读器场景：通过schema.isHeaderLine判断并跳过表头行、checkColumnNumValid校验列数后按列名取列
 * 文件模式的产物使用JavaCG2FileOutputAdapter生成在target目录，不依赖完整解析执行
 */
@Slf4j
public class TestRecordSourceReadByColumn {

    private static final String FILE_OUTPUT_DIR_HEADER_OFF = "target/record_source_test/header_off";

    private static final String FILE_OUTPUT_DIR_HEADER_ON = "target/record_source_test/header_on";

    // class_annotation无属性值的数据行（省略“注解属性名称”“注解属性值”两列）
    private static final String[] CLASS_ANNOTATION_ROW_WITHOUT_ATTRIBUTE = new String[]{"a.b.ClassA", "Lcom/example/AnnotationA;"};

    // class_annotation有属性值的数据行
    private static final String[] CLASS_ANNOTATION_ROW_WITH_ATTRIBUTE = new String[]{"a.b.ClassB", "Lcom/example/AnnotationB;", "value", "test1"};

    // method_call数据行，各列取值与列结构定义顺序一致
    private static final String[] METHOD_CALL_ROW = new String[]{
            "1", "1", "a.b.ClassC.main([Ljava/lang/String;)V", "it:a.b.ClassD.print()V", "10", "void", "0", "t", "void", "void", "1", "1", ""};

    @BeforeClass
    public static void init() {
        createDir(FILE_OUTPUT_DIR_HEADER_OFF);
        createDir(FILE_OUTPUT_DIR_HEADER_ON);
    }

    /**
     * 验证内存实现getSchema()返回正确列结构，表头关闭时按列名取列端到端可用（含可变行宽行）
     */
    @Test
    public void testMemoryGetSchemaAndReadByColumnHeaderOff() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter(false);
        writeClassAnnotationRows(outputAdapter);

        JavaCG2RecordSource recordSource = outputAdapter.genRecordSource(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION);
        Assert.assertNotNull("已生成的结果文件应能获得读回数据来源", recordSource);
        Assert.assertSame("内存实现的getSchema()应返回文件类型枚举上定义的列结构",
                JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION.getSchema(), recordSource.getSchema());

        List<String[]> dataRows = readRowsByColumnName(recordSource);
        Assert.assertEquals("内存实现读回的数据行数量应为2", 2, dataRows.size());
        assertClassAnnotationRow(dataRows.get(0), CLASS_ANNOTATION_ROW_WITHOUT_ATTRIBUTE);
        assertClassAnnotationRow(dataRows.get(1), CLASS_ANNOTATION_ROW_WITH_ATTRIBUTE);
    }

    /**
     * 验证内存实现表头开启时genBufferedReader跳过表头行，按列名取列结果与表头关闭时一致
     */
    @Test
    public void testMemoryGetSchemaAndReadByColumnHeaderOn() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter(true);
        writeClassAnnotationRows(outputAdapter);

        // 原始内容首行为表头行
        String content = outputAdapter.getMainRecordContent(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION);
        JavaCG2OutputFileSchema schema = JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION.getSchema();
        Assert.assertTrue("表头开启时原始内容首行应为表头行", schema.isHeaderLine(content.substring(0, content.indexOf(JavaCG2Constants.NEW_LINE))));

        JavaCG2RecordSource recordSource = outputAdapter.genRecordSource(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION);
        Assert.assertSame("内存实现的getSchema()应返回文件类型枚举上定义的列结构",
                JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION.getSchema(), recordSource.getSchema());

        List<String[]> dataRows = readRowsByColumnName(recordSource);
        Assert.assertEquals("内存实现读回的数据行数量应为2（表头行被跳过）", 2, dataRows.size());
        assertClassAnnotationRow(dataRows.get(0), CLASS_ANNOTATION_ROW_WITHOUT_ATTRIBUTE);
        assertClassAnnotationRow(dataRows.get(1), CLASS_ANNOTATION_ROW_WITH_ATTRIBUTE);
    }

    /**
     * 验证文件实现getSchema()返回正确列结构，生成表头开、关两组文件产物，按列名取列同一行数据的结果一致
     */
    @Test
    public void testFileGetSchemaAndReadByColumnHeaderOnOff() throws IOException {
        JavaCG2OutputFileSchema schema = JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL.getSchema();

        // 表头关闭的文件产物
        String filePathHeaderOff = writeMethodCallFile(FILE_OUTPUT_DIR_HEADER_OFF, false);
        JavaCG2RecordSource recordSourceHeaderOff = new JavaCG2FileRecordSource(filePathHeaderOff, JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL);
        Assert.assertSame("文件实现的getSchema()应返回文件类型枚举上定义的列结构", schema, recordSourceHeaderOff.getSchema());
        List<String[]> dataRowsHeaderOff = readRowsByColumnName(recordSourceHeaderOff);
        Assert.assertEquals("表头关闭的文件产物读回的数据行数量应为1", 1, dataRowsHeaderOff.size());

        // 表头开启的文件产物，文件首行为表头行
        String filePathHeaderOn = writeMethodCallFile(FILE_OUTPUT_DIR_HEADER_ON, true);
        JavaCG2RecordSource recordSourceHeaderOn = new JavaCG2FileRecordSource(filePathHeaderOn, JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL);
        Assert.assertSame("文件实现的getSchema()应返回文件类型枚举上定义的列结构", schema, recordSourceHeaderOn.getSchema());
        List<String> lineListHeaderOn = JavaCG2FileUtil.readFile2List(filePathHeaderOn);
        Assert.assertEquals("表头开启的文件产物应有两行（表头行+数据行）", 2, lineListHeaderOn.size());
        Assert.assertEquals("表头开启的文件产物首行应为表头行", schema.genHeaderLine(), lineListHeaderOn.get(0));
        List<String[]> dataRowsHeaderOn = readRowsByColumnName(recordSourceHeaderOn);
        Assert.assertEquals("表头开启的文件产物读回的数据行数量应为1（表头行被跳过）", 1, dataRowsHeaderOn.size());

        // 表头开、关两组产物中同一行数据按列名取列的结果一致
        Assert.assertArrayEquals("表头开、关两组文件产物按列名取列的结果应一致", dataRowsHeaderOff.get(0), dataRowsHeaderOn.get(0));
        assertMethodCallRow(dataRowsHeaderOn.get(0));
    }

    /**
     * 验证经输出适配器genRecordSource读回文件产物时，getSchema()与按列名取列同样可用
     */
    @Test
    public void testFileAdapterGenRecordSourceReadByColumn() throws IOException {
        writeMethodCallFile(FILE_OUTPUT_DIR_HEADER_OFF, false);
        JavaCG2OutputInfo javaCG2OutputInfo = new JavaCG2OutputInfo(FILE_OUTPUT_DIR_HEADER_OFF, JavaCG2Constants.EXT_MD);
        JavaCG2FileOutputAdapter fileOutputAdapter = new JavaCG2FileOutputAdapter(javaCG2OutputInfo);

        JavaCG2RecordSource recordSource = fileOutputAdapter.genRecordSource(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL);
        Assert.assertNotNull("文件输出适配器应能获得读回数据来源", recordSource);
        List<String[]> dataRows = readRowsByColumnName(recordSource);
        Assert.assertEquals("经文件输出适配器读回的数据行数量应为1", 1, dataRows.size());
        assertMethodCallRow(dataRows.get(0));
    }

    /**
     * 验证扩展文件（持有文件名而非文件类型枚举）及未定义列结构的文件类型，getSchema()返回null
     */
    @Test
    public void testGetSchemaNull() throws IOException {
        // 扩展文件：文件名与文件类型枚举不匹配，内存读回实现持有文件名
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter(false);
        try (JavaCG2RecordSink recordSink = outputAdapter.genOtherRecordSink("jar_diff_modified_classes")) {
            recordSink.writeRow("jar-diff", "new.jar", "old.jar", "a.b.Class1", "N", "a/b/Class1.class", "2", "", "");
        }
        JavaCG2RecordSource otherRecordSource = outputAdapter.genOtherRecordSource("jar_diff_modified_classes");
        Assert.assertNotNull("已生成的扩展文件应能获得读回数据来源", otherRecordSource);
        Assert.assertNull("扩展文件无固定列结构，getSchema()应返回null", otherRecordSource.getSchema());

        // 直接以null列结构构造的内存读回实现
        JavaCG2MemoryRecordSource nullSchemaRecordSource = new JavaCG2MemoryRecordSource("line1\n", (JavaCG2OutputFileSchema) null);
        Assert.assertNull("列结构为null时getSchema()应返回null", nullSchemaRecordSource.getSchema());

        // 文件类型未定义列结构（无固定列结构的文件）
        String filePath = JavaCG2FileUtil.genFilePath(FILE_OUTPUT_DIR_HEADER_OFF, JavaCG2OutPutFileTypeEnum.OPFTE_LOG_METHOD_SPEND_TIME.getFileName(), JavaCG2Constants.EXT_MD);
        JavaCG2RecordSource recordSource = new JavaCG2FileRecordSource(filePath, JavaCG2OutPutFileTypeEnum.OPFTE_LOG_METHOD_SPEND_TIME);
        Assert.assertNull("未定义列结构的文件类型getSchema()应返回null", recordSource.getSchema());
    }

    /**
     * 验证自写裸读器场景：通过schema.isHeaderLine判断并跳过表头行、checkColumnNumValid校验列数后按列名取列
     */
    @Test
    public void testRawReaderWithHeaderAndColumnNumCheck() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter(true);
        writeClassAnnotationRows(outputAdapter);

        // 自写裸读器不使用RecordSource，直接处理原始内容，需要自行处理表头行
        JavaCG2OutputFileSchema schema = JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION.getSchema();
        List<String[]> dataRows = new ArrayList<>();
        List<String> lineList = readRawLines(outputAdapter.getMainRecordContent(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION));
        for (int i = 0; i < lineList.size(); i++) {
            String line = lineList.get(i);
            if (i == 0 && schema.isHeaderLine(line)) {
                // 首行为表头行时跳过，开启表头时存在，关闭时不存在
                continue;
            }
            if (StringUtils.isBlank(line)) {
                // 跳过空行
                continue;
            }
            String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
            Assert.assertTrue("数据行列数应在最小列数与列数之间 " + line, schema.checkColumnNumValid(columns.length));
            dataRows.add(columns);
        }
        Assert.assertEquals("裸读器解析的数据行数量应为2", 2, dataRows.size());
        assertClassAnnotationRow(dataRows.get(0), CLASS_ANNOTATION_ROW_WITHOUT_ATTRIBUTE);
        assertClassAnnotationRow(dataRows.get(1), CLASS_ANNOTATION_ROW_WITH_ATTRIBUTE);
    }

    // 创建目录，已存在时跳过
    private static void createDir(String dirPath) {
        File dir = new File(dirPath);
        if (dir.isDirectory()) {
            return;
        }
        Assert.assertTrue("创建目录失败 " + JavaCG2FileUtil.getCanonicalPath(dir), dir.mkdirs());
    }

    // 通过内存输出适配器写class_annotation文件的数据行
    private void writeClassAnnotationRows(JavaCG2MemoryOutputAdapter outputAdapter) throws IOException {
        try (JavaCG2RecordSink recordSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION)) {
            recordSink.writeRow(CLASS_ANNOTATION_ROW_WITHOUT_ATTRIBUTE);
            recordSink.writeRow(CLASS_ANNOTATION_ROW_WITH_ATTRIBUTE);
        }
    }

    // 通过文件输出适配器生成method_call文件产物，返回文件路径
    private String writeMethodCallFile(String outputDirPath, boolean headerEnable) throws IOException {
        JavaCG2OutputInfo javaCG2OutputInfo = new JavaCG2OutputInfo(outputDirPath, JavaCG2Constants.EXT_MD);
        javaCG2OutputInfo.setOutputFileHeaderEnable(headerEnable);
        JavaCG2FileOutputAdapter fileOutputAdapter = new JavaCG2FileOutputAdapter(javaCG2OutputInfo);
        try (JavaCG2RecordSink recordSink = fileOutputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL)) {
            recordSink.writeRow(METHOD_CALL_ROW);
        }
        return javaCG2OutputInfo.getMainFilePath(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL);
    }

    // 经RecordSource读回全部数据行，演示按列名取列的标准读回方式
    private List<String[]> readRowsByColumnName(JavaCG2RecordSource recordSource) throws IOException {
        JavaCG2OutputFileSchema schema = recordSource.getSchema();
        Assert.assertNotNull("getSchema()应返回列结构", schema);
        List<String[]> dataRows = new ArrayList<>();
        try (BufferedReader bufferedReader = recordSource.genBufferedReader()) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                Assert.assertTrue("数据行列数应在最小列数与列数之间 " + line, schema.checkColumnNumValid(columns.length));
                dataRows.add(columns);
            }
        }
        return dataRows;
    }

    // 断言class_annotation数据行按列名取列的结果
    private void assertClassAnnotationRow(String[] columns, String[] expectColumns) {
        JavaCG2OutputFileSchema schema = JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION.getSchema();
        Assert.assertEquals("完整类名列取值不一致", expectColumns[0], getColumn(columns, schema, "完整类名"));
        Assert.assertEquals("注解类名列取值不一致", expectColumns[1], getColumn(columns, schema, "注解类名"));
        Assert.assertEquals("注解属性名称列取值不一致", expectColumns.length > 2 ? expectColumns[2] : "",
                getColumn(columns, schema, "注解属性名称"));
        Assert.assertEquals("注解属性值列取值不一致", expectColumns.length > 3 ? expectColumns[3] : "",
                getColumn(columns, schema, "注解属性值"));
    }

    // 断言method_call数据行按列名取列的结果
    private void assertMethodCallRow(String[] columns) {
        JavaCG2OutputFileSchema schema = JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL.getSchema();
        Assert.assertEquals("方法调用序号列取值不一致", "1", getColumn(columns, schema, "方法调用序号"));
        Assert.assertEquals("是否启用列取值不一致", "1", getColumn(columns, schema, "是否启用"));
        Assert.assertEquals("调用方完整方法列取值不一致", "a.b.ClassC.main([Ljava/lang/String;)V", getColumn(columns, schema, "调用方完整方法"));
        Assert.assertEquals("方法调用类型及被调用方完整方法列取值不一致", "it:a.b.ClassD.print()V",
                getColumn(columns, schema, "方法调用类型及被调用方完整方法"));
        Assert.assertEquals("调用方法源代码行号列取值不一致", "10", getColumn(columns, schema, "调用方法源代码行号"));
        Assert.assertEquals("描述信息列取值不一致", "", getColumn(columns, schema, "描述信息"));
    }

    // 按列名取列，列下标超出数据行列数时（可变行宽行省略的末尾列）视为空字符串
    private String getColumn(String[] columns, JavaCG2OutputFileSchema schema, String columnName) {
        int index = schema.getColumnIndex(columnName);
        Assert.assertTrue("列名未在列结构中定义 " + columnName, index >= 0);
        if (index >= columns.length) {
            return "";
        }
        return columns[index];
    }

    // 读取原始内容的全部行
    private List<String> readRawLines(String content) {
        return Arrays.asList(content.split(JavaCG2Constants.NEW_LINE));
    }
}
