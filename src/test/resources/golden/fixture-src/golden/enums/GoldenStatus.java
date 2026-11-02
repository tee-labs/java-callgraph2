package golden.enums;

/**
 * 黄金基线夹具：枚举类，覆盖枚举常量构造函数参数赋值、枚举字段与枚举方法
 */
public enum GoldenStatus {

    INIT("I", 1),
    RUNNING("R", 2),
    FINISHED("F", 3);

    // 枚举字段
    private final String code;

    // 枚举字段
    private final int level;

    GoldenStatus(String code, int level) {
        this.code = code;
        this.level = level;
    }

    /**
     * 枚举实例方法，读取枚举字段
     */
    public String buildLabel() {
        return code + ":" + level;
    }

    /**
     * 枚举静态方法，遍历枚举常量
     */
    public static GoldenStatus fromCode(String code) {
        for (GoldenStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return INIT;
    }
}
