package golden.dto;

/**
 * 黄金基线夹具：dto的使用方，覆盖get/set方法调用、字段关联关系分析、直接读写其他类的public字段
 */
public class GoldenDtoUser {

    /**
     * 通过set方法写dto字段，直接写其他类的public字段
     */
    public GoldenDto buildAndFill(String name, int score) {
        GoldenDto dto = new GoldenDto();
        dto.setName(name);
        dto.setScore(score);
        dto.setValid(true);
        dto.tag = "from-user";
        return dto;
    }

    /**
     * set方法参数直接使用get方法的返回值，覆盖通过get/set方法关联的字段关系分析（直接赋值）
     */
    public void copyScore(GoldenDto source, GoldenDto target) {
        target.setScore(source.getScore());
    }

    /**
     * set方法参数使用get方法返回值等值转换后的值，覆盖通过get/set方法关联的字段关系分析（等值转换）
     */
    public int increaseScore(GoldenDto dto, int delta) {
        dto.setScore(dto.getScore() + delta);
        return dto.getScore();
    }

    /**
     * 通过get方法读dto字段，读与写其他类的public字段
     */
    public String readTag(GoldenDto dto) {
        String name = dto.getName();
        dto.tag = dto.tag + "-" + name;
        return dto.tag;
    }

    /**
     * 通过get方法读dto字段并参与比较
     */
    public boolean checkValid(GoldenDto dto) {
        return dto.isValid() && dto.getScore() > 0;
    }
}
