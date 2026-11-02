package golden.holder;

/**
 * 黄金基线夹具：静态字段，覆盖静态字段的初始化（含方法调用初始化）、读与写
 */
public class GoldenStaticHolder {

    // static final字段通过方法调用初始化，覆盖static、final字段初始化方法信息
    public static final String VERSION = computeVersion();

    // 静态字段，覆盖静态字段读与写
    public static int totalCalls = 0;

    // 静态字段，覆盖静态字段读与写
    public static int errorCount = 0;

    static {
        totalCalls = 100;
    }

    /**
     * 静态方法，被static final字段初始化调用
     */
    public static String computeVersion() {
        return "v1.0";
    }

    /**
     * 静态方法，写静态字段
     */
    public static void increaseCall() {
        totalCalls++;
    }

    /**
     * 静态方法，写静态字段
     */
    public static void increaseError() {
        errorCount++;
    }
}
