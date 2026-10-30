package test.spring.xmlbean;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 用于测试Spring XML文件中定义的Bean的接口
 * 会被打包进target/test.jar并被解析
 */
public interface TestXmlService {

    String doSomething(String arg);
}
