package test.spring;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.dto.spring.SpringBeanInXml;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElAllowedVariableEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.extensions.codeparser.SpringXmlBeanParserInterface;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 测试内置的Spring XML文件中定义的Bean解析类
 * 解析输入为target/test.jar（由mvn test时的maven-antrun-plugin自动生成），其中包含：
 * 1 测试类test/spring/xmlbean/下通过注解定义的Spring Bean及注入XML bean的字段
 * 2 Spring XML文件test/spring/spring_bean_test.xml
 */
public class TestSpringXmlBean extends TestBase {

    // XML文件中定义的bean名称
    private static final String BEAN_NAME_TEST_XML_SERVICE = "testXmlService";

    // XML文件中定义的bean对应的实现类
    private static final String BEAN_CLASS_TEST_XML_SERVICE_IMPL = "test.spring.xmlbean.TestXmlServiceImpl";

    // 嵌套beans元素中定义的bean名称
    private static final String BEAN_NAME_XML_DEV = "xmlDevBean";

    // 无id属性的bean对应的类，不应出现在spring_bean文件中
    private static final String BEAN_CLASS_NO_ID = "test.spring.xmlbean.TestXmlBeanNoId";

    // Spring XML文件在jar中的路径
    private static final String XML_ENTRY_PATH = "test/spring/spring_bean_test.xml";

    // Spring Bean定义方式为XML文件定义
    private static final String DEFINE_IN_XML = "x";

    // 默认输出根目录
    private static final String OUT_DIR_DEFAULT = OUT_TEST_DIR;

    // 验证忽略表达式时使用的输出根目录
    private static final String OUT_DIR_IGNORE = "out/test_spring_xml_ignore";

    // 验证自定义实现覆盖内置实现时使用的输出根目录
    private static final String OUT_DIR_CUSTOM = "out/test_spring_xml_custom";

    // 验证关闭内置解析开关时使用的输出根目录
    private static final String OUT_DIR_SWITCH_OFF = "out/test_spring_xml_switch_off";

    /**
     * 验证a：spring_bean文件中包含XML定义的Bean行（定义方式为x），包含profile值与XML路径；无id的bean不出现
     * 验证b：@Autowired注入XML中定义的bean的字段调用方法时，方法调用文件中出现_SPR_ACT_I调用类型，且被调用类型为XML中定义的实现类
     */
    @Test
    public void testSpringBeanInXmlRecordedAndTypeReplaced() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_DIR_DEFAULT);
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());

        // 验证a：spring_bean文件中XML定义的Bean行
        List<String> springBeanLineList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_BEAN);
        String serviceBeanRow = findRowByBeanName(springBeanLineList, BEAN_NAME_TEST_XML_SERVICE);
        Assert.assertNotNull("spring_bean文件中应存在XML定义的bean行 " + BEAN_NAME_TEST_XML_SERVICE, serviceBeanRow);
        String[] serviceBeanColumns = serviceBeanRow.split("\t", -1);
        Assert.assertEquals("bean对应的类名应为XML中定义的实现类", BEAN_CLASS_TEST_XML_SERVICE_IMPL, serviceBeanColumns[2]);
        Assert.assertEquals("无profile的bean的profile列应为空", "", serviceBeanColumns[3]);
        Assert.assertEquals("bean的定义方式应为XML文件定义", DEFINE_IN_XML, serviceBeanColumns[4]);
        Assert.assertTrue("XML定义的bean行应包含XML文件路径 " + XML_ENTRY_PATH, serviceBeanColumns[6].endsWith(XML_ENTRY_PATH));

        // 验证a：嵌套beans元素中定义的bean行，profile为dev
        String devBeanRow = findRowByBeanName(springBeanLineList, BEAN_NAME_XML_DEV);
        Assert.assertNotNull("spring_bean文件中应存在嵌套beans元素中定义的bean行 " + BEAN_NAME_XML_DEV, devBeanRow);
        String[] devBeanColumns = devBeanRow.split("\t", -1);
        Assert.assertEquals("嵌套beans元素中bean的类名", "java.lang.Object", devBeanColumns[2]);
        Assert.assertEquals("嵌套beans元素中bean的profile应为dev", "dev", devBeanColumns[3]);
        Assert.assertEquals("嵌套beans元素中bean的定义方式应为XML文件定义", DEFINE_IN_XML, devBeanColumns[4]);

        // 验证a：无id属性的bean被跳过
        Assert.assertNull("无id属性的bean不应出现在spring_bean文件中", findRowByClassName(springBeanLineList, BEAN_CLASS_NO_ID));

        // 验证b：方法调用文件中出现通过XML中定义的bean替换实际类型的调用
        List<String> methodCallLineList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL);
        String sprActIRow = findRowContaining(methodCallLineList, "_SPR_ACT_I");
        Assert.assertNotNull("方法调用文件中应存在_SPR_ACT_I调用类型", sprActIRow);
        Assert.assertTrue("通过XML中定义的bean替换的实际类型应为实现类 " + BEAN_CLASS_TEST_XML_SERVICE_IMPL,
                sprActIRow.contains(BEAN_CLASS_TEST_XML_SERVICE_IMPL));
    }

    /**
     * 验证c：通过忽略表达式按beanName忽略XML中的Bean后，spring_bean文件无该行，方法调用文件中不出现按该Bean的_SPR_ACT_I替换
     */
    @Test
    public void testIgnoreXmlBeanByEl() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_DIR_IGNORE);
        // 按bean名称忽略XML中定义的testXmlService
        javaCG2ConfigureWrapper.setElConfigText(JavaCG2ElConfigEnum.ECE_HANDLE_IGNORE_SPRING_BEAN_IN_XML,
                JavaCG2ElAllowedVariableEnum.EAVE_SPB_BEAN_NAME.getVariableName() + " == '" + BEAN_NAME_TEST_XML_SERVICE + "'");
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());

        // spring_bean文件中被忽略的bean行应消失，未被忽略的bean行仍存在
        List<String> springBeanLineList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_BEAN);
        Assert.assertNull("被忽略的XML bean不应出现在spring_bean文件中", findRowByBeanName(springBeanLineList, BEAN_NAME_TEST_XML_SERVICE));
        Assert.assertNotNull("未被忽略的XML bean应出现在spring_bean文件中", findRowByBeanName(springBeanLineList, BEAN_NAME_XML_DEV));

        // 方法调用文件中不应出现按被忽略Bean的_SPR_ACT_I替换（验证UseSpringBeanByAnnotationHandler中应用的忽略表达式生效）
        List<String> methodCallLineList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL);
        Assert.assertNull("被忽略的XML bean不应再触发_SPR_ACT_I类型替换", findRowContaining(methodCallLineList, "_SPR_ACT_I"));
    }

    /**
     * 验证d：通过ExtensionsManager设置自定义的SpringXmlBeanParserInterface实现后，内置实现让位，spring_bean文件无XML定义行
     */
    @Test
    public void testCustomParserOverridesBuiltIn() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_DIR_CUSTOM);
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        // 设置自定义的空实现，自定义实现优先于内置实现
        javaCG2Entry.getExtensionsManager().setSpringXmlBeanParser(new EmptySpringXmlBeanParser());
        Assert.assertTrue(javaCG2Entry.run());

        List<String> springBeanLineList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_BEAN);
        Assert.assertNull("使用自定义实现时XML定义的bean不应出现在spring_bean文件中", findRowByBeanName(springBeanLineList, BEAN_NAME_TEST_XML_SERVICE));
        Assert.assertNull("使用自定义实现时XML定义的bean不应出现在spring_bean文件中", findRowByBeanName(springBeanLineList, BEAN_NAME_XML_DEV));
    }

    /**
     * 验证e：配置参数use.built.in.spring.xml.bean.parser为false且未指定自定义实现时，不使用内置解析类，spring_bean文件无XML定义行
     */
    @Test
    public void testUseBuiltInParserSwitchOff() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_DIR_SWITCH_OFF);
        // 关闭内置的Spring XML Bean解析类
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_USE_BUILT_IN_SPRING_XML_BEAN_PARSER, Boolean.FALSE.toString());
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());

        List<String> springBeanLineList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_BEAN);
        Assert.assertNull("关闭内置解析开关后XML定义的bean不应出现在spring_bean文件中", findRowByBeanName(springBeanLineList, BEAN_NAME_TEST_XML_SERVICE));
        Assert.assertNull("关闭内置解析开关后XML定义的bean不应出现在spring_bean文件中", findRowByBeanName(springBeanLineList, BEAN_NAME_XML_DEV));
        List<String> methodCallLineList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL);
        Assert.assertNull("关闭内置解析开关后不应出现按XML中定义的Bean的_SPR_ACT_I类型替换", findRowContaining(methodCallLineList, "_SPR_ACT_I"));
    }

    // 读取生成的结果文件内容
    private List<String> readFile4Output(JavaCG2Entry javaCG2Entry, JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        String filePath = javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getMainFilePath(fileTypeEnum);
        Assert.assertTrue("结果文件应存在: " + filePath, new File(filePath).exists());
        return JavaCG2FileUtil.readFile2List(filePath);
    }

    // 按bean名称查找spring_bean文件中的行
    private String findRowByBeanName(List<String> lineList, String beanName) {
        String prefix = beanName + "\t";
        for (String line : lineList) {
            if (line.startsWith(prefix)) {
                return line;
            }
        }
        return null;
    }

    // 按类名查找spring_bean文件中的行
    private String findRowByClassName(List<String> lineList, String className) {
        for (String line : lineList) {
            if (line.contains("\t" + className + "\t")) {
                return line;
            }
        }
        return null;
    }

    // 查找包含指定内容的行
    private String findRowContaining(List<String> lineList, String content) {
        for (String line : lineList) {
            if (line.contains(content)) {
                return line;
            }
        }
        return null;
    }

    // 自定义的Spring XML Bean解析类，不解析任何内容，用于验证自定义实现优先于内置实现
    private static class EmptySpringXmlBeanParser implements SpringXmlBeanParserInterface {

        @Override
        public String getBeanClass(String beanId) {
            return null;
        }

        @Override
        public List<SpringBeanInXml> getBeanInXml() {
            return Collections.emptyList();
        }

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
