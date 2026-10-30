package com.adrninistrator.javacg2.handler;

import com.adrninistrator.javacg2.common.JavaCG2CommonNameConstants;
import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.enums.JavaCG2ConstantTypeEnum;
import com.adrninistrator.javacg2.common.enums.JavaCG2YesNoEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfInfo;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.dto.counter.JavaCG2Counter;
import com.adrninistrator.javacg2.dto.field.FieldPossibleTypes;
import com.adrninistrator.javacg2.dto.inputoutput.JavaCG2InputAndOutput;
import com.adrninistrator.javacg2.dto.instruction.InvokeInstructionPosAndCallee;
import com.adrninistrator.javacg2.dto.jar.ClassAndJarNum;
import com.adrninistrator.javacg2.dto.type.JavaCG2GenericsType;
import com.adrninistrator.javacg2.dto.type.JavaCG2Type;
import com.adrninistrator.javacg2.el.manager.JavaCG2ElManager;
import com.adrninistrator.javacg2.extensions.annotationattributes.AnnotationAttributesFormatterInterface;
import com.adrninistrator.javacg2.extensions.manager.ExtensionsManager;
import com.adrninistrator.javacg2.spring.SpringDefineControllerTaskHandler;
import com.adrninistrator.javacg2.spring.UseSpringBeanByAnnotationHandler;
import com.adrninistrator.javacg2.util.JavaCG2AnnotationUtil;
import com.adrninistrator.javacg2.util.JavaCG2ByteCodeUtil;
import com.adrninistrator.javacg2.util.JavaCG2ClassMethodUtil;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import com.adrninistrator.javacg2.util.JavaCG2GenericsTypeUtil;
import com.adrninistrator.javacg2.writer.WriterSupportSkip;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSink;
import copy.javassist.bytecode.BadBytecode;
import copy.javassist.bytecode.SignatureAttribute;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.MethodParameter;
import org.apache.bcel.classfile.MethodParameters;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author adrninistrator
 * @date 2021/8/21
 * @description: 对类进行处理
 */
@Slf4j
public class ClassHandler {

    private final JavaClass javaClass;

    private final String classFileName;

    private final ConstantPoolGen cpg;

    private final Set<String> handledMethodNameArgsReturnTypeSet;

    private final String className;

    private final JavaCG2InputAndOutput javaCG2InputAndOutput;

    private final int classJarNum;

    private final boolean logMethodSpendTime;

    private final JavaCG2ConfInfo javaCG2ConfInfo;

    private final JavaCG2ElManager javaCG2ElManager;

    @Setter
    private UseSpringBeanByAnnotationHandler useSpringBeanByAnnotationHandler;

    @Setter
    private SpringDefineControllerTaskHandler springDefineControllerTaskHandler;

    @Setter
    private Map<String, Boolean> runnableImplClassMap;
    @Setter
    private Map<String, Boolean> callableImplClassMap;
    @Setter
    private Map<String, Boolean> transactionCallbackImplClassMap;
    @Setter
    private Map<String, Boolean> transactionCallbackWithoutResultChildClassMap;
    @Setter
    private Map<String, Boolean> threadChildClassMap;

    private ExtensionsManager extensionsManager;

    @Setter
    private JavaCG2Counter callIdCounter;
    @Setter
    private JavaCG2Counter methodNumCounter;
    @Setter
    private JavaCG2Counter failCounter;
    @Setter
    private JavaCG2Counter fieldRelationshipCounter;

    @Setter
    private JavaCG2RecordSink classAnnotationSink;
    @Setter
    private JavaCG2RecordSink classReferenceSink;
    @Setter
    private JavaCG2RecordSink dupClassReferenceSink;
    @Setter
    private JavaCG2RecordSink dupMethodInfoSink;
    @Setter
    private JavaCG2RecordSink dupFieldInfoSink;
    @Setter
    private JavaCG2RecordSink enumInitArgFieldSink;
    @Setter
    private JavaCG2RecordSink enumInitAssignInfoSink;
    @Setter
    private JavaCG2RecordSink fieldAnnotationSink;
    @Setter
    private JavaCG2RecordSink fieldGenericsTypeSink;
    @Setter
    private JavaCG2RecordSink fieldInfoSink;
    @Setter
    private JavaCG2RecordSink fieldRelationshipSink;
    @Setter
    private JavaCG2RecordSink fieldUsageOtherSink;
    @Setter
    private JavaCG2RecordSink springDIFieldSink;
    @Setter
    private JavaCG2RecordSink getMethodSink;
    @Setter
    private JavaCG2RecordSink lambdaMethodInfoSink;
    @Setter
    private JavaCG2RecordSink methodAnnotationSink;
    @Setter
    private JavaCG2RecordSink methodArgAnnotationSink;
    @Setter
    private JavaCG2RecordSink methodArgGenericsTypeSink;
    @Setter
    private JavaCG2RecordSink methodArgumentSink;
    @Setter
    private JavaCG2RecordSink methodCallInfoSink;
    @Setter
    private JavaCG2RecordSink methodCallMethodCallReturnSink;
    @Setter
    private JavaCG2RecordSink methodCallStaticFieldSink;
    @Setter
    private JavaCG2RecordSink methodCallNonStaticFieldSink;
    @Setter
    private JavaCG2RecordSink methodCallFieldActualTypeSink;
    @Setter
    private JavaCG2RecordSink methodCallStaticFieldMCRSink;
    @Setter
    private JavaCG2RecordSink methodCallSink;
    @Setter
    private JavaCG2RecordSink methodCallRawCalleeSink;
    @Setter
    private JavaCG2RecordSink methodCatchSink;
    @Setter
    private JavaCG2RecordSink methodFinallySink;
    @Setter
    private JavaCG2RecordSink methodInfoSink;
    @Setter
    private JavaCG2RecordSink methodLineNumberSink;
    @Setter
    private JavaCG2RecordSink methodReturnArgSeqSink;
    @Setter
    private JavaCG2RecordSink methodReturnCallIdSink;
    @Setter
    private JavaCG2RecordSink methodReturnConstValueSink;
    @Setter
    private JavaCG2RecordSink methodReturnFieldInfoSink;
    @Setter
    private JavaCG2RecordSink methodReturnGenericsTypeSink;
    @Setter
    private JavaCG2RecordSink methodThrowSink;
    @Setter
    private JavaCG2RecordSink setMethodSink;
    @Setter
    private JavaCG2RecordSink staticFinalFieldMethodCallIdSink;

    @Setter
    private WriterSupportSkip logMethodSpendTimeWriter;

    private AnnotationAttributesFormatterInterface annotationAttributesFormatter;

    @Setter
    private ClassAndJarNum classAndJarNum;

    // 非静态字段字段所有可能的类型
    private FieldPossibleTypes nonStaticFieldPossibleTypes;

    // 当前类已记录过的set方法名（get方法没有参数，只会有一个）
    private Set<String> recordedSetMethodSet;

    public ClassHandler(JavaClass javaClass, String classFileName, JavaCG2InputAndOutput javaCG2InputAndOutput, int classJarNum) {
        this.javaClass = javaClass;
        this.classFileName = classFileName;
        this.javaCG2InputAndOutput = javaCG2InputAndOutput;
        this.classJarNum = classJarNum;

        className = javaClass.getClassName();
        cpg = new ConstantPoolGen(javaClass.getConstantPool());
        handledMethodNameArgsReturnTypeSet = new HashSet<>();

        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = javaCG2InputAndOutput.getJavaCG2ConfigureWrapper();
        logMethodSpendTime = javaCG2ConfigureWrapper.getMainConfig(JavaCG2ConfigKeyEnum.CKE_LOG_METHOD_SPEND_TIME);

        javaCG2ConfInfo = javaCG2InputAndOutput.getJavaCG2ConfInfo();
        if (javaCG2ConfInfo.isParseMethodCallTypeValue()) {
            recordedSetMethodSet = new HashSet<>();
        }
        if (javaCG2ConfInfo.isFirstParseInitMethodType()) {
            nonStaticFieldPossibleTypes = new FieldPossibleTypes();
        }

        javaCG2ElManager = javaCG2InputAndOutput.getJavaCG2ElManager();
    }

    // 记录类之间引用关系
    public void recordReferencedClass(boolean duplicateClass) throws IOException {
        // 处理引用类
        ConstantPool constantPool = javaClass.getConstantPool();

        Set<String> referencedClassSet = new HashSet<>();

        for (int i = 0; i < constantPool.getLength(); i++) {
            Constant constant = null;
            try {
                constant = constantPool.getConstant(i);
            } catch (ClassFormatException e) {
                // 正常情况，不需要处理
            }
            if (constant == null || constant.getTag() != Const.CONSTANT_Class) {
                continue;
            }

            String referencedClass = constantPool.constantToString(constant);
            if (!JavaCG2ClassMethodUtil.isObjectClass(referencedClass)) {
                // 只处理非Object类的引用，去除类名中的数组形式
                String newReferencedClass = JavaCG2ClassMethodUtil.removeArrayInClassName(referencedClass);
                referencedClassSet.add(newReferencedClass);
            }
        }

        // 处理以注解方式使用的被引用类
        Set<String> annotationTypeSet = new HashSet<>();
        JavaCG2AnnotationUtil.recordAllAnnotationType(javaClass.getAnnotationEntries(), annotationTypeSet);
        for (Method method : javaClass.getMethods()) {
            JavaCG2AnnotationUtil.recordAllAnnotationType(method.getAnnotationEntries(), annotationTypeSet);
        }
        for (Field field : javaClass.getFields()) {
            JavaCG2AnnotationUtil.recordAllAnnotationType(field.getAnnotationEntries(), annotationTypeSet);
        }

        JavaCG2RecordSink usedClassReferenceSink = duplicateClass ? dupClassReferenceSink : classReferenceSink;
        List<String> referencedClassList = new ArrayList<>(referencedClassSet);
        referencedClassList.addAll(annotationTypeSet);
        Collections.sort(referencedClassList);

        boolean written = false;
        // 写入其他被引用类的类名
        for (String referencedClass : referencedClassList) {
            JavaCG2ConstantTypeEnum javaCG2ConstantTypeEnum = JavaCG2ConstantTypeEnum.getFromType(referencedClass);
            if (javaCG2ConstantTypeEnum != null && javaCG2ConstantTypeEnum.isPrimitive()) {
                // 跳过基本类型
                continue;
            }
            written = true;
            usedClassReferenceSink.writeRow(className, referencedClass, String.valueOf(classJarNum));
        }
        if (!written) {
            // 假如当前类未引用其他类，则写入当前类引用自己，避免漏掉当前类
            usedClassReferenceSink.writeRow(className, className, String.valueOf(classJarNum));
        }
    }

    // 使用Jar兼容性检查模式，仅解析基础信息时的处理
    public void parseCompatibilityMode(boolean duplicateClass) throws IOException {
        // 记录类之间引用关系
        recordReferencedClass(duplicateClass);

        for (Method method : javaClass.getMethods()) {
            // 生成格式化后的方法参数
            String methodArgTypes = JavaCG2ClassMethodUtil.genArgTypeStr(method.getArgumentTypes());
            // 生成格式化后的完整方法
            String fullMethod = JavaCG2ClassMethodUtil.formatFullMethod(className, method.getName(), methodArgTypes);

            if (javaCG2ElManager.checkIgnoreParseMethod(fullMethod)) {
                log.debug("使用Jar兼容性检查模式，需要跳过解析方法 {}", fullMethod);
                continue;
            }

            methodNumCounter.addAndGet();

            String methodReturnTypeStr = method.getReturnType().toString();
            // 记录方法名+参数类型+返回类型
            String methodNameArgsReturnType = JavaCG2ClassMethodUtil.genFullMethodWithReturnType(method.getName() + methodArgTypes, methodReturnTypeStr);
            if (duplicateClass) {
                recordMethodInfo(dupMethodInfoSink, method, fullMethod, false);
            }
            if (handledMethodNameArgsReturnTypeSet.add(methodNameArgsReturnType)) {
                // 记录方法信息
                recordMethodInfo(methodInfoSink, method, fullMethod, false);
            } else {
                // 出现方法名+参数类型+返回类型均相同的方法
                log.warn("使用Jar兼容性检查模式，出现方法名+参数类型+返回类型均相同的方法 {} {}", fullMethod, methodNameArgsReturnType);
            }
        }

        // 处理字段
        JavaCG2RecordSink usedFieldInfoSink = duplicateClass ? dupFieldInfoSink : fieldInfoSink;
        for (Field field : javaClass.getFields()) {
            // 记录字段信息
            recordFieldInfo(usedFieldInfoSink, field, false, false, false);
        }
    }

    // 处理非重复的类
    public boolean handleClass() throws IOException {
        // 记录类之间引用关系
        recordReferencedClass(false);

        // 记录类上的注解信息
        JavaCG2AnnotationUtil.writeAnnotationInfo(classAnnotationSink, javaClass.getAnnotationEntries(), annotationAttributesFormatter, className);

        // 从类上的注解中派生Spring Controller相关的信息
        springDefineControllerTaskHandler.handleClassAnnotation(className, javaClass.getAnnotationEntries());

        if (javaCG2ConfInfo.isFirstParseInitMethodType()) {
            for (Method method : javaClass.getMethods()) {
                // 解析构造函数
                if (JavaCG2ClassMethodUtil.isInitMethod(method.getName()) &&
                        !parseInitMethod(method)) {
                    return false;
                }
            }
        }

        /*
            记录当前类非静态的字段名称及类型Map
            key     字段名称
            value   字段类型
         */
        Map<String, JavaCG2Type> nonStaticFieldNameTypeMap = new HashMap<>();

        /*
            记录当前类非静态的字段名称及集合类型中的泛型类型Map
            key     字段名称
            value   集合类型中的泛型类型
         */
        Map<String, List<JavaCG2GenericsType>> nonStaticFieldNameGenericsTypeMap = new HashMap<>();

        /*
            记录当前类的static、final字段名称及初始化方法call_id的Map
            key     当前类的static、final字段名称
            value   对应的方法调用指令位置及被调用方法列表
         */
        Map<String, List<InvokeInstructionPosAndCallee>> sfFieldInvokeInstructionMap = new HashMap<>();

        /*
            记录当前类的static、final字段名称及对应的类型的Map
            key     当前类的static、final字段名称
            value   当前类的static、final字段类型
         */
        Map<String, Type> staticFinalFieldNameTypeMap = new HashMap<>();

        // 存在get方法的字段名称Set
        Set<String> fieldWithGetMethodNameSet = new HashSet<>();

        // 存在set方法的字段名称Set
        Set<String> fieldWithSetMethodNameSet = new HashSet<>();

        // 预处理字段
        for (Field field : javaClass.getFields()) {
            preHandleField(field, nonStaticFieldNameTypeMap, nonStaticFieldNameGenericsTypeMap, sfFieldInvokeInstructionMap, staticFinalFieldNameTypeMap);
        }

        // 处理方法
        for (Method method : javaClass.getMethods()) {
            if (!handleMethod(method, nonStaticFieldNameTypeMap, nonStaticFieldNameGenericsTypeMap, sfFieldInvokeInstructionMap, staticFinalFieldNameTypeMap,
                    fieldWithGetMethodNameSet, fieldWithSetMethodNameSet)) {
                return false;
            }
        }

        // 处理字段
        for (Field field : javaClass.getFields()) {
            handleField(field, nonStaticFieldNameGenericsTypeMap, fieldWithGetMethodNameSet, fieldWithSetMethodNameSet);
        }
        return true;
    }

    // 处理重复类
    public void handleDuplicateClass() throws IOException {
        // 记录类之间引用关系
        recordReferencedClass(true);

        // 处理方法
        for (Method method : javaClass.getMethods()) {
            String fullMethod = JavaCG2ClassMethodUtil.formatFullMethod(className, method.getName(), method.getArgumentTypes());
            recordMethodInfo(dupMethodInfoSink, method, fullMethod, false);
        }

        // 处理字段
        for (Field field : javaClass.getFields()) {
            // 记录字段信息
            recordFieldInfo(dupFieldInfoSink, field, false, false, false);
        }
    }

    // 解析构造函数
    private boolean parseInitMethod(Method method) {
        MethodGen mg = new MethodGen(method, className, cpg);
        String callerFullMethod = JavaCG2ClassMethodUtil.formatFullMethod(className, method.getName(), method.getArgumentTypes());
        MethodHandler4TypeAndValue methodHandler4TypeAndValue = new MethodHandler4TypeAndValue(method, mg, javaClass, callerFullMethod, javaCG2InputAndOutput);
        methodHandler4TypeAndValue.setFailCounter(failCounter);
        methodHandler4TypeAndValue.setRecordFieldPossibleTypeFlag(true);
        methodHandler4TypeAndValue.setUseFieldPossibleTypeFlag(false);
        methodHandler4TypeAndValue.setAnalyseFieldRelationshipFlag(false);
        methodHandler4TypeAndValue.setOnlyAnalyseReturnTypeFlag(false);
        methodHandler4TypeAndValue.setNonStaticFieldPossibleTypes(nonStaticFieldPossibleTypes);

        return methodHandler4TypeAndValue.handleMethod();
    }

    // 预处理字段
    private void preHandleField(Field field,
                                Map<String, JavaCG2Type> nonStaticFieldNameTypeMap,
                                Map<String, List<JavaCG2GenericsType>> nonStaticFieldNameGenericsTypeMap,
                                Map<String, List<InvokeInstructionPosAndCallee>> sfFieldInvokeInstructionMap,
                                Map<String, Type> staticFinalFieldNameTypeMap) {
        String fieldName = field.getName();
        Type fieldType = field.getType();

        if (javaCG2ConfInfo.isParseMethodCallTypeValue() && field.isStatic() && field.isFinal()) {
            // 处理static、final字段
            sfFieldInvokeInstructionMap.put(fieldName, new ArrayList<>(1));
            staticFinalFieldNameTypeMap.put(fieldName, fieldType);
        }

        if (!field.isStatic()) {
            // 处理非静态字段
            try {
                String fieldGenericSignature = field.getGenericSignature();
                if (fieldGenericSignature != null) {
                    SignatureAttribute.ObjectType javassistFieldType = SignatureAttribute.toFieldSignature(fieldGenericSignature);
                    List<JavaCG2GenericsType> fieldGenericsTypeList = new ArrayList<>();
                    // 解析字段定义中的泛型类型
                    JavaCG2GenericsTypeUtil.parseTypeDefineGenericsType(javassistFieldType, false, fieldGenericsTypeList);
                    if (!fieldGenericsTypeList.isEmpty()) {
                        nonStaticFieldNameGenericsTypeMap.put(fieldName, fieldGenericsTypeList);
                    }
                }
            } catch (BadBytecode e) {
                log.error("处理字段出现异常 {} {}", className, fieldName, e);
            }

            JavaCG2Type javaCG2Type = JavaCG2ByteCodeUtil.genJavaCG2Type(field.getType());
            nonStaticFieldNameTypeMap.put(fieldName, javaCG2Type);
        }
    }

    // 处理字段
    private void handleField(Field field, Map<String, List<JavaCG2GenericsType>> nonStaticFieldNameGenericsTypeMap,
                             Set<String> fieldWithGetMethodNameSet, Set<String> fieldWithSetMethodNameSet) throws IOException {
        String fieldName = field.getName();
        JavaCG2Type javaCG2Type = JavaCG2ByteCodeUtil.genJavaCG2Type(field.getType());
        // 判断当前字段是否存在对应的get/set方法
        boolean existsGetMethod = fieldWithGetMethodNameSet.contains(fieldName);
        boolean existsSetMethod = fieldWithSetMethodNameSet.contains(fieldName);

        boolean existsGenericsType = false;
        if (!field.isStatic()) {
            // 当前字段为非静态字段，判断是否为涉及泛型类型的集合
            List<JavaCG2GenericsType> fieldGenericsTypeList = nonStaticFieldNameGenericsTypeMap.get(fieldName);
            if (fieldGenericsTypeList != null) {
                // 当前字段为涉及泛型类型的集合
                existsGenericsType = true;
                // 记录非静态字段集合中涉及的泛型类型
                fieldGenericsTypeSink.writeRow(
                        className,
                        fieldName,
                        JavaCG2Constants.FILE_KEY_CLASS_TYPE,
                        JavaCG2ByteCodeUtil.genGenericsTypeStr4Fixed(javaCG2Type));

                int genericsSeq = 0;
                for (JavaCG2GenericsType fieldGenericsType : fieldGenericsTypeList) {
                    fieldGenericsTypeSink.writeRow(
                            className,
                            fieldName,
                            JavaCG2Constants.FILE_KEY_GENERICS_TYPE,
                            JavaCG2GenericsTypeUtil.genGenericsTypeStr(genericsSeq, fieldGenericsType));
                    genericsSeq++;
                }
            }

            // 获取属于Spring Bean的字段类型（Spring依赖注入字段解析不依赖符号执行，始终执行）
            List<String> springBeanTypeList = useSpringBeanByAnnotationHandler.getSpringBeanTypeList(className, fieldName);
            // 仅当只能找到一个对应的Bean时才记录Spring依赖注入字段信息
            if (springBeanTypeList.size() == 1) {
                springDIFieldSink.writeRow(
                        className,
                        javaCG2Type.getType(),
                        fieldName,
                        springBeanTypeList.get(0));
            }
        }

        // 记录字段信息
        recordFieldInfo(fieldInfoSink, field, existsGetMethod, existsSetMethod, existsGenericsType);

        // 记录字段上的注解信息
        JavaCG2AnnotationUtil.writeAnnotationInfo(fieldAnnotationSink, field.getAnnotationEntries(), annotationAttributesFormatter, className, fieldName);
    }

    // 记录字段信息
    private void recordFieldInfo(JavaCG2RecordSink usedFieldInfoSink, Field field, boolean existsGetMethod, boolean existsSetMethod, boolean existsGenericsType) throws IOException {
        String fieldName = field.getName();
        JavaCG2Type javaCG2Type = JavaCG2ByteCodeUtil.genJavaCG2Type(field.getType());
        String fieldModifiers = JavaCG2ByteCodeUtil.getModifiersString(field.getAccessFlags());
        String primitiveType = JavaCG2YesNoEnum.parseStrValue(JavaCG2ConstantTypeEnum.isPrimitiveType(javaCG2Type.getType()));
        String staticFlag = JavaCG2YesNoEnum.parseStrValue(field.isStatic());
        String finalFlag = JavaCG2YesNoEnum.parseStrValue(field.isFinal());

        usedFieldInfoSink.writeRow(
                className,
                fieldName,
                javaCG2Type.getType(),
                String.valueOf(javaCG2Type.getArrayDimensions()),
                JavaCG2ClassMethodUtil.getClassCategory(javaCG2Type.getType()),
                fieldModifiers,
                primitiveType,
                staticFlag,
                finalFlag,
                JavaCG2YesNoEnum.parseStrValue(existsGetMethod),
                JavaCG2YesNoEnum.parseStrValue(existsSetMethod),
                JavaCG2YesNoEnum.parseStrValue(existsGenericsType),
                String.valueOf(classJarNum));
    }

    /**
     * 处理方法
     *
     * @param method
     * @param nonStaticFieldNameTypeMap
     * @param nonStaticFieldNameGenericsTypeMap
     * @param sfFieldInvokeInstructionMap
     * @param staticFinalFieldNameTypeMap
     * @param fieldWithGetMethodNameSet
     * @param fieldWithSetMethodNameSet
     * @return false: 处理失败 true: 处理成功
     */
    private boolean handleMethod(Method method,
                                 Map<String, JavaCG2Type> nonStaticFieldNameTypeMap,
                                 Map<String, List<JavaCG2GenericsType>> nonStaticFieldNameGenericsTypeMap,
                                 Map<String, List<InvokeInstructionPosAndCallee>> sfFieldInvokeInstructionMap,
                                 Map<String, Type> staticFinalFieldNameTypeMap,
                                 Set<String> fieldWithGetMethodNameSet,
                                 Set<String> fieldWithSetMethodNameSet) throws IOException {
        // 生成格式化后的方法参数
        String methodArgTypes = JavaCG2ClassMethodUtil.genArgTypeStr(method.getArgumentTypes());
        // 生成格式化后的完整方法
        String fullMethod = JavaCG2ClassMethodUtil.formatFullMethod(className, method.getName(), methodArgTypes);

        if (javaCG2ElManager.checkIgnoreParseMethod(fullMethod)) {
            log.debug("需要跳过解析方法 {}", fullMethod);
            return true;
        }

        methodNumCounter.addAndGet();

        long startTime = 0;
        if (logMethodSpendTime) {
            startTime = System.currentTimeMillis();
            // 记录当前处理的类名、方法
            logMethodSpendTimeWriter.write(classFileName + JavaCG2Constants.FLAG_TAB + fullMethod);
        }

        log.debug("处理Method: {}", fullMethod);

        String methodReturnTypeStr = method.getReturnType().toString();
        boolean returnExistsGenericsType = false;
        Set<Integer> methodArgsGenericsTypeSeqSet = new HashSet<>();
        String methodGenericSignature = method.getGenericSignature();
        if (methodGenericSignature != null) {
            try {
                SignatureAttribute.MethodSignature methodSignature = SignatureAttribute.toMethodSignature(methodGenericSignature);
                // 记录方法返回泛型类型
                returnExistsGenericsType = recordMethodReturnGenericsType(fullMethod, methodReturnTypeStr, method.getReturnType(), methodSignature);

                // 记录方法参数中泛型类型
                recordMethodArgsGenericsType(fullMethod, methodReturnTypeStr, method.getArgumentTypes(), methodSignature, methodArgsGenericsTypeSeqSet);
            } catch (BadBytecode e) {
                log.error("处理方法签名出现异常 {}", fullMethod, e);
            }
        }

        // 记录方法名+参数类型+返回类型
        String methodNameArgsReturnType = JavaCG2ClassMethodUtil.genFullMethodWithReturnType(method.getName() + methodArgTypes, methodReturnTypeStr);
        if (handledMethodNameArgsReturnTypeSet.add(methodNameArgsReturnType)) {
            // 记录方法信息
            recordMethodInfo(methodInfoSink, method, fullMethod, returnExistsGenericsType);
        } else {
            // 出现方法名+参数类型+返回类型均相同的方法
            log.warn("出现方法名+参数类型+返回类型均相同的方法 {} {}", fullMethod, methodNameArgsReturnType);
        }

        MethodGen mg = new MethodGen(method, className, cpg);
        recordMethodArgument(fullMethod, methodReturnTypeStr, mg, methodArgsGenericsTypeSeqSet);

        // 处理方法调用，这里不判断当前方法调用是否需要忽略
        MethodHandler4Invoke methodHandler4Invoke = new MethodHandler4Invoke(method,
                mg,
                javaClass,
                javaCG2InputAndOutput,
                methodArgTypes,
                fullMethod,
                useSpringBeanByAnnotationHandler,
                callIdCounter,
                classJarNum);
        methodHandler4Invoke.setFailCounter(failCounter);
        methodHandler4Invoke.setRunnableImplClassMap(runnableImplClassMap);
        methodHandler4Invoke.setCallableImplClassMap(callableImplClassMap);
        methodHandler4Invoke.setTransactionCallbackImplClassMap(transactionCallbackImplClassMap);
        methodHandler4Invoke.setTransactionCallbackWithoutResultChildClassMap(transactionCallbackWithoutResultChildClassMap);
        methodHandler4Invoke.setThreadChildClassMap(threadChildClassMap);
        methodHandler4Invoke.setExtensionsManager(extensionsManager);
        methodHandler4Invoke.setMethodCallSink(methodCallSink);
        methodHandler4Invoke.setMethodCallRawCalleeSink(methodCallRawCalleeSink);
        methodHandler4Invoke.setLambdaMethodInfoSink(lambdaMethodInfoSink);
        methodHandler4Invoke.setMethodAnnotationSink(methodAnnotationSink);
        methodHandler4Invoke.setSpringDefineControllerTaskHandler(springDefineControllerTaskHandler);
        methodHandler4Invoke.setMethodArgAnnotationSink(methodArgAnnotationSink);
        methodHandler4Invoke.setMethodLineNumberSink(methodLineNumberSink);
        methodHandler4Invoke.setClassAndJarNum(classAndJarNum);
        methodHandler4Invoke.setFieldUsageOtherSink(fieldUsageOtherSink);

        if (javaCG2ConfInfo.isParseMethodCallTypeValue()) {
            methodHandler4Invoke.setParseMethodCallTypeValueFlag(true);
            methodHandler4Invoke.setEnumInitArgFieldSink(enumInitArgFieldSink);
            methodHandler4Invoke.setEnumInitAssignInfoSink(enumInitAssignInfoSink);
            methodHandler4Invoke.setMethodCallInfoSink(methodCallInfoSink);
            methodHandler4Invoke.setMethodCallMethodCallReturnSink(methodCallMethodCallReturnSink);
            methodHandler4Invoke.setMethodCallStaticFieldSink(methodCallStaticFieldSink);
            methodHandler4Invoke.setMethodCallNonStaticFieldSink(methodCallNonStaticFieldSink);
            methodHandler4Invoke.setMethodCallFieldActualTypeSink(methodCallFieldActualTypeSink);
            methodHandler4Invoke.setMethodCallStaticFieldMCRSink(methodCallStaticFieldMCRSink);
            methodHandler4Invoke.setMethodReturnArgSeqSink(methodReturnArgSeqSink);
            methodHandler4Invoke.setMethodReturnCallIdSink(methodReturnCallIdSink);
            methodHandler4Invoke.setMethodCatchSink(methodCatchSink);
            methodHandler4Invoke.setMethodFinallySink(methodFinallySink);
            methodHandler4Invoke.setMethodThrowSink(methodThrowSink);
            methodHandler4Invoke.setStaticFinalFieldMethodCallIdSink(staticFinalFieldMethodCallIdSink);
            methodHandler4Invoke.setGetMethodSink(getMethodSink);
            methodHandler4Invoke.setSetMethodSink(setMethodSink);
            methodHandler4Invoke.setMethodReturnConstValueSink(methodReturnConstValueSink);
            methodHandler4Invoke.setMethodReturnFieldInfoSink(methodReturnFieldInfoSink);
            methodHandler4Invoke.setRecordedSetMethodSet(recordedSetMethodSet);
            methodHandler4Invoke.setNonStaticFieldNameTypeMap(nonStaticFieldNameTypeMap);
            methodHandler4Invoke.setNonStaticFieldNameGenericsTypeMap(nonStaticFieldNameGenericsTypeMap);
            methodHandler4Invoke.setFieldWithGetMethodNameSet(fieldWithGetMethodNameSet);
            methodHandler4Invoke.setFieldWithSetMethodNameSet(fieldWithSetMethodNameSet);
            if (JavaCG2CommonNameConstants.METHOD_NAME_CLINIT.equals(method.getName())) {
                // 当前方法为静态代码块
                methodHandler4Invoke.setInClinitMethod(true);
                methodHandler4Invoke.setSfFieldInvokeInstructionMap(sfFieldInvokeInstructionMap);
                methodHandler4Invoke.setStaticFinalFieldNameTypeMap(staticFinalFieldNameTypeMap);
            }
            if (javaCG2ConfInfo.isFirstParseInitMethodType()) {
                methodHandler4Invoke.setNonStaticFieldPossibleTypes(nonStaticFieldPossibleTypes);
            }
            if (javaCG2ConfInfo.isAnalyseFieldRelationship()) {
                methodHandler4Invoke.setFieldRelationshipSink(fieldRelationshipSink);
                methodHandler4Invoke.setFieldRelationshipCounter(fieldRelationshipCounter);
            }
        }
        boolean success = methodHandler4Invoke.handleMethod();
        if (logMethodSpendTime) {
            long spendTime = System.currentTimeMillis() - startTime;
            // 记录方法处理耗时
            logMethodSpendTimeWriter.write(JavaCG2Constants.FLAG_TAB + spendTime + JavaCG2Constants.NEW_LINE);
        }
        return success;
    }

    // 记录方法信息
    private void recordMethodInfo(JavaCG2RecordSink usedMethodInfoSink, Method method, String fullMethod, boolean returnExistsGenericsType) throws IOException {
        // 获取方法指令的HASH
        Code code = method.getCode();
        String methodInstructionsHash = "";
        if (code != null) {
            String methodCode = MethodCodeInstructionHandler.getMethodCodeStr(method);
            methodInstructionsHash = DigestUtils.md5Hex(methodCode);
        }

        JavaCG2Type javaCG2Type = JavaCG2ByteCodeUtil.genJavaCG2Type(method.getReturnType());
        // 记录方法的信息
        usedMethodInfoSink.writeRow(
                fullMethod,
                String.valueOf(method.getAccessFlags()),
                javaCG2Type.getType(),
                String.valueOf(javaCG2Type.getArrayDimensions()),
                JavaCG2ClassMethodUtil.getClassCategory(javaCG2Type.getType()),
                JavaCG2YesNoEnum.parseStrValue(returnExistsGenericsType),
                methodInstructionsHash,
                String.valueOf(classJarNum));
    }

    // 记录方法返回泛型类型
    private boolean recordMethodReturnGenericsType(String fullMethod, String methodReturnTypeStr, Type methodReturnType, SignatureAttribute.MethodSignature methodSignature) {
        try {
            SignatureAttribute.Type returnType = methodSignature.getReturnType();
            if (!(returnType instanceof SignatureAttribute.ClassType)) {
                return false;
            }

            SignatureAttribute.ClassType classType = (SignatureAttribute.ClassType) returnType;
            List<JavaCG2GenericsType> methodReturnGenericsTypeList = new ArrayList<>();
            // 解析方法返回中泛型类型，外层处理
            JavaCG2GenericsTypeUtil.parseTypeDefineGenericsType(classType, false, methodReturnGenericsTypeList);
            if (methodReturnGenericsTypeList.isEmpty()) {
                // 未获取到方法返回中泛型类型
                return false;
            }

            // 记录返回类型，序号固定为0
            JavaCG2Type javaCG2Type = JavaCG2ByteCodeUtil.genJavaCG2Type(methodReturnType);
            methodReturnGenericsTypeSink.writeRow(
                    fullMethod,
                    methodReturnTypeStr,
                    JavaCG2Constants.FILE_KEY_CLASS_TYPE,
                    JavaCG2ByteCodeUtil.genGenericsTypeStr4Fixed(javaCG2Type));

            // 获取到方法返回中泛型类型，记录
            for (int genericsSeq = 0; genericsSeq < methodReturnGenericsTypeList.size(); genericsSeq++) {
                JavaCG2GenericsType methodReturnGenericsType = methodReturnGenericsTypeList.get(genericsSeq);
                methodReturnGenericsTypeSink.writeRow(
                        fullMethod,
                        methodReturnTypeStr,
                        JavaCG2Constants.FILE_KEY_GENERICS_TYPE,
                        JavaCG2GenericsTypeUtil.genGenericsTypeStr(genericsSeq, methodReturnGenericsType));
            }
            return true;
        } catch (Exception e) {
            log.error("error ", e);
            return false;
        }
    }

    // 记录方法参数中泛型类型
    private void recordMethodArgsGenericsType(String fullMethod, String methodReturnType, Type[] argTypes, SignatureAttribute.MethodSignature methodSignature,
                                              Set<Integer> methodArgsGenericsTypeSeqSet) throws IOException {
        SignatureAttribute.Type[] parameterTypes = methodSignature.getParameterTypes();
        if (ArrayUtils.isEmpty(parameterTypes)) {
            return;
        }
        for (int argSeq = 0; argSeq < parameterTypes.length; argSeq++) {
            SignatureAttribute.Type argType = parameterTypes[argSeq];
            if (!(argType instanceof SignatureAttribute.ClassType)) {
                continue;
            }

            // 当前方法参数存在泛型类型
            methodArgsGenericsTypeSeqSet.add(argSeq);
            SignatureAttribute.ClassType classType = (SignatureAttribute.ClassType) argType;
            List<JavaCG2GenericsType> methodArgsGenericsTypeList = new ArrayList<>();
            // 解析方法参数中泛型类型，外层处理
            JavaCG2GenericsTypeUtil.parseTypeDefineGenericsType(classType, false, methodArgsGenericsTypeList);
            if (methodArgsGenericsTypeList.isEmpty()) {
                // 未获取到方法参数中泛型类型
                continue;
            }

            // 记录参数类型，序号固定为0
            JavaCG2Type javaCG2Type = JavaCG2ByteCodeUtil.genJavaCG2Type(argTypes[argSeq]);
            methodArgGenericsTypeSink.writeRow(
                    fullMethod,
                    methodReturnType,
                    String.valueOf(argSeq),
                    JavaCG2Constants.FILE_KEY_CLASS_TYPE,
                    JavaCG2ByteCodeUtil.genGenericsTypeStr4Fixed(javaCG2Type));

            // 获取到方法参数中泛型类型，记录
            for (int genericsSeq = 0; genericsSeq < methodArgsGenericsTypeList.size(); genericsSeq++) {
                JavaCG2GenericsType methodArgsGenericsType = methodArgsGenericsTypeList.get(genericsSeq);
                methodArgGenericsTypeSink.writeRow(
                        fullMethod,
                        methodReturnType,
                        String.valueOf(argSeq),
                        JavaCG2Constants.FILE_KEY_GENERICS_TYPE,
                        JavaCG2GenericsTypeUtil.genGenericsTypeStr(genericsSeq, methodArgsGenericsType));
            }
        }
    }

    // 处理方法参数
    private void recordMethodArgument(String fullMethod, String methodReturnType, MethodGen mg, Set<Integer> methodArgsGenericsTypeSeqSet) throws IOException {
        Type[] argTypes = mg.getArgumentTypes();

        for (Attribute attribute : mg.getAttributes()) {
            if (!(attribute instanceof MethodParameters)) {
                continue;
            }
            ConstantPool constantPool = mg.getConstantPool().getConstantPool();
            MethodParameters methodParameters = (MethodParameters) attribute;
            MethodParameter[] parameters = methodParameters.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                String argName = parameters[i].getParameterName(constantPool);
                doRecordMethodArgument(fullMethod, methodReturnType, i, argTypes[i], argName, methodArgsGenericsTypeSeqSet);
            }
            return;
        }

        LocalVariableTable localVariableTable = mg.getLocalVariableTable(mg.getConstantPool());
        for (int i = 0; i < argTypes.length; i++) {
            int argIndex = JavaCG2ByteCodeUtil.getLocalVariableTableIndex(mg, i);
            LocalVariable localVariable = localVariableTable.getLocalVariable(argIndex, 0);
            String argName = (localVariable == null ? "" : localVariable.getName());
            doRecordMethodArgument(fullMethod, methodReturnType, i, argTypes[i], argName, methodArgsGenericsTypeSeqSet);
        }
    }

    private void doRecordMethodArgument(String fullMethod, String methodReturnType, int seq, Type argType, String argName, Set<Integer> methodArgsGenericsTypeSeqSet) throws IOException {
        JavaCG2Type javaCG2Type = JavaCG2ByteCodeUtil.genJavaCG2Type(argType);
        String existsGenericsType = JavaCG2YesNoEnum.parseStrValue(methodArgsGenericsTypeSeqSet.contains(seq));
        methodArgumentSink.writeRow(
                fullMethod,
                methodReturnType,
                String.valueOf(seq),
                argName,
                javaCG2Type.getType(),
                String.valueOf(javaCG2Type.getArrayDimensions()),
                JavaCG2ClassMethodUtil.getClassCategory(javaCG2Type.getType()),
                existsGenericsType);
    }

    public void recordExtensionsManager(ExtensionsManager extensionsManager) {
        this.extensionsManager = extensionsManager;
        annotationAttributesFormatter = extensionsManager.getAnnotationAttributesFormatter();
    }
}
