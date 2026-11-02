package golden.core;

/**
 * 黄金基线夹具：被继承的父类，覆盖继承关系的解析
 * 本目录下的夹具源码仅用于一次性手工编译生成golden-fixture.jar（提交入库），
 * 不参与mvn编译，也不会被antrun打进target/test.jar，避免污染其他测试的解析输入
 */
public class GoldenBaseService {

    // 父类字段
    protected String baseName = "golden-base";

    /**
     * 供子类调用的public方法
     */
    public String queryBase(String input) {
        return "base:" + input;
    }

    /**
     * 供子类调用的protected方法
     */
    protected int baseCalc(int first, int second) {
        return first + second;
    }
}
