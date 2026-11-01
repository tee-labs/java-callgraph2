package com.adrninistrator.javacg2.common.enums;

import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchema;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author adrninistrator
 * @date 2023/4/12
 * @description: 生成文件类型枚举
 * 有固定列结构的文件类型在当前枚举常量上定义列结构（JavaCG2OutputFileSchema实例），作为结果文件各列名称与列数的单一来源
 * 列名与列数以各结果文件实际写入的数据为准
 * 未定义列结构的文件类型：无固定列结构的文件（_log_method_spend_time）、不代表实际文件的非法值
 * jar_info文件定义了列结构但不允许输出表头（文件中包含jar_info数据行之外的特殊行）
 */
@Getter
public enum JavaCG2OutPutFileTypeEnum {

    /*
        注解类文件：注解没有属性值时省略“注解属性名称”“注解属性值”两列，最小列数为列数减2
     */
    OPFTE_CLASS_ANNOTATION("class_annotation", "类的注解", new JavaCG2OutputFileSchema(2,
            "完整类名", "注解类名", "注解属性名称", "注解属性值")),
    OPFTE_CLASS_EXT_IMPL_GENERICS_TYPE("class_ext_impl_generics_type", "类的继承或实现的泛型信息", new JavaCG2OutputFileSchema(
            "完整类名", "继承或实现类型", "继承或实现的序号", "父类或接口的完整类名", "泛型类型序号", "泛型类型类名", "泛型数组类型的维度", "泛型类型变量名称", "泛型类型分类")),
    OPFTE_CLASS_INFO("class_info", "类的信息", new JavaCG2OutputFileSchema(
            "完整类名", "类的access_flags", "类文件的HASH值", "类所在的jar文件序号", "类在jar包中的路径")),
    OPFTE_CLASS_REFERENCE("class_reference", "引用的类", new JavaCG2OutputFileSchema(
            "引用的完整类名", "被引用的完整类名", "类所在的jar文件序号")),
    OPFTE_DUP_CLASS_REFERENCE("dup_class_reference", "重复同名类引用的类", new JavaCG2OutputFileSchema(
            "引用的完整类名", "被引用的完整类名", "类所在的jar文件序号")),
    OPFTE_CLASS_SIGNATURE_GENERICS_TYPE("class_signature_generics_type", "类的签名中的泛型信息", new JavaCG2OutputFileSchema(
            "完整类名", "序号", "泛型类型变量名称", "泛型的父类类名")),
    OPFTE_DUP_CLASS_INFO("dup_class_info", "重复同名类的信息", new JavaCG2OutputFileSchema(
            "完整类名", "类的access_flags", "类文件的HASH值", "类所在的jar文件序号", "类在jar包中的路径")),
    OPFTE_DUP_METHOD_INFO("dup_method_info", "重复同名类的方法的信息", new JavaCG2OutputFileSchema(
            "完整方法", "方法的access_flags", "返回类型类名", "返回类型数组的维度", "返回类型分类", "返回类型是否存在泛型类型", "方法指令的HASH值", "方法所在的jar文件序号")),
    OPFTE_DUP_FIELD_INFO("dup_field_info", "重复同名类的字段的信息", new JavaCG2OutputFileSchema(
            "完整类名", "字段名称", "字段类型", "字段数组类型的维度", "字段中的泛型类型分类", "字段修饰符", "基本类型", "static标志", "final标志", "是否存在对应的get方法",
            "是否存在对应的set方法", "是否存在泛型类型", "字段所在的jar文件序号")),
    OPFTE_ENUM_INIT_ARG_FIELD("enum_init_arg_field", "枚举类构造函数参数与字段赋值关系", new JavaCG2OutputFileSchema(
            "枚举类构造函数完整方法", "用于赋值的参数序号", "被赋值的字段类型", "被赋值的字段名")),
    OPFTE_ENUM_INIT_ASSIGN_INFO("enum_init_assign_info", "枚举类初始化赋值信息", new JavaCG2OutputFileSchema(
            "枚举类构造函数完整方法", "枚举常量名称", "枚举字段序号", "被赋值的参数序号", "被赋值的字段类型", "字段值是否BASE64编码", "被赋值的字段值")),
    OPFTE_EXTENDS_IMPL("extends_impl", "继承与实现相关信息", new JavaCG2OutputFileSchema(
            "完整类名", "类的access_flags", "类型", "父类或接口的完整类名")),
    OPFTE_FIELD_ANNOTATION("field_annotation", "字段的注解", new JavaCG2OutputFileSchema(3,
            "完整类名", "字段名称", "注解类名", "注解属性名称", "注解属性值")),
    OPFTE_FIELD_GENERICS_TYPE("field_generics_type", "非静态字段集合中涉及的泛型类型", new JavaCG2OutputFileSchema(
            "完整类名", "字段名", "类型", "类型序号", "类型类名", "数组类型的维度", "类型变量名称", "泛型通配符", "泛型通配符引用的类型", "类型分类")),
    OPFTE_FIELD_INFO("field_info", "字段信息", new JavaCG2OutputFileSchema(
            "完整类名", "字段名称", "字段类型", "字段数组类型的维度", "字段中的泛型类型分类", "字段修饰符", "基本类型", "static标志", "final标志", "是否存在对应的get方法",
            "是否存在对应的set方法", "是否存在泛型类型", "字段所在的jar文件序号")),
    OPFTE_FIELD_RELATIONSHIP("field_relationship", "通过get/set方法关联的字段关系", new JavaCG2OutputFileSchema(
            "关联关系id", "get方法调用序号", "set方法调用序号", "调用方完整方法", "调用方源代码行号", "get方法完整类名", "get方法方法名", "set方法完整类名", "set方法方法名",
            "关联关系是否有效", "关联关系类型")),
    OPFTE_FIELD_USAGE_OTHER("field_usage_other", "使用其他类中字段的使用情况", new JavaCG2OutputFileSchema(
            "完整方法", "方法返回类型", "static标志", "使用字段时get还是put", "被使用的字段所在的类名", "被使用的字段名称", "被使用的字段类型", "使用字段的源代码行号",
            "类所在的jar文件序号", "被使用的字段所在的jar文件序号")),
    OPFTE_GET_METHOD("get_method", "dto的get方法及字段", new JavaCG2OutputFileSchema(
            "完整类名", "方法名", "字段名", "字段分类", "字段类型", "字段数组类型的维度", "完整方法", "方法返回类型")),
    OPFTE_INNER_CLASS("inner_class", "内部类信息", new JavaCG2OutputFileSchema(
            "内部类完整类名", "外部类完整类名", "是否为匿名内部类")),
    /*
        jar_info文件中除数据行外还写入结果目录、最终解析的jar文件等特殊行，不允许输出表头
     */
    OPFTE_JAR_INFO("jar_info", "jar文件信息", new JavaCG2OutputFileSchema(4, false,
            "jar文件类型", "jar文件序号", "外层jar文件完整路径", "jar文件中的jar文件路径")),
    OPFTE_JAVACG2_CONFIG("javacg2_config", "java-callgraph2组件使用的配置参数", new JavaCG2OutputFileSchema(
            "配置文件名", "配置参数名", "配置参数值", "配置参数类型")),
    /*
        Lambda表达式方法信息：未找到下一个被调用方法时省略最后一列，最小列数为2
     */
    OPFTE_LAMBDA_METHOD_INFO("lambda_method_info", "Lambda表达式方法信息", new JavaCG2OutputFileSchema(2,
            "方法调用序号", "被调用方完整方法", "下一个被调用完整方法")),
    // 无固定列结构，不定义列结构
    OPFTE_LOG_METHOD_SPEND_TIME("_log_method_spend_time", "日志-方法处理耗时"),
    OPFTE_METHOD_ANNOTATION("method_annotation", "方法的注解", new JavaCG2OutputFileSchema(4,
            "完整方法", "方法返回类型", "方法所在的jar文件序号", "注解类名", "注解属性名称", "注解属性值")),
    OPFTE_METHOD_ARG_ANNOTATION("method_arg_annotation", "方法参数的注解", new JavaCG2OutputFileSchema(4,
            "完整方法", "方法返回类型", "参数序号", "注解类名", "注解属性名称", "注解属性值")),
    OPFTE_METHOD_ARG_GENERICS_TYPE("method_arg_generics_type", "方法参数集合中涉及的泛型类型", new JavaCG2OutputFileSchema(
            "完整方法", "方法返回类型", "参数序号", "类型", "类型序号", "类型类名", "数组类型的维度", "类型变量名称", "泛型通配符", "泛型通配符引用的类型", "类型分类")),
    OPFTE_METHOD_ARGUMENT("method_argument", "方法参数", new JavaCG2OutputFileSchema(
            "完整方法", "方法返回类型", "参数序号", "参数名称", "参数类型", "参数数组类型的维度", "参数类型分类", "是否存在泛型类型")),
    OPFTE_METHOD_CALL("method_call", "方法调用", new JavaCG2OutputFileSchema(
            "方法调用序号", "是否启用", "调用方完整方法", "方法调用类型及被调用方完整方法", "调用方法源代码行号", "调用方法返回类型", "被调用对象数组的维度", "被调用对象类型",
            "被调用方法原始的返回类型", "被调用方法实际的返回类型", "调用方法jar文件序号", "被调用方法jar文件序号", "描述信息")),
    OPFTE_METHOD_CALL_RAW_CALLEE("method_call_raw_callee", "方法调用被调用对象的原始类型", new JavaCG2OutputFileSchema(
            "方法调用序号", "原始的被调用完整类名")),
    OPFTE_METHOD_CALL_INFO("method_call_info", "方法调用的信息", new JavaCG2OutputFileSchema(
            "方法调用序号", "被调用对象或参数序号", "序号", "类型", "是否为数组格式", "数组值组合序号", "数组维度", "数组下标", "值的类型", "对应的值", "调用方完整方法",
            "方法返回类型")),
    OPFTE_METHOD_CALL_METHOD_CALL_RETURN("method_call_method_call_return", "方法调用使用方法调用返回值", new JavaCG2OutputFileSchema(
            "方法调用序号", "被调用对象或参数序号", "序号", "是否为数组格式", "返回值被使用的方法调用序号", "被调用方完整方法", "被调用方方法返回类型")),
    OPFTE_METHOD_CALL_STATIC_FIELD("method_call_static_field", "方法调用使用静态字段信息", new JavaCG2OutputFileSchema(
            "方法调用序号", "被调用对象或参数序号", "序号", "静态字段所在类完整类名", "静态字段名称", "静态字段类型", "调用方完整方法", "调用方方法返回类型")),
    OPFTE_METHOD_CALL_NON_STATIC_FIELD("method_call_non_static_field", "方法调用使用非静态字段信息", new JavaCG2OutputFileSchema(
            "方法调用序号", "被调用对象或参数序号", "序号", "非静态字段所在类完整类名", "非静态字段名称", "非静态字段类型", "调用方完整方法", "调用方方法返回类型")),
    OPFTE_METHOD_CALL_FIELD_ACTUAL_TYPE("method_call_field_actual_type", "方法调用被调用对象为非静态字段时的实际类型（运行时多态）", new JavaCG2OutputFileSchema(
            "调用方完整方法", "调用方法源代码行号", "被调用方字段声明类型", "被调用方字段名称", "被调用方字段实际类型")),
    OPFTE_METHOD_CALL_STATIC_FIELD_MCR("method_call_static_field_mcr", "方法调用使用静态字段方法调用返回值", new JavaCG2OutputFileSchema(
            "方法调用序号", "被调用对象或参数序号", "序号", "静态字段所在类完整类名", "静态字段名称", "静态字段类型", "被调用方完整方法", "被调用方方法返回类型",
            "调用方完整方法", "调用方方法返回类型")),
    OPFTE_METHOD_CATCH("method_catch", "方法的catch信息", new JavaCG2OutputFileSchema(
            "完整方法", "方法返回类型", "catch捕获的异常类型", "catch标志", "try代码块开始代码行号", "try代码块结束代码行号", "try代码块最小方法调用ID", "try代码块最大方法调用ID",
            "catch代码块开始指令偏移量", "catch代码块结束指令偏移量", "catch代码块开始代码行号", "catch代码块结束代码行号", "catch代码块最小方法调用ID", "catch代码块最大方法调用ID")),
    OPFTE_METHOD_FINALLY("method_finally", "方法的finally信息", new JavaCG2OutputFileSchema(
            "完整方法", "方法返回类型", "finally对应的try或catch", "try或catch代码块开始代码行号", "try或catch代码块结束代码行号", "try或catch代码块最小方法调用ID",
            "try或catch代码块最大方法调用ID", "finally代码块开始代码行号")),
    OPFTE_METHOD_INFO("method_info", "方法的信息", new JavaCG2OutputFileSchema(
            "完整方法", "方法的access_flags", "返回类型类名", "返回类型数组的维度", "返回类型分类", "返回类型是否存在泛型类型", "方法指令的HASH值", "方法所在的jar文件序号")),
    OPFTE_METHOD_LINE_NUMBER("method_line_number", "方法代码行号", new JavaCG2OutputFileSchema(
            "完整方法", "方法返回类型", "起始代码行号", "结束代码行号")),
    OPFTE_METHOD_RETURN_ARG_SEQ("method_return_arg_seq", "方法返回值对应的参数序号", new JavaCG2OutputFileSchema(
            "完整方法", "方法返回类型", "方法返回值对应的方法参数序号", "是否返回等值转换前的方法参数")),
    OPFTE_METHOD_RETURN_CALL_ID("method_return_call_id", "方法返回值对应的方法调用ID", new JavaCG2OutputFileSchema(
            "完整方法", "方法返回类型", "方法返回值对应的方法调用序号", "是否返回等值转换前的方法调用")),
    OPFTE_METHOD_RETURN_CONST_VALUE("method_return_const_value", "方法返回的常量值（含null）", new JavaCG2OutputFileSchema(
            "完整方法", "方法返回类型", "常量值序号", "常量类型", "常量值是否BASE64编码", "常量的值")),
    OPFTE_METHOD_RETURN_FIELD_INFO("method_return_field_info", "方法返回的字段（含枚举）", new JavaCG2OutputFileSchema(
            "完整方法", "方法返回类型", "字段信息序号", "字段是否为静态", "字段是否属于this对象", "字段所在的类完整类名", "字段类型完整类名", "字段数组类型的维度", "字段名称")),
    OPFTE_METHOD_RETURN_GENERICS_TYPE("method_return_generics_type", "方法返回集合中涉及的泛型类型", new JavaCG2OutputFileSchema(
            "完整方法", "方法返回类型", "类型", "类型序号", "类型类名", "数组类型的维度", "类型变量名称", "泛型通配符", "泛型通配符引用的类型", "类型分类")),
    OPFTE_METHOD_THROW("method_throw", "方法通过throw抛出的异常信息", new JavaCG2OutputFileSchema(
            "完整方法", "方法返回类型", "throw指令的偏移量", "throw的代码行号", "序号", "throw的异常类型", "throw的标志", "catch代码块开始指令偏移量",
            "catch的异常对象变量名称", "对应的方法调用ID")),
    OPFTE_PACKAGE_INFO("package_info", "包名信息", new JavaCG2OutputFileSchema(
            "包名", "包名层级", "类所在的jar文件序号", "jar文件名")),
    OPFTE_SET_METHOD("set_method", "dto的set方法及字段", new JavaCG2OutputFileSchema(
            "完整类名", "方法名", "字段名", "字段分类", "字段类型", "字段数组类型的维度", "完整方法", "方法返回类型")),
    OPFTE_SF_FIELD_METHOD_CALL("sf_field_method_call", "static、final字段初始化方法信息（含枚举）", new JavaCG2OutputFileSchema(
            "字段所在的完整类名", "字段名称", "序号", "字段初始化对应的方法调用序号", "字段类型", "字段数组类型的维度", "初始化方法被调类名", "初始化方法被调用方法名")),
    OPFTE_SPRING_BEAN("spring_bean", "Spring Bean信息", new JavaCG2OutputFileSchema(
            "Spring Bean的名称", "序号", "完整类名", "profile", "Spring Bean的定义方式", "在Java代码中定义时对应的注解类名", "在Java代码中定义时所在的类名或在XML中定义时对应的文件路径")),
    OPFTE_SPRING_SCAN_PACKAGE_JAVA("spring_scan_package_java", "Java代码中定义的Spring的包扫描路径", new JavaCG2OutputFileSchema(
            "包扫描路径类型", "序号", "包扫描路径", "在Java代码中定义时所在的类名或在XML中定义时对应的文件路径")),
    OPFTE_SPRING_AOP_ASPECT_JAVA("spring_aop_aspect_java", "Java代码中定义的Spring AOP Aspect", new JavaCG2OutputFileSchema(
            "aspect排序数值", "aspect在Java代码中定义时所在的类名")),
    OPFTE_SPRING_AOP_POINTCUT_JAVA("spring_aop_pointcut_java", "Java代码中定义的Spring AOP Pointcut", new JavaCG2OutputFileSchema(
            "pointcut表达式是否为base64格式", "pointcut表达式", "pointcut在Java代码中定义时所在的完整方法")),
    OPFTE_SPRING_AOP_ADVICE_JAVA("spring_aop_advice_java", "Java代码中定义的Spring AOP Advice", new JavaCG2OutputFileSchema(
            "advice的完整方法", "advice的方法返回类型", "Java代码中定义的advice注解简单类名", "pointcut表达式是否为base64格式", "pointcut表达式", "aspect排序数值")),
    OPFTE_SPRING_DI_FIELD("spring_di_field", "Spring依赖注入字段信息", new JavaCG2OutputFileSchema(
            "完整类名", "字段类型", "字段名称", "Spring Bean类型")),
    OPFTE_SPRING_TASK_XML("spring_task_xml", "Spring定时任务信息，通过XML定义", new JavaCG2OutputFileSchema(
            "Spring Bean的名称", "方法名", "XML文件路径")),
    OPFTE_SPRING_AOP_ASPECT_XML("spring_aop_aspect_xml", "Spring AOP aspect信息，在XML中定义", new JavaCG2OutputFileSchema(
            "XML中定义的aspect的ID", "XML中定义的aspect对应的Bean名称", "aspect排序数值", "XML文件路径")),
    OPFTE_SPRING_AOP_POINTCUT_XML("spring_aop_pointcut_xml", "Spring AOP pointcut信息，在XML中定义", new JavaCG2OutputFileSchema(
            "XML中定义的pointcut的ID", "pointcut表达式是否为base64格式", "pointcut表达式", "XML文件路径")),
    OPFTE_SPRING_AOP_ADVICE_XML("spring_aop_advice_xml", "Spring AOP advice信息，在XML中定义", new JavaCG2OutputFileSchema(
            "XML中定义的aspect的ID", "XML中定义的aspect的方法名", "XML中定义的advice XML元素的名称", "XML中的pointcut-ref名称", "pointcut表达式是否为base64格式",
            "pointcut表达式", "aspect排序数值", "XML文件路径")),
    OPFTE_SPRING_SCAN_PACKAGE_XML("spring_scan_package_xml", "Spring的包扫描路径，在XML文件中定义", new JavaCG2OutputFileSchema(
            "包扫描路径类型", "序号", "包扫描路径", "XML文件路径")),
    OPFTE_COMPATIBILITY_CLASS_REFERENCE_MISSING("compatibility_class_reference_missing", "Jar兼容性检查-被引用的类不存在", new JavaCG2OutputFileSchema(
            "引用方完整类名", "被引用的完整类名", "引用方所在的jar文件序号", "引用方所在jar文件路径", "引用方所在jar文件的内部路径")),
    OPFTE_COMPATIBILITY_METHOD_CALL_MISSING("compatibility_method_call_missing", "Jar兼容性检查-方法调用中的被调用类/方法不存在或不可访问", new JavaCG2OutputFileSchema(
            "问题描述", "调用方完整方法", "调用方法源代码行号", "方法调用类型", "被调用方完整方法", "被调用方法原始的返回类型", "调用方所在jar文件序号", "调用方所在jar文件路径",
            "调用方所在jar文件的内部路径", "检查明细")),
    OPFTE_COMPATIBILITY_FIELD_USAGE_MISSING("compatibility_field_usage_missing", "Jar兼容性检查-被使用的字段不存在或不可访问", new JavaCG2OutputFileSchema(
            "问题描述", "使用字段的方法完整方法", "使用字段时get还是put", "被使用的字段所在的类名", "被使用的字段名称", "被使用的字段类型", "使用字段的类所在jar文件序号",
            "使用字段的类所在jar文件路径", "使用字段的类所在jar文件的内部路径", "检查明细")),
    OPFTE_COMPATIBILITY_DUP_CLASS_CONFLICT("compatibility_dup_class_conflict", "Jar兼容性检查-重复同名类冲突", new JavaCG2OutputFileSchema(
            "问题类型", "完整类名", "类版本1的class文件在jar文件中的路径", "类版本1的class文件所在jar文件序号", "类版本1的class文件所在jar文件路径",
            "类版本2的class文件在jar文件中的路径", "类版本2的class文件所在jar文件序号", "类版本2的class文件所在jar文件路径", "方法名及参数或字段名称", "方法返回类型或字段类型",
            "问题描述")),
    OPFTE_SPRING_CONTROLLER("spring_controller", "Spring Controller信息", new JavaCG2OutputFileSchema(
            "记录id", "方法hash+字节数", "序号", "用于显示的URI", "类上的注解path属性原始值", "方法上的注解path属性原始值", "注解类名", "唯一类名",
            "方法所在的jar文件序号", "方法可能用于文件上传", "方法可能用于文件下载", "完整方法", "方法返回类型")),
    OPFTE_SPRING_TASK_JAVA("spring_task_java", "Spring定时任务信息，通过Java代码注解定义", new JavaCG2OutputFileSchema(
            "记录id", "方法hash+字节数", "Spring Bean的名称", "完整类名", "方法名", "类型", "完整方法", "方法返回类型", "在Java代码中定义时所在的类名")),
    // 非法值，不代表实际的文件，不定义列结构
    OPFTE_ILLEGAL("illegal", "非法值"),
    ;

    // 文件名转文件类型枚举的Map
    private static final Map<String, JavaCG2OutPutFileTypeEnum> FILE_NAME_ENUM_MAP = new HashMap<>();

    static {
        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : values()) {
            FILE_NAME_ENUM_MAP.put(fileTypeEnum.fileName, fileTypeEnum);
        }
    }

    private final String fileName;
    private final String desc;
    // 输出文件的列结构，未定义列结构的文件类型为null
    private final JavaCG2OutputFileSchema schema;

    JavaCG2OutPutFileTypeEnum(String fileName, String desc) {
        this(fileName, desc, null);
    }

    JavaCG2OutPutFileTypeEnum(String fileName, String desc, JavaCG2OutputFileSchema schema) {
        this.fileName = fileName;
        this.desc = desc;
        this.schema = schema;
    }

    /**
     * 根据文件名查询文件类型枚举
     *
     * @param fileName 文件名，不包含文件后缀
     * @return 文件类型枚举，未找到时返回null
     */
    public static JavaCG2OutPutFileTypeEnum getFromFileName(String fileName) {
        return FILE_NAME_ENUM_MAP.get(fileName);
    }
}
