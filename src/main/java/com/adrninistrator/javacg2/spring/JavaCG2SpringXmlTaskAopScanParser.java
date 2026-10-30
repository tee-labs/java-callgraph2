package com.adrninistrator.javacg2.spring;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.common.enums.JavaCG2YesNoEnum;
import com.adrninistrator.javacg2.extensions.codeparser.AbstractSaveData2FileParser;
import com.adrninistrator.javacg2.extensions.codeparser.SpringXmlTaskAopScanParserInterface;
import com.adrninistrator.javacg2.util.JavaCG2Util;
import com.adrninistrator.javacg2.util.JavaCG2XmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 对Spring XML文件中的定时任务（task）、AOP、组件扫描（component-scan）信息解析的类（内置默认实现）
 * 逻辑与java-all-call-graph项目中的SpringXmlCodeParser保持同构，便于日后对照维护
 * 生成的文件名与列格式和java-all-call-graph当前产出的spring_task_xml、spring_aop_aspect_xml、spring_aop_pointcut_xml、
 * spring_aop_advice_xml、spring_scan_package_xml文件保持一致
 */
@Slf4j
public class JavaCG2SpringXmlTaskAopScanParser extends AbstractSaveData2FileParser implements SpringXmlTaskAopScanParserInterface {

    // Spring XML文件中根元素的名称
    private static final String ELEMENT_NAME_BEANS = "beans";

    // task命名空间中的定时任务相关元素名称
    private static final String ELEMENT_NAME_TASK_SCHEDULED_TASKS = "task:scheduled-tasks";

    private static final String ELEMENT_NAME_TASK_SCHEDULED = "task:scheduled";

    // aop命名空间中的相关元素名称
    private static final String ELEMENT_NAME_AOP_CONFIG = "aop:config";

    private static final String ELEMENT_NAME_AOP_POINTCUT = "aop:pointcut";

    private static final String ELEMENT_NAME_AOP_ASPECT = "aop:aspect";

    // context命名空间中的组件扫描元素名称
    private static final String ELEMENT_NAME_CONTEXT_COMPONENT_SCAN = "context:component-scan";

    // aop:aspect元素的order属性未指定时使用的默认值
    private static final String DEFAULT_ASPECT_ORDER = String.valueOf(Integer.MAX_VALUE);

    // aop advice元素的名称
    private static final String ELEMENT_NAME_AOP_ADVICE_BEFORE = "aop:before";

    private static final String ELEMENT_NAME_AOP_ADVICE_AFTER = "aop:after";

    private static final String ELEMENT_NAME_AOP_ADVICE_AROUND = "aop:around";

    private static final String ELEMENT_NAME_AOP_ADVICE_AFTER_RETURNING = "aop:after-returning";

    private static final String ELEMENT_NAME_AOP_ADVICE_AFTER_THROWING = "aop:after-throwing";

    private static final String[] AOP_ADVICE_ELEMENT_NAMES = new String[]{
            ELEMENT_NAME_AOP_ADVICE_BEFORE,
            ELEMENT_NAME_AOP_ADVICE_AFTER,
            ELEMENT_NAME_AOP_ADVICE_AROUND,
            ELEMENT_NAME_AOP_ADVICE_AFTER_RETURNING,
            ELEMENT_NAME_AOP_ADVICE_AFTER_THROWING
    };

    /*
        context:component-scan元素的base-package属性值使用的分隔符
        与Spring的ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS保持一致
     */
    private static final String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

    @Override
    public String[] chooseFileNames() {
        return new String[]{
                JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_TASK_XML.getFileName(),
                JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_POINTCUT_XML.getFileName(),
                JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ASPECT_XML.getFileName(),
                JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ADVICE_XML.getFileName(),
                JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_SCAN_PACKAGE_XML.getFileName()};
    }

    @Override
    public String[] chooseJarEntryOtherFileExt() {
        // 与JarEntryPreHandle1Parser中拼接"."加文件后缀后的路由Map的key保持一致，需要以"."开头
        return new String[]{JavaCG2Constants.FLAG_DOT + "xml"};
    }

    @Override
    public boolean parseJarEntryOtherFile(InputStream inputStream, String jarEntryPath, String jarEntryName) {
        Element root;
        try {
            root = JavaCG2XmlUtil.parseXmlRootElement(inputStream);
        } catch (Exception e) {
            // 解析XML文件出错时不退出执行
            log.warn("解析XML文件出错 {} {}", jarEntryPath, e.getMessage());
            return true;
        }

        try {
            if (!ELEMENT_NAME_BEANS.equals(root.getName())) {
                log.debug("跳过非Spring XML: {}", jarEntryPath);
                return true;
            }

            log.info("处理Spring XML文件 {}", jarEntryPath);
            for (Element element : root.getChildren()) {
                String elementQualifiedName = element.getQualifiedName();
                if (ELEMENT_NAME_TASK_SCHEDULED_TASKS.equals(elementQualifiedName)) {
                    // 处理task:scheduled-tasks元素
                    handleSpringTask(element, jarEntryPath);
                } else if (ELEMENT_NAME_AOP_CONFIG.equals(elementQualifiedName)) {
                    // 处理aop:config元素
                    handleSpringAop(element, jarEntryPath);
                } else if (ELEMENT_NAME_CONTEXT_COMPONENT_SCAN.equals(elementQualifiedName)) {
                    // 处理context:component-scan元素
                    handleSpringComponentScan(element, jarEntryPath);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("error ", e);
            return false;
        }
    }

    // 处理task:scheduled-tasks元素
    private void handleSpringTask(Element element, String jarEntryPath) throws IOException {
        for (Element element2 : element.getChildren()) {
            if (ELEMENT_NAME_TASK_SCHEDULED.equals(element2.getQualifiedName())) {
                String beanName = element2.getAttributeValue("ref");
                String methodName = element2.getAttributeValue("method");
                writeData2Files(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_TASK_XML.getFileName(), beanName, methodName, jarEntryPath);
            }
        }
    }

    // 处理aop:config元素
    private void handleSpringAop(Element element, String jarEntryPath) throws IOException {
        for (Element element2 : element.getChildren()) {
            String elementQualifiedName2 = element2.getQualifiedName();
            if (ELEMENT_NAME_AOP_POINTCUT.equals(elementQualifiedName2)) {
                // 处理aop:config元素下直接定义的aop:pointcut元素
                String pointcutId = element2.getAttributeValue("id");
                String expression = element2.getAttributeValue("expression");
                writePointcut(pointcutId, expression, jarEntryPath);
            } else if (ELEMENT_NAME_AOP_ASPECT.equals(elementQualifiedName2)) {
                // 处理aop:aspect元素
                String aspectId = element2.getAttributeValue("id");
                String ref = element2.getAttributeValue("ref");
                String order = element2.getAttributeValue("order", DEFAULT_ASPECT_ORDER);
                writeData2Files(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ASPECT_XML.getFileName(), aspectId, ref, order, jarEntryPath);

                for (Element element3 : element2.getChildren()) {
                    String elementQualifiedName3 = element3.getQualifiedName();
                    if (ELEMENT_NAME_AOP_POINTCUT.equals(elementQualifiedName3)) {
                        // 处理aop:aspect元素下定义的aop:pointcut元素
                        String pointcutId = element3.getAttributeValue("id");
                        String expression = element3.getAttributeValue("expression");
                        writePointcut(pointcutId, expression, jarEntryPath);
                    } else if (StringUtils.equalsAny(elementQualifiedName3, AOP_ADVICE_ELEMENT_NAMES)) {
                        // 处理aop advice元素
                        String method = element3.getAttributeValue("method");
                        String pointcut = element3.getAttributeValue("pointcut", "");
                        String pointcutRef = element3.getAttributeValue("pointcut-ref", "");
                        JavaCG2YesNoEnum base64 = JavaCG2YesNoEnum.NO;
                        if (StringUtils.isNotBlank(pointcut) && JavaCG2Util.checkNeedBase64(pointcut)) {
                            base64 = JavaCG2YesNoEnum.YES;
                            pointcut = JavaCG2Util.base64Encode(pointcut);
                        }
                        writeData2Files(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ADVICE_XML.getFileName(), aspectId, method, element3.getQualifiedName(),
                                pointcutRef, base64.getStrValue(), pointcut, order, jarEntryPath);
                    }
                }
            }
        }
    }

    // 写入pointcut信息，表达式需要时进行base64编码
    private void writePointcut(String pointcutId, String expression, String jarEntryPath) throws IOException {
        JavaCG2YesNoEnum base64 = JavaCG2YesNoEnum.NO;
        if (JavaCG2Util.checkNeedBase64(expression)) {
            base64 = JavaCG2YesNoEnum.YES;
            expression = JavaCG2Util.base64Encode(expression);
        }
        writeData2Files(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_POINTCUT_XML.getFileName(), pointcutId, base64.getStrValue(), expression, jarEntryPath);
    }

    // 处理context:component-scan元素
    private void handleSpringComponentScan(Element element, String jarEntryPath) throws IOException {
        String basePackageStr = element.getAttributeValue("base-package");
        if (StringUtils.isBlank(basePackageStr)) {
            // base-package属性未指定时不处理
            return;
        }

        /*
            对base-package属性值的拆分方式与Spring的
            org.springframework.context.annotation.ComponentScanBeanDefinitionParser.parse方法中
            调用StringUtils.tokenizeToStringArray方法的逻辑保持一致
         */
        List<String> basePackageList = tokenizeBasePackage(basePackageStr);
        for (int i = 0; i < basePackageList.size(); i++) {
            writeData2Files(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_SCAN_PACKAGE_XML.getFileName(), JavaCG2Constants.FILE_KEY_SPRING_DEFINE_IN_XML,
                    String.valueOf(i), basePackageList.get(i), jarEntryPath);
        }
    }

    // 按分隔符拆分base-package属性值，与Spring的StringUtils.tokenizeToStringArray方法逻辑一致（去除token两端的空白，忽略空token）
    private List<String> tokenizeBasePackage(String basePackageStr) {
        List<String> basePackageList = new ArrayList<>();
        StringTokenizer stringTokenizer = new StringTokenizer(basePackageStr, CONFIG_LOCATION_DELIMITERS);
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken().trim();
            if (!token.isEmpty()) {
                basePackageList.add(token);
            }
        }
        return basePackageList;
    }
}
