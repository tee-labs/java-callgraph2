package com.adrninistrator.javacg2.markdown.enums;

import lombok.Getter;

/**
 * @author adrninistrator
 * @date 2022/11/27
 * @description: markdown代码块类型枚举
 */
@Getter
public enum MDCodeBlockTypeEnum {
    MDCBTE_SQL("sql"),
    MDCBTE_JAVA("java"),
    MDCBTE_JS("js"),
    ;

    private final String type;

    MDCodeBlockTypeEnum(String type) {
        this.type = type;
    }
}
