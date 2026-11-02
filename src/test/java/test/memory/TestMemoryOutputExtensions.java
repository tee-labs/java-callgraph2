package test.memory;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.extensions.codeparser.AbstractSaveData2FileParser;
import com.adrninistrator.javacg2.writer.JavaCG2MemoryOutputAdapter;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2026/11/2
 * @description: 纯内存输出模式下扩展解析器写出的结果文件的可读回性测试
 * 扩展解析器经outputAdapter.genOtherRecordSink()方法写出的结果文件在纯内存模式下保存在内存中，可从内存侧读回：
 * 1 内置的Spring XML定时任务/AOP/组件扫描信息解析类（JavaCG2SpringXmlTaskAopScanParser）写出的5个其他类型结果文件
 * 2 通过ExtensionsManager.addCodeParser()方法注册的自定义扩展解析器写出的结果文件
 * 内置的Spring XML Bean解析类（JavaCG2SpringXmlBeanParser）不直接写出文件，其解析的数据由Spring Bean处理类经主结果文件写出
 */
@Slf4j
public class TestMemoryOutputExtensions extends TestBase {

    // 内置的Spring XML定时任务/AOP/组件扫描信息解析类写出的结果文件类型
    private static final JavaCG2OutPutFileTypeEnum[] SPRING_XML_TASK_AOP_SCAN_FILE_TYPES = new JavaCG2OutPutFileTypeEnum[]{
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_TASK_XML,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_POINTCUT_XML,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ASPECT_XML,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ADVICE_XML,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_SCAN_PACKAGE_XML
    };

    // 测试jar文件中Spring XML文件的路径
    private static final String XML_ENTRY_PATH_BEAN = "test/spring/spring_bean_test.xml";

    private static final String XML_ENTRY_PATH_TASK_AOP_SCAN = "test/spring/spring_task_aop_scan_test.xml";

    // 自定义扩展解析器生成的结果文件名
    private static final String CUSTOM_PARSER_FILE_NAME = "memory_test_custom_xml_entry";

    /**
     * 场景A：纯内存模式下完整运行时，内置的Spring XML定时任务/AOP/组件扫描信息解析类写出的每个结果文件都可从内存读回，且包含关键行
     */
    @Test
    public void testBuiltInSpringXmlTaskAopScanParserFilesReadable() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        // 关闭Jar兼容性检查，当前测试仅验证扩展解析器写出的结果文件在内存模式的读回
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_CHECK_JAR_COMPATIBILITY_ENABLE, Boolean.FALSE.toString());

        JavaCG2MemoryOutputAdapter memoryOutputAdapter = new JavaCG2MemoryOutputAdapter();
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        javaCG2Entry.setOutputAdapter(memoryOutputAdapter);
        Assert.assertTrue("纯内存模式下执行失败", javaCG2Entry.run());

        // 内置解析类生成的每个文件都应登记在处理结果信息中
        List<String> otherFileNameList = new ArrayList<>(javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getOtherFileNameSet());
        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : SPRING_XML_TASK_AOP_SCAN_FILE_TYPES) {
            Assert.assertTrue("扩展解析器生成的文件应登记在处理结果信息中 " + fileTypeEnum.getFileName(),
                    otherFileNameList.contains(fileTypeEnum.getFileName()));
        }

        // spring_task_xml文件可读回，且包含定时任务关键行（Bean名称、方法名、XML文件路径）
        String taskContent = assertOtherRecordContentReadable(memoryOutputAdapter, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_TASK_XML);
        Assert.assertTrue("spring_task_xml文件应包含定时任务对应的Bean名称与方法名", taskContent.contains("testXmlTaskBean\tdoTask\t"));
        Assert.assertTrue("spring_task_xml文件应包含XML文件路径 " + XML_ENTRY_PATH_TASK_AOP_SCAN, taskContent.contains(XML_ENTRY_PATH_TASK_AOP_SCAN));

        // spring_aop_pointcut_xml文件可读回，且包含两个pointcut的ID与表达式
        String pointcutContent = assertOtherRecordContentReadable(memoryOutputAdapter, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_POINTCUT_XML);
        Assert.assertTrue("spring_aop_pointcut_xml文件应包含aop:config下直接定义的pointcut", pointcutContent.contains("testXmlPointcutDirect\t0\t"));
        Assert.assertTrue("spring_aop_pointcut_xml文件应包含aop:aspect下定义的pointcut", pointcutContent.contains("testXmlPointcutInAspect\t0\t"));
        Assert.assertTrue("spring_aop_pointcut_xml文件应包含pointcut表达式", pointcutContent.contains("within(test.spring.xmlbean..*)"));

        // spring_aop_aspect_xml文件可读回，且包含3个aspect的ID
        String aspectContent = assertOtherRecordContentReadable(memoryOutputAdapter, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ASPECT_XML);
        Assert.assertTrue("spring_aop_aspect_xml文件应包含指定order的aspect", aspectContent.contains("testXmlAspect\ttestXmlAspectBean\t2\t"));
        Assert.assertTrue("spring_aop_aspect_xml文件应包含未指定order的aspect", aspectContent.contains("testXmlAspectNoOrder\t"));
        Assert.assertTrue("spring_aop_aspect_xml文件应包含表达式含换行符的aspect", aspectContent.contains("testXmlAspectWithNewline\t"));

        // spring_aop_advice_xml文件可读回，且包含4种advice的方法名
        String adviceContent = assertOtherRecordContentReadable(memoryOutputAdapter, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ADVICE_XML);
        Assert.assertTrue("spring_aop_advice_xml文件应包含aop:before的advice", adviceContent.contains("\tbeforeAdvice\taop:before\t"));
        Assert.assertTrue("spring_aop_advice_xml文件应包含aop:after-returning的advice", adviceContent.contains("\tafterReturningAdvice\taop:after-returning\t"));
        Assert.assertTrue("spring_aop_advice_xml文件应包含aop:around的advice", adviceContent.contains("\taroundAdvice\taop:around\t"));
        Assert.assertTrue("spring_aop_advice_xml文件应包含aop:after-throwing的advice", adviceContent.contains("\tafterThrowingAdvice\taop:after-throwing\t"));

        // spring_scan_package_xml文件可读回，且包含3条包扫描路径记录（类型为x，序号0~2）
        String scanPackageContent = assertOtherRecordContentReadable(memoryOutputAdapter, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_SCAN_PACKAGE_XML);
        Assert.assertTrue("spring_scan_package_xml文件应包含第1条包扫描路径", scanPackageContent.contains("x\t0\ttest.spring.xmlbean\t"));
        Assert.assertTrue("spring_scan_package_xml文件应包含第2条包扫描路径", scanPackageContent.contains("x\t1\ttest.spring.xmltask\t"));
        Assert.assertTrue("spring_scan_package_xml文件应包含第3条包扫描路径", scanPackageContent.contains("x\t2\ttest.spring.xmlaop\t"));

        // 内置解析类写出的每个文件也可经内存RecordSource读回
        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : SPRING_XML_TASK_AOP_SCAN_FILE_TYPES) {
            JavaCG2RecordSource recordSource = memoryOutputAdapter.genOtherRecordSource(fileTypeEnum.getFileName());
            Assert.assertNotNull("已写出的扩展结果文件应能获得读回数据来源 " + fileTypeEnum.getFileName(), recordSource);
            List<String> lineList = readRecordSourceLines(recordSource);
            Assert.assertFalse("通过内存RecordSource读回的扩展结果文件不应为空 " + fileTypeEnum.getFileName(), lineList.isEmpty());
        }
    }

    /**
     * 场景B：纯内存模式下，通过ExtensionsManager.addCodeParser()方法注册的自定义扩展解析器写出的结果文件可从内存读回
     */
    @Test
    public void testCustomCodeParserFileReadable() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        // 关闭Jar兼容性检查，当前测试仅验证自定义扩展解析器写出的结果文件在内存模式的读回
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_CHECK_JAR_COMPATIBILITY_ENABLE, Boolean.FALSE.toString());

        JavaCG2MemoryOutputAdapter memoryOutputAdapter = new JavaCG2MemoryOutputAdapter();
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        javaCG2Entry.setOutputAdapter(memoryOutputAdapter);
        // 在run()方法之前通过addCodeParser方法注册自定义扩展解析器
        javaCG2Entry.getExtensionsManager().addCodeParser(new CustomXmlEntryInfoParser());
        Assert.assertTrue("纯内存模式下执行失败", javaCG2Entry.run());

        // 自定义扩展生成的文件应登记在处理结果信息中
        Assert.assertNotNull("自定义扩展生成的文件应登记在处理结果信息中 " + CUSTOM_PARSER_FILE_NAME,
                javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getOtherFilePath(CUSTOM_PARSER_FILE_NAME));

        // 自定义扩展写出的文件可从内存读回，测试jar文件中的两个Spring XML文件均被处理
        String content = memoryOutputAdapter.getOtherRecordContent(CUSTOM_PARSER_FILE_NAME);
        Assert.assertTrue("自定义扩展写出的文件内容不应为空 " + CUSTOM_PARSER_FILE_NAME, StringUtils.isNotBlank(content));
        Assert.assertTrue("自定义扩展写出的文件应包含XML文件路径 " + XML_ENTRY_PATH_BEAN, content.contains(XML_ENTRY_PATH_BEAN));
        Assert.assertTrue("自定义扩展写出的文件应包含XML文件路径 " + XML_ENTRY_PATH_TASK_AOP_SCAN, content.contains(XML_ENTRY_PATH_TASK_AOP_SCAN));

        // 通过内存RecordSource读回，每个XML文件对应一行，每行为2列（路径、名称）
        JavaCG2RecordSource recordSource = memoryOutputAdapter.genOtherRecordSource(CUSTOM_PARSER_FILE_NAME);
        Assert.assertNotNull("已写出的自定义扩展结果文件应能获得读回数据来源", recordSource);
        List<String> lineList = readRecordSourceLines(recordSource);
        Assert.assertEquals("自定义扩展写出的文件中每个XML文件应对应一行", 2, lineList.size());
        for (String line : lineList) {
            String[] columns = line.split("\t", -1);
            Assert.assertEquals("自定义扩展写出的文件每行应为2列 " + line, 2, columns.length);
            Assert.assertTrue("第1列应为XML文件在jar中的路径 " + line, StringUtils.isNotBlank(columns[0]));
            Assert.assertEquals("第2列应为XML文件名称", StringUtils.substringAfterLast(columns[0], "/"), columns[1]);
        }
    }

    // 断言指定的其他类型结果文件可从内存读回且非空，返回文件内容
    private String assertOtherRecordContentReadable(JavaCG2MemoryOutputAdapter memoryOutputAdapter, JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        String content = memoryOutputAdapter.getOtherRecordContent(fileTypeEnum.getFileName());
        Assert.assertTrue("内置解析类写出的结果文件内容不应为空 " + fileTypeEnum.getFileName(), StringUtils.isNotBlank(content));
        return content;
    }

    // 读取内存RecordSource的全部行
    private List<String> readRecordSourceLines(JavaCG2RecordSource recordSource) {
        try (BufferedReader bufferedReader = recordSource.genBufferedReader()) {
            List<String> lineList = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lineList.add(line);
            }
            return lineList;
        } catch (IOException e) {
            log.error("error ", e);
            throw new IllegalStateException("读取内存RecordSource失败 " + e.getMessage(), e);
        }
    }

    // 自定义扩展解析器：记录jar文件中每个XML文件的路径与名称，经输出适配器写出
    private static class CustomXmlEntryInfoParser extends AbstractSaveData2FileParser {

        @Override
        public String chooseFileName() {
            return CUSTOM_PARSER_FILE_NAME;
        }

        @Override
        public String[] chooseJarEntryOtherFileExt() {
            // 与JarEntryPreHandle1Parser中拼接"."加文件后缀后的路由Map的key保持一致，需要以"."开头
            return new String[]{JavaCG2Constants.FLAG_DOT + "xml"};
        }

        @Override
        public boolean parseJarEntryOtherFile(InputStream inputStream, String jarEntryPath, String jarEntryName) {
            try {
                writeData2File(jarEntryPath, jarEntryName);
                return true;
            } catch (Exception e) {
                log.error("error ", e);
                return false;
            }
        }
    }
}
