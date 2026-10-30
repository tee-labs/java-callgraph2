package com.adrninistrator.javacg2.common;

/**
 * @author adrninistrator
 * @date 2022/10/3
 * @description:
 */
public class SpringAnnotationConstants {

    public static final String[] ANNOTATION_TYPE_COMPONENTS = new String[]{
            "Lorg/springframework/stereotype/Component;",
            "Lorg/springframework/stereotype/Controller;",
            "Lorg/springframework/stereotype/Repository;",
            "Lorg/springframework/stereotype/Service;",
            "Lorg/springframework/web/bind/annotation/RestController;"
    };

    public static final String ANNOTATION_ATTRIBUTE_VALUE = "value";
    public static final String ANNOTATION_ATTRIBUTE_NAME = "name";
    public static final String ANNOTATION_ATTRIBUTE_TYPE = "type";
    public static final String ANNOTATION_ATTRIBUTE_BASE_PACKAGES = "basePackages";
    public static final String ANNOTATION_ATTRIBUTE_POINTCUT = "pointcut";

    public static final String ANNOTATION_TYPE_RESOURCE = "Ljavax/annotation/Resource;";
    public static final String ANNOTATION_TYPE_AUTOWIRED = "Lorg/springframework/beans/factory/annotation/Autowired;";
    public static final String ANNOTATION_TYPE_QUALIFIER = "Lorg/springframework/beans/factory/annotation/Qualifier;";
    public static final String ANNOTATION_TYPE_CONFIGURATION = "Lorg/springframework/context/annotation/Configuration;";
    public static final String ANNOTATION_TYPE_BEAN = "Lorg/springframework/context/annotation/Bean;";
    public static final String ANNOTATION_TYPE_COMPONENT_SCAN = "Lorg/springframework/context/annotation/ComponentScan;";
    public static final String ANNOTATION_TYPE_ASPECT = "Lorg/aspectj/lang/annotation/Aspect;";
    public static final String ANNOTATION_TYPE_NAMED = "Ljavax/inject/Named;";
    public static final String ANNOTATION_TYPE_INJECT = "Ljavax/inject/Inject;";

    public static final String ANNOTATION_TYPE_ORDER = "Lorg/springframework/core/annotation/Order;";

    public static final String[] ANNOTATION_TYPE_ADVICES = new String[]{
            "Lorg/aspectj/lang/annotation/AfterReturning;",
            "Lorg/aspectj/lang/annotation/AfterThrowing;",
            "Lorg/aspectj/lang/annotation/After;",
            "Lorg/aspectj/lang/annotation/Around;",
            "Lorg/aspectj/lang/annotation/Before;",
    };

    public static final String ANNOTATION_TYPE_POINTCUT = "Lorg/aspectj/lang/annotation/Pointcut;";

    /*
        以下为Java风格的Spring MVC与定时任务相关注解的完整类名
        用于从类/方法的注解中派生Spring Controller与Spring定时任务信息，派生规则与java-all-call-graph的WriteDbHandler4ClassAnnotation、
        WriteDbHandler4MethodAnnotation中对应逻辑保持一致
     */

    // Spring MVC的Controller注解
    public static final String ANNOTATION_NAME_CONTROLLER = "org.springframework.stereotype.Controller";

    // Spring MVC的RestController注解
    public static final String ANNOTATION_NAME_REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";

    // Spring MVC的Controller相关注解
    public static final String[] ANNOTATION_NAME_CONTROLLERS = new String[]{
            ANNOTATION_NAME_CONTROLLER,
            ANNOTATION_NAME_REST_CONTROLLER
    };

    // Spring MVC的RequestMapping注解
    public static final String ANNOTATION_NAME_REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping";

    // Spring MVC的RequestMapping相关注解
    public static final String[] ANNOTATION_NAME_REQUEST_MAPPINGS = new String[]{
            ANNOTATION_NAME_REQUEST_MAPPING,
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PatchMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping"
    };

    // Spring定时任务的Scheduled注解
    public static final String ANNOTATION_NAME_SCHEDULED = "org.springframework.scheduling.annotation.Scheduled";

    // Spring MVC的RequestMapping相关注解中代表路径的属性名称
    public static final String ANNOTATION_ATTRIBUTE_PATH = "path";

    public static final String ANNOTATION_CLASS_NAME_BEAN = "org.springframework.context.annotation.Bean";

    public static final String ANNOTATION_SIMPLE_CLASS_NAME_AFTER_RETURNING = "AfterReturning";
    public static final String ANNOTATION_SIMPLE_CLASS_NAME_AFTER_THROWING = "AfterThrowing";
    public static final String ANNOTATION_SIMPLE_CLASS_NAME_AFTER = "After";

    public static final String ANNOTATION_SIMPLE_CLASS_NAME_AROUND = "Around";
    public static final String ANNOTATION_SIMPLE_CLASS_NAME_BEFORE = "Before";


    private SpringAnnotationConstants() {
        throw new IllegalStateException("illegal");
    }
}
