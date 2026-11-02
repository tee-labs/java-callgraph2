package golden.core;

/**
 * 黄金基线夹具：被实现的接口，覆盖接口实现关系的解析
 */
public interface GoldenSpi {

    // 接口常量
    int DEFAULT_PRIORITY = 10;

    /**
     * 接口方法，由实现类实现
     */
    String exec(String task);
}
