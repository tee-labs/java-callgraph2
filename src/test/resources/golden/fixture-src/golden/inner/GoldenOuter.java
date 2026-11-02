package golden.inner;

import golden.core.GoldenSpi;

/**
 * 黄金基线夹具：内部类，覆盖命名内部类、静态嵌套类与匿名内部类
 */
public class GoldenOuter {

    // 外部类字段，供内部类读取
    private String outerField = "outer";

    /**
     * 命名内部类（非静态）
     */
    public class NamedInner {

        public String describe() {
            return "inner:" + outerField;
        }
    }

    /**
     * 静态嵌套类
     */
    public static class StaticNested {

        public int add(int first, int second) {
            return first + second;
        }
    }

    /**
     * 使用命名内部类
     */
    public String useNamedInner() {
        NamedInner inner = new NamedInner();
        return inner.describe();
    }

    /**
     * 使用静态嵌套类
     */
    public int useStaticNested(int first, int second) {
        StaticNested nested = new StaticNested();
        return nested.add(first, second);
    }

    /**
     * 匿名内部类实现接口
     */
    public GoldenSpi useAnonymous() {
        return new GoldenSpi() {

            @Override
            public String exec(String task) {
                return "anonymous:" + task;
            }
        };
    }
}
