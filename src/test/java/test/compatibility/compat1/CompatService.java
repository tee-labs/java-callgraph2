package test.compatibility.compat1;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 用于Jar兼容性检查测试的类，在部分测试场景中不会被打包进需要解析的jar文件，从而制造被引用类不存在的兼容性问题
 */
public class CompatService {

    // 非final的public静态字段，避免编译时被内联，保证使用该字段的指令保留在字节码中
    public static int PUBLIC_STATIC_FIELD = 1;

    public int doSomething(int arg) {
        return arg + 1;
    }

    public static int staticMethod() {
        return 2;
    }
}
