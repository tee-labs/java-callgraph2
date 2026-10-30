package com.adrninistrator.javacg2.spring;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.dto.spring.SpringBeanInXml;
import com.adrninistrator.javacg2.extensions.codeparser.SpringXmlBeanParserInterface;
import com.adrninistrator.javacg2.util.JavaCG2Util;
import com.adrninistrator.javacg2.util.JavaCG2XmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 对Spring XML文件中的bean解析的类（内置默认实现）
 * 逻辑与java-all-call-graph项目中的SpringXmlBeanParser保持同构，便于日后对照维护
 */
@Slf4j
public class JavaCG2SpringXmlBeanParser implements SpringXmlBeanParserInterface {

    // Spring XML文件中根元素及嵌套beans元素的名称
    private static final String ELEMENT_NAME_BEANS = "beans";

    // Spring XML文件中bean元素的名称
    private static final String ELEMENT_NAME_BEAN = "bean";

    /*
        Spring Bean对应的Map
        key Bean名称
        value Bean的类名
     */
    private Map<String, String> beanMap;

    // XML文件中定义的Spring Bean信息
    private List<SpringBeanInXml> springBeanInXmlList;

    @Override
    public void initCodeParser() {
        beanMap = new HashMap<>();
        springBeanInXmlList = new ArrayList<>();
    }

    @Override
    public String getBeanClass(String beanId) {
        return beanMap.get(beanId);
    }

    @Override
    public List<SpringBeanInXml> getBeanInXml() {
        return springBeanInXmlList;
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
                return true;
            }

            log.info("处理Spring XML文件 {}", jarEntryPath);
            // 处理XML中的bean元素
            for (Element element : root.getChildren()) {
                if (checkBeanElement(element)) {
                    // 解析bean元素
                    parseBeanElement(element, Collections.emptyList());
                }
            }
            // 处理XML中的beans元素
            for (Element element : root.getChildren()) {
                if (checkBeansElement(element)) {
                    // beans元素
                    List<String> profileList = new ArrayList<>();
                    // 解析beans元素
                    parseBeansElement(element, profileList);
                }
            }

            for (SpringBeanInXml springBeanInXml : springBeanInXmlList) {
                springBeanInXml.setXmlFilePath(jarEntryPath);
            }
            return true;
        } catch (Exception e) {
            log.error("error ", e);
            return false;
        }
    }

    // 解析beans元素
    private void parseBeansElement(Element element, List<String> profileList) {
        String profile = element.getAttributeValue("profile");
        if (StringUtils.isNotBlank(profile)) {
            profileList.add(profile);
        }

        for (Element childElement : element.getChildren()) {
            if (checkBeansElement(childElement)) {
                // 解析beans元素
                List<String> newProfileList = new ArrayList<>(profileList);
                parseBeansElement(childElement, newProfileList);
            } else if (checkBeanElement(childElement)) {
                // 解析bean元素
                parseBeanElement(childElement, profileList);
            }
        }
    }

    // 解析bean元素
    private void parseBeanElement(Element element, List<String> profileList) {
        String beanId = element.getAttributeValue("id");
        String beanClass = element.getAttributeValue("class");
        if (StringUtils.isNoneBlank(beanId, beanClass)) {
            // Spring XML文件中定义的bean可不指定id属性，这种情况下不处理
            beanMap.put(beanId, beanClass);
            SpringBeanInXml springBeanInXml = new SpringBeanInXml();
            springBeanInXml.setSpringBeanName(beanId);
            springBeanInXml.setClassName(beanClass);
            if (JavaCG2Util.isCollectionEmpty(profileList)) {
                springBeanInXml.setProfile("");
            } else {
                String profile = StringUtils.join(profileList, JavaCG2Constants.FLAG_COMMA);
                springBeanInXml.setProfile(profile);
            }
            springBeanInXmlList.add(springBeanInXml);
        }
    }

    private boolean checkBeansElement(Element element) {
        return ELEMENT_NAME_BEANS.equals(element.getQualifiedName());
    }

    private boolean checkBeanElement(Element element) {
        return ELEMENT_NAME_BEAN.equals(element.getQualifiedName());
    }
}
