package com.adrninistrator.javacg2.el.enums;

import lombok.Getter;

/**
 * @author adrninistrator
 * @date 2025/2/22
 * @description:
 */
@Getter
public enum ElStringFunctionTwoArgsEnum {
    CONTAINS_IGNORE_CASE("string.containsIC"),
    ENDS_WITH_IGNORE_CASE("string.endsWithIC"),
    EQUALS_IGNORE_CASE("string.equalsIC"),
    STARTS_WITH_IGNORE_CASE("string.startsWithIC"),
    ;

    private final String name;

    ElStringFunctionTwoArgsEnum(String name) {
        this.name = name;
    }
}
