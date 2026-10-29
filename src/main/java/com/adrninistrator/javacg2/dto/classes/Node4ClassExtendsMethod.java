package com.adrninistrator.javacg2.dto.classes;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2021/6/27
 * @description: 处理涉及继承的类的方法时使用的栈的节点
 */

@Getter
public class Node4ClassExtendsMethod {
    // 父类类名
    private final String superClassName;

    // 当前处理的子类序号
    @Setter
    private int childClassIndex;

    public Node4ClassExtendsMethod(String superClassName, int childClassIndex) {
        this.superClassName = superClassName;
        this.childClassIndex = childClassIndex;
    }
}
