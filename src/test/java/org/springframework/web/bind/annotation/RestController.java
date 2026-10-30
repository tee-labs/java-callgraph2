package org.springframework.web.bind.annotation;

import org.springframework.stereotype.Controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 测试使用的伪@RestController注解，仅类名与org.springframework同名，不依赖任何Spring类
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Controller
public @interface RestController {
}
