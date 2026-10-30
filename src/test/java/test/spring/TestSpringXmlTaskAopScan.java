package test.spring;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.extensions.codeparser.SpringXmlTaskAopScanParserInterface;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import com.adrninistrator.javacg2.util.JavaCG2Util;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 测试内置的Spring XML文件中的定时任务（task）、AOP、组件扫描（component-scan）信息解析类
 * 解析输入为target/test.jar（由mvn test时的maven-antrun-plugin自动生成），其中包含：
 * 1 Spring XML文件test/spring/spring_task_aop_scan_test.xml（含task:scheduled-tasks、aop:config、context:component-scan元素）
 */
public class TestSpringXmlTaskAopScan extends TestBase {

    // Spring XML文件在jar中的路径
    private static final String XML_ENTRY_PATH = "test/spring/spring_task_aop_scan_test.xml";

    // 默认输出根目录
    private static final String OUT_DIR_DEFAULT = "out/test_spring_task_aop_scan_default";

    // 验证自定义实现覆盖内置实现时使用的输出根目录
    private static final String OUT_DIR_CUSTOM = "out/test_spring_task_aop_scan_custom";

    // 验证关闭内置解析开关时使用的输出根目录
    private static final String OUT_DIR_SWITCH_OFF = "out/test_spring_task_aop_scan_switch_off";

    /**
     * 验证a：内置解析类将XML中的定时任务、AOP aspect/pointcut/advice、组件扫描信息写入对应的结果文件
     * 列数、列顺序、列内容与java-all-call-graph现有产物保持一致
     */
    @Test
    public void testTaskAopScanRecorded() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_DIR_DEFAULT);
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());

        // 验证spring_task_xml文件，列1 Bean名称，列2 方法名，列3 XML文件路径
        List<String> taskLineList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_TASK_XML);
        Assert.assertEquals("spring_task_xml文件中应有1条定时任务记录", 1, taskLineList.size());
        String[] taskColumns = taskLineList.get(0).split("\t", -1);
        Assert.assertEquals("spring_task_xml文件应为3列", 3, taskColumns.length);
        Assert.assertEquals("定时任务对应的Bean名称", "testXmlTaskBean", taskColumns[0]);
        Assert.assertEquals("定时任务对应的方法名", "doTask", taskColumns[1]);
        Assert.assertTrue("定时任务所在XML文件路径 " + taskColumns[2], taskColumns[2].endsWith(XML_ENTRY_PATH));

        // 验证spring_aop_pointcut_xml文件，列1 pointcut的ID，列2 是否base64，列3 表达式，列4 XML文件路径；顺序与XML中出现顺序一致
        List<String> pointcutLineList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_POINTCUT_XML);
        Assert.assertEquals("spring_aop_pointcut_xml文件中应有2条pointcut记录", 2, pointcutLineList.size());
        String[] pointcut1Columns = pointcutLineList.get(0).split("\t", -1);
        Assert.assertEquals(4, pointcut1Columns.length);
        Assert.assertEquals("aop:config下直接定义的pointcut的ID", "testXmlPointcutDirect", pointcut1Columns[0]);
        Assert.assertEquals("无需base64编码的表达式标志", "0", pointcut1Columns[1]);
        Assert.assertEquals("pointcut表达式", "execution(* test.spring.xmlbean.TestXmlServiceImpl.*(..))", pointcut1Columns[2]);
        Assert.assertTrue(pointcut1Columns[3].endsWith(XML_ENTRY_PATH));
        String[] pointcut2Columns = pointcutLineList.get(1).split("\t", -1);
        Assert.assertEquals(4, pointcut2Columns.length);
        Assert.assertEquals("aop:aspect下定义的pointcut的ID", "testXmlPointcutInAspect", pointcut2Columns[0]);
        Assert.assertEquals("无需base64编码的表达式标志", "0", pointcut2Columns[1]);
        Assert.assertEquals("pointcut表达式", "within(test.spring.xmlbean..*)", pointcut2Columns[2]);
        Assert.assertTrue(pointcut2Columns[3].endsWith(XML_ENTRY_PATH));

        // 验证spring_aop_aspect_xml文件，列1 aspect的ID，列2 对应的Bean名称，列3 order值，列4 XML文件路径；未指定order时为Integer.MAX_VALUE
        List<String> aspectLineList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ASPECT_XML);
        Assert.assertEquals("spring_aop_aspect_xml文件中应有3条aspect记录", 3, aspectLineList.size());
        String[] aspect1Columns = aspectLineList.get(0).split("\t", -1);
        Assert.assertEquals(4, aspect1Columns.length);
        Assert.assertEquals("aspect的ID", "testXmlAspect", aspect1Columns[0]);
        Assert.assertEquals("aspect对应的Bean名称", "testXmlAspectBean", aspect1Columns[1]);
        Assert.assertEquals("aspect指定的order值", "2", aspect1Columns[2]);
        String[] aspect2Columns = aspectLineList.get(1).split("\t", -1);
        Assert.assertEquals(4, aspect2Columns.length);
        Assert.assertEquals("未指定order的aspect的ID", "testXmlAspectNoOrder", aspect2Columns[0]);
        Assert.assertEquals("未指定order的aspect的order默认值", String.valueOf(Integer.MAX_VALUE), aspect2Columns[2]);
        String[] aspect3Columns = aspectLineList.get(2).split("\t", -1);
        Assert.assertEquals(4, aspect3Columns.length);
        Assert.assertEquals("含换行表达式的aspect的ID", "testXmlAspectWithNewline", aspect3Columns[0]);
        Assert.assertEquals("aspect指定的order值", "3", aspect3Columns[2]);

        // 验证spring_aop_advice_xml文件，列1 aspect的ID，列2 方法名，列3 advice元素名称，列4 pointcut-ref，列5 是否base64，列6 pointcut表达式，列7 order值，列8 XML文件路径
        List<String> adviceLineList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ADVICE_XML);
        Assert.assertEquals("spring_aop_advice_xml文件中应有4条advice记录", 4, adviceLineList.size());
        String[] advice1Columns = adviceLineList.get(0).split("\t", -1);
        Assert.assertEquals(8, advice1Columns.length);
        Assert.assertEquals("aop:before对应的aspect的ID", "testXmlAspect", advice1Columns[0]);
        Assert.assertEquals("aop:before对应的方法名", "beforeAdvice", advice1Columns[1]);
        Assert.assertEquals("advice XML元素名称", "aop:before", advice1Columns[2]);
        Assert.assertEquals("pointcut-ref名称", "testXmlPointcutInAspect", advice1Columns[3]);
        Assert.assertEquals("未内联定义pointcut时无需base64编码", "0", advice1Columns[4]);
        Assert.assertEquals("未内联定义pointcut时该列为空", "", advice1Columns[5]);
        Assert.assertEquals("aspect的order值", "2", advice1Columns[6]);
        String[] advice2Columns = adviceLineList.get(1).split("\t", -1);
        Assert.assertEquals(8, advice2Columns.length);
        Assert.assertEquals("aop:after-returning对应的aspect的ID", "testXmlAspect", advice2Columns[0]);
        Assert.assertEquals("advice XML元素名称", "aop:after-returning", advice2Columns[2]);
        Assert.assertEquals("未指定pointcut-ref时该列为空", "", advice2Columns[3]);
        Assert.assertEquals("无需base64编码的表达式标志", "0", advice2Columns[4]);
        Assert.assertEquals("内联定义的pointcut表达式", "execution(* test.spring..doSomething(..))", advice2Columns[5]);
        String[] advice3Columns = adviceLineList.get(2).split("\t", -1);
        Assert.assertEquals(8, advice3Columns.length);
        Assert.assertEquals("aop:around对应的aspect的ID", "testXmlAspectNoOrder", advice3Columns[0]);
        Assert.assertEquals("pointcut-ref名称", "testXmlPointcutDirect", advice3Columns[3]);
        Assert.assertEquals("aspect的order默认值", String.valueOf(Integer.MAX_VALUE), advice3Columns[6]);
        String[] advice4Columns = adviceLineList.get(3).split("\t", -1);
        Assert.assertEquals(8, advice4Columns.length);
        Assert.assertEquals("aop:after-throwing对应的aspect的ID", "testXmlAspectWithNewline", advice4Columns[0]);
        Assert.assertEquals("advice XML元素名称", "aop:after-throwing", advice4Columns[2]);
        Assert.assertEquals("含换行的表达式需要base64编码", "1", advice4Columns[4]);
        Assert.assertEquals("含换行的表达式应进行base64编码",
                JavaCG2Util.base64Encode("execution(* test.spring.xmlbean..*.*(..))\n"), advice4Columns[5]);
        Assert.assertEquals("aspect的order值", "3", advice4Columns[6]);

        // 验证spring_scan_package_xml文件，列1 类型固定为x，列2 序号，列3 包扫描路径，列4 XML文件路径
        List<String> scanPackageLineList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_SCAN_PACKAGE_XML);
        Assert.assertEquals("spring_scan_package_xml文件中应有3条包扫描路径记录", 3, scanPackageLineList.size());
        checkScanPackageRow(scanPackageLineList.get(0), "0", "test.spring.xmlbean");
        checkScanPackageRow(scanPackageLineList.get(1), "1", "test.spring.xmltask");
        checkScanPackageRow(scanPackageLineList.get(2), "2", "test.spring.xmlaop");
    }

    /**
     * 验证b：通过ExtensionsManager设置自定义的SpringXmlTaskAopScanParserInterface实现后，内置实现让位，不生成XML相关的结果文件
     */
    @Test
    public void testCustomParserOverridesBuiltIn() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_DIR_CUSTOM);
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        // 设置自定义的空实现，自定义实现优先于内置实现
        javaCG2Entry.getExtensionsManager().setSpringXmlTaskAopScanParser(new EmptySpringXmlTaskAopScanParser());
        Assert.assertTrue(javaCG2Entry.run());

        // 使用自定义实现时未注册内置实现生成的文件，对应文件不应生成
        Assert.assertFalse("使用自定义实现时不应生成spring_task_xml文件",
                new File(javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo()
                        .getMainFilePath(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_TASK_XML)).exists());
        Assert.assertFalse("使用自定义实现时不应生成spring_aop_advice_xml文件",
                new File(javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo()
                        .getMainFilePath(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ADVICE_XML)).exists());
        Assert.assertFalse("使用自定义实现时不应生成spring_scan_package_xml文件",
                new File(javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo()
                        .getMainFilePath(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_SCAN_PACKAGE_XML)).exists());
    }

    /**
     * 验证c：配置参数use.built.in.spring.xml.task.aop.scan.parser为false且未指定自定义实现时，不使用内置解析类，不生成XML相关的结果文件
     */
    @Test
    public void testUseBuiltInParserSwitchOff() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_DIR_SWITCH_OFF);
        // 关闭内置的Spring XML定时任务/AOP/组件扫描信息解析类
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_USE_BUILT_IN_SPRING_XML_TASK_AOP_SCAN_PARSER, Boolean.FALSE.toString());
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());

        Assert.assertFalse("关闭内置解析开关后不应生成spring_task_xml文件",
                new File(javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo()
                        .getMainFilePath(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_TASK_XML)).exists());
        Assert.assertFalse("关闭内置解析开关后不应生成spring_aop_aspect_xml文件",
                new File(javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo()
                        .getMainFilePath(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ASPECT_XML)).exists());
        Assert.assertFalse("关闭内置解析开关后不应生成spring_aop_pointcut_xml文件",
                new File(javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo()
                        .getMainFilePath(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_POINTCUT_XML)).exists());
        Assert.assertFalse("关闭内置解析开关后不应生成spring_scan_package_xml文件",
                new File(javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo()
                        .getMainFilePath(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_SCAN_PACKAGE_XML)).exists());
    }

    // 读取生成的结果文件内容
    private List<String> readFile4Output(JavaCG2Entry javaCG2Entry, JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        String filePath = javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getMainFilePath(fileTypeEnum);
        Assert.assertTrue("结果文件应存在: " + filePath, new File(filePath).exists());
        return JavaCG2FileUtil.readFile2List(filePath);
    }

    // 检查spring_scan_package_xml文件中的一行
    private void checkScanPackageRow(String line, String seq, String basePackage) {
        String[] columns = line.split("\t", -1);
        Assert.assertEquals("spring_scan_package_xml文件应为4列", 4, columns.length);
        Assert.assertEquals("XML中定义的包扫描路径类型", "x", columns[0]);
        Assert.assertEquals("包扫描路径的序号", seq, columns[1]);
        Assert.assertEquals("包扫描路径", basePackage, columns[2]);
        Assert.assertTrue("包扫描路径所在XML文件路径 " + columns[3], columns[3].endsWith(XML_ENTRY_PATH));
    }

    // 自定义的Spring XML定时任务/AOP/组件扫描信息解析类，不解析任何内容，用于验证自定义实现优先于内置实现
    private static class EmptySpringXmlTaskAopScanParser implements SpringXmlTaskAopScanParserInterface {

        @Override
        public String[] chooseJarEntryOtherFileExt() {
            return new String[]{".xml"};
        }

        @Override
        public boolean parseJarEntryOtherFile(InputStream inputStream, String jarEntryPath, String jarEntryName) {
            return true;
        }
    }
}
