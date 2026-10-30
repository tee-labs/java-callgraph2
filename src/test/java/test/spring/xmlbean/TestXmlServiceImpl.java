package test.spring.xmlbean;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: {@link TestXmlService}的实现类，在Spring XML文件中定义为bean
 * 会被打包进target/test.jar并被解析
 */
public class TestXmlServiceImpl implements TestXmlService {

    @Override
    public String doSomething(String arg) {
        return "done: " + arg;
    }
}
