package golden.anno;

import golden.holder.GoldenStaticHolder;

/**
 * 黄金基线夹具：类与方法上使用注解，覆盖类的注解、方法的注解、方法参数的注解
 */
@GoldenMarker(value = "annotated-service", order = 3)
public class GoldenAnnotatedService {

    /**
     * 方法注解（仅value属性）与方法参数注解
     */
    @GoldenTrace("doServe")
    public String doServe(@GoldenMarker("arg-marker") String input) {
        // 调用静态方法
        GoldenStaticHolder.increaseCall();
        return "served:" + input;
    }

    /**
     * 方法注解（全部属性），覆盖方法返回常量值的三目运算
     */
    @GoldenTrace(value = "quietServe", verbose = true)
    public String quietServe(String input) {
        return input == null ? GoldenStaticHolder.VERSION : input;
    }

    /**
     * 未使用注解的方法，作为同文件中的对照
     */
    public String noAnnotation(String input) {
        return "plain:" + input;
    }
}
