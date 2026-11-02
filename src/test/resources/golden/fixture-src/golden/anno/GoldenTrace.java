package golden.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 黄金基线夹具：方法上使用的注解，含两个属性（其中一个为boolean类型），覆盖注解属性值的解析
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GoldenTrace {

    String value();

    boolean verbose() default false;
}
