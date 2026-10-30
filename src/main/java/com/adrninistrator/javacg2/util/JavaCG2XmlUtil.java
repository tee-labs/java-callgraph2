package com.adrninistrator.javacg2.util;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: XML文件处理工具类
 */
public class JavaCG2XmlUtil {

    /**
     * 根据InputStream解析XML，获取根元素
     * 不会关闭传入的InputStream
     *
     * @param inputStream
     * @return XML文件的根元素
     * @throws IOException
     * @throws JDOMException
     */
    public static Element parseXmlRootElement(InputStream inputStream) throws IOException, JDOMException {
        SAXBuilder saxBuilder = new SAXBuilder();
        // 允许在XML文件中定义DOCTYPE
        saxBuilder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        // 禁止读取外部通用实体
        saxBuilder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        // 禁止读取外部参数实体
        saxBuilder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        // 不读取DTD
        saxBuilder.setEntityResolver(new NoOpEntityResolver());

        Document document = saxBuilder.build(inputStream);
        return document.getRootElement();
    }

    // 不读取外部实体的EntityResolver实现，对任何实体均返回空的InputSource，作用是不读取外部DTD
    private static class NoOpEntityResolver implements EntityResolver {

        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            return new InputSource(new StringReader(""));
        }
    }

    private JavaCG2XmlUtil() {
        throw new IllegalStateException("illegal");
    }
}
