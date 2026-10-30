package org.springframework.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Spring的@Service注解的测试桩类，仅用于测试代码编译
 * 与Spring中的注解使用相同的全限定名，使编译后的class文件中出现与Spring注解一致的描述符，供解析逻辑识别
 * 不会被打包进target/test.jar（打包规则仅包含test目录下的内容），也不会随主代码发布
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Service {

    String value() default "";
}
