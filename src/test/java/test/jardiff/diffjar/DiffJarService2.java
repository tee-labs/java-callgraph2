package test.jardiff.diffjar;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: JarDiff e2e测试使用的夹具类，仅在新版本的jar文件中出现，用于验证新增的类及方法
 */
public class DiffJarService2 {

    public String newMethod(String arg) {
        return arg + "!";
    }
}
