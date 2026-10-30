package com.adrninistrator.javacg2.extensions.codeparser;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 对Spring XML文件中的定时任务（task）、AOP、组件扫描（component-scan）信息解析的接口
 * 解析结果输出到spring_task_xml、spring_aop_aspect_xml、spring_aop_pointcut_xml、spring_aop_advice_xml、spring_scan_package_xml文件
 * 未通过{@link com.adrninistrator.javacg2.extensions.manager.ExtensionsManager#setSpringXmlTaskAopScanParser}指定实现时，
 * 默认使用内置的{@link com.adrninistrator.javacg2.spring.JavaCG2SpringXmlTaskAopScanParser}
 */
public interface SpringXmlTaskAopScanParserInterface extends JarEntryOtherFileParser {
}
