package test.compatibility.compat1;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 用于Jar兼容性检查测试的类，引用了CompatService类，单独打包时会产生被引用类、方法、字段不存在的兼容性问题
 */
public class UseService {

    public int useService() {
        CompatService compatService = new CompatService();
        int result = compatService.doSomething(CompatService.PUBLIC_STATIC_FIELD);
        result += CompatService.staticMethod();
        return result;
    }
}
