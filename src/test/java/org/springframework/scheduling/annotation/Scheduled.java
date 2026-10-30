package org.springframework.scheduling.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 测试使用的伪@Scheduled注解，仅类名与org.springframework同名，不依赖任何Spring类
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Scheduled {

    String cron() default "";

    long fixedRate() default -1L;

    long fixedDelay() default -1L;

    long initialDelay() default -1L;
}
