package test.jardiff.diffjar;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: JarDiff e2e测试使用的夹具类，仅引用JDK中的类，会被打包进构造的新旧两个版本的jar文件中
 */
public class DiffJarService {

    public String doSomething(String arg) {
        return arg + "_suffix";
    }

    public int calc(int a, int b) {
        return a + b;
    }

    public static String flag() {
        return "flag";
    }
}
