package golden.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 黄金基线夹具：类与方法参数上使用的注解，含两个属性，覆盖注解属性值的解析
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface GoldenMarker {

    String value() default "";

    int order() default 0;
}
