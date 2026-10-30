package org.springframework.web.bind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 测试使用的伪@GetMapping注解，仅类名与org.springframework同名，不依赖任何Spring类
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GetMapping {

    String[] value() default {};

    String[] path() default {};
}
