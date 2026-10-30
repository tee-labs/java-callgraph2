package test.spring.xmlbean;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 在Spring XML文件中以无id属性方式定义的bean对应的类
 * Spring允许bean不指定id属性，解析时应跳过该bean，当前类不应出现在spring_bean文件中
 * 会被打包进target/test.jar并被解析
 */
public class TestXmlBeanNoId {
}
