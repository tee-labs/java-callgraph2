package com.adrninistrator.javacg2.common.enums;

import lombok.Getter;

/**
 * @author adrninistrator
 * @date 2023/4/12
 * @description: 生成文件类型枚举
 */
@Getter
public enum JavaCG2OutPutFileTypeEnum {

    OPFTE_CLASS_ANNOTATION("class_annotation", "类的注解"),
    OPFTE_CLASS_EXT_IMPL_GENERICS_TYPE("class_ext_impl_generics_type", "类的继承或实现的泛型信息"),
    OPFTE_CLASS_INFO("class_info", "类的信息"),
    OPFTE_CLASS_REFERENCE("class_reference", "引用的类"),
    OPFTE_DUP_CLASS_REFERENCE("dup_class_reference", "重复同名类引用的类"),
    OPFTE_CLASS_SIGNATURE_GENERICS_TYPE("class_signature_generics_type", "类的签名中的泛型信息"),
    OPFTE_DUP_CLASS_INFO("dup_class_info", "重复同名类的信息"),
    OPFTE_DUP_METHOD_INFO("dup_method_info", "重复同名类的方法的信息"),
    OPFTE_DUP_FIELD_INFO("dup_field_info", "重复同名类的字段的信息"),
    OPFTE_ENUM_INIT_ARG_FIELD("enum_init_arg_field", "枚举类构造函数参数与字段赋值关系"),
    OPFTE_ENUM_INIT_ASSIGN_INFO("enum_init_assign_info", "枚举类初始化赋值信息"),
    OPFTE_EXTENDS_IMPL("extends_impl", "继承与实现相关信息"),
    OPFTE_FIELD_ANNOTATION("field_annotation", "字段的注解"),
    OPFTE_FIELD_GENERICS_TYPE("field_generics_type", "非静态字段集合中涉及的泛型类型"),
    OPFTE_FIELD_INFO("field_info", "字段信息"),
    OPFTE_FIELD_RELATIONSHIP("field_relationship", "通过get/set方法关联的字段关系"),
    OPFTE_FIELD_USAGE_OTHER("field_usage_other", "使用其他类中字段的使用情况"),
    OPFTE_GET_METHOD("get_method", "dto的get方法及字段"),
    OPFTE_INNER_CLASS("inner_class", "内部类信息"),
    OPFTE_JAR_INFO("jar_info", "jar文件信息"),
    OPFTE_JAVACG2_CONFIG("javacg2_config", "java-callgraph2组件使用的配置参数"),
    OPFTE_LAMBDA_METHOD_INFO("lambda_method_info", "Lambda表达式方法信息"),
    OPFTE_LOG_METHOD_SPEND_TIME("_log_method_spend_time", "日志-方法处理耗时"),
    OPFTE_METHOD_ANNOTATION("method_annotation", "方法的注解"),
    OPFTE_METHOD_ARG_ANNOTATION("method_arg_annotation", "方法参数的注解"),
    OPFTE_METHOD_ARG_GENERICS_TYPE("method_arg_generics_type", "方法参数集合中涉及的泛型类型"),
    OPFTE_METHOD_ARGUMENT("method_argument", "方法参数"),
    OPFTE_METHOD_CALL("method_call", "方法调用"),
    OPFTE_METHOD_CALL_RAW_CALLEE("method_call_raw_callee", "方法调用被调用对象的原始类型"),
    OPFTE_METHOD_CALL_INFO("method_call_info", "方法调用的信息"),
    OPFTE_METHOD_CALL_METHOD_CALL_RETURN("method_call_method_call_return", "方法调用使用方法调用返回值"),
    OPFTE_METHOD_CALL_STATIC_FIELD("method_call_static_field", "方法调用使用静态字段信息"),
    OPFTE_METHOD_CALL_NON_STATIC_FIELD("method_call_non_static_field", "方法调用使用非静态字段信息"),
    OPFTE_METHOD_CALL_FIELD_ACTUAL_TYPE("method_call_field_actual_type", "方法调用被调用对象为非静态字段时的实际类型（运行时多态）"),
    OPFTE_METHOD_CALL_STATIC_FIELD_MCR("method_call_static_field_mcr", "方法调用使用静态字段方法调用返回值"),
    OPFTE_METHOD_CATCH("method_catch", "方法的catch信息"),
    OPFTE_METHOD_FINALLY("method_finally", "方法的finally信息"),
    OPFTE_METHOD_INFO("method_info", "方法的信息"),
    OPFTE_METHOD_LINE_NUMBER("method_line_number", "方法代码行号"),
    OPFTE_METHOD_RETURN_ARG_SEQ("method_return_arg_seq", "方法返回值对应的参数序号"),
    OPFTE_METHOD_RETURN_CALL_ID("method_return_call_id", "方法返回值对应的方法调用ID"),
    OPFTE_METHOD_RETURN_CONST_VALUE("method_return_const_value", "方法返回的常量值（含null）"),
    OPFTE_METHOD_RETURN_FIELD_INFO("method_return_field_info", "方法返回的字段（含枚举）"),
    OPFTE_METHOD_RETURN_GENERICS_TYPE("method_return_generics_type", "方法返回集合中涉及的泛型类型"),
    OPFTE_METHOD_THROW("method_throw", "方法通过throw抛出的异常信息"),
    OPFTE_PACKAGE_INFO("package_info", "包名信息"),
    OPFTE_SET_METHOD("set_method", "dto的set方法及字段"),
    OPFTE_SF_FIELD_METHOD_CALL("sf_field_method_call", "static、final字段初始化方法信息（含枚举）"),
    OPFTE_SPRING_BEAN("spring_bean", "Spring Bean信息"),
    OPFTE_SPRING_SCAN_PACKAGE_JAVA("spring_scan_package_java", "Java代码中定义的Spring的包扫描路径"),
    OPFTE_SPRING_AOP_ASPECT_JAVA("spring_aop_aspect_java", "Java代码中定义的Spring AOP Aspect"),
    OPFTE_SPRING_AOP_POINTCUT_JAVA("spring_aop_pointcut_java", "Java代码中定义的Spring AOP Pointcut"),
    OPFTE_SPRING_AOP_ADVICE_JAVA("spring_aop_advice_java", "Java代码中定义的Spring AOP Advice"),
    OPFTE_SPRING_DI_FIELD("spring_di_field", "Spring依赖注入字段信息"),
    OPFTE_SPRING_TASK_XML("spring_task_xml", "Spring定时任务信息，通过XML定义"),
    OPFTE_SPRING_AOP_ASPECT_XML("spring_aop_aspect_xml", "Spring AOP aspect信息，在XML中定义"),
    OPFTE_SPRING_AOP_POINTCUT_XML("spring_aop_pointcut_xml", "Spring AOP pointcut信息，在XML中定义"),
    OPFTE_SPRING_AOP_ADVICE_XML("spring_aop_advice_xml", "Spring AOP advice信息，在XML中定义"),
    OPFTE_SPRING_SCAN_PACKAGE_XML("spring_scan_package_xml", "Spring的包扫描路径，在XML文件中定义"),
    OPFTE_COMPATIBILITY_CLASS_REFERENCE_MISSING("compatibility_class_reference_missing", "Jar兼容性检查-被引用的类不存在"),
    OPFTE_COMPATIBILITY_METHOD_CALL_MISSING("compatibility_method_call_missing", "Jar兼容性检查-方法调用中的被调用类/方法不存在或不可访问"),
    OPFTE_COMPATIBILITY_FIELD_USAGE_MISSING("compatibility_field_usage_missing", "Jar兼容性检查-被使用的字段不存在或不可访问"),
    OPFTE_COMPATIBILITY_DUP_CLASS_CONFLICT("compatibility_dup_class_conflict", "Jar兼容性检查-重复同名类冲突"),
    OPFTE_ILLEGAL("illegal", "非法值"),
    ;

    private final String fileName;
    private final String desc;

    JavaCG2OutPutFileTypeEnum(String fileName, String desc) {
        this.fileName = fileName;
        this.desc = desc;
    }
}
