package golden.core;

import golden.enums.GoldenStatus;
import golden.holder.GoldenStaticHolder;

/**
 * 黄金基线夹具：继承父类并实现接口，同时覆盖方法调用、字段读写、try-catch-finally、throw
 */
public class GoldenChildService extends GoldenBaseService implements GoldenSpi {

    // 非静态字段，覆盖字段的读与写
    private GoldenStatus currentStatus;

    @Override
    public String exec(String task) {
        try {
            // 调用父类方法与枚举字段的方法，覆盖方法调用类型解析
            String prefix = queryBase(task);
            return prefix + "-" + currentStatus.buildLabel();
        } catch (RuntimeException e) {
            // 调用静态方法写静态字段，覆盖静态字段使用与方法调用
            GoldenStaticHolder.increaseError();
            return "error";
        } finally {
            // 覆盖finally代码块与字段写
            currentStatus = GoldenStatus.FINISHED;
        }
    }

    /**
     * 调用父类protected方法与接口常量
     */
    public int calcWithBase(int value) {
        return baseCalc(value, DEFAULT_PRIORITY) * 2;
    }

    /**
     * 覆盖throw语句
     */
    public String throwIfNegative(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("negative: " + value);
        }
        return "ok";
    }
}
