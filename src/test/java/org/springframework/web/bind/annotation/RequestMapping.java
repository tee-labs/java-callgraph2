package org.springframework.web.bind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 测试使用的伪@RequestMapping注解，仅类名与org.springframework同名，不依赖任何Spring类
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequestMapping {

    String[] value() default {};

    String[] path() default {};

    // 真实注解中该属性为RequestMethod枚举数组，测试中简化为String，用于验证非路径属性不被用于拼接URI
    String method() default "";
}
