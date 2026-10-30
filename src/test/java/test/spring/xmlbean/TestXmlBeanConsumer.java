package test.spring.xmlbean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 通过@Autowired注入XML文件中定义的bean并调用其方法的类
 * 字段名称与Spring XML文件中bean的id一致（@Autowired注解未指定@Qualifier时按字段名称匹配bean）
 * 会被打包进target/test.jar并被解析
 */
@Service
public class TestXmlBeanConsumer {

    // 注入XML文件中定义的bean，bean的id为testXmlService
    @Autowired
    private TestXmlService testXmlService;

    public String callService(String arg) {
        return testXmlService.doSomething(arg);
    }
}
