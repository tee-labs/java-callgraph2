package golden.dto;

/**
 * 黄金基线夹具：dto类，覆盖字段信息、get/set方法识别，含private、protected、public字段
 */
public class GoldenDto {

    // private字段，存在对应的get与set方法
    private String name;

    // private字段，存在对应的get与set方法
    private int score;

    // protected字段，存在对应的get（is前缀）与set方法
    protected boolean valid;

    // public字段，无对应的get与set方法，供其他类直接读写，覆盖使用其他类字段的情况
    public String tag;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
