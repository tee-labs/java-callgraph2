package com.adrninistrator.javacg2.parser;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.enums.JavaCG2DirEnum;
import com.adrninistrator.javacg2.common.enums.JavaCG2YesNoEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfInfo;
import com.adrninistrator.javacg2.dto.classes.InnerClassInfo;
import com.adrninistrator.javacg2.dto.counter.JavaCG2Counter;
import com.adrninistrator.javacg2.dto.inputoutput.JavaCG2InputAndOutput;
import com.adrninistrator.javacg2.dto.jar.ClassAndJarNum;
import com.adrninistrator.javacg2.dto.type.JavaCG2GenericsType;
import com.adrninistrator.javacg2.extensions.annotationattributes.AnnotationAttributesFormatterInterface;
import com.adrninistrator.javacg2.extensions.manager.ExtensionsManager;
import com.adrninistrator.javacg2.handler.ClassHandler;
import com.adrninistrator.javacg2.spring.UseSpringBeanByAnnotationHandler;
import com.adrninistrator.javacg2.util.JavaCG2AnnotationUtil;
import com.adrninistrator.javacg2.util.JavaCG2ByteCodeUtil;
import com.adrninistrator.javacg2.util.JavaCG2ClassMethodUtil;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import com.adrninistrator.javacg2.util.JavaCG2GenericsTypeUtil;
import com.adrninistrator.javacg2.util.JavaCG2JarUtil;
import com.adrninistrator.javacg2.writer.WriterSupportSkip;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSink;
import copy.javassist.bytecode.BadBytecode;
import copy.javassist.bytecode.SignatureAttribute;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Signature;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author adrninistrator
 * @date 2022/9/14
 * @description: 解析jar包中的文件，正式处理
 */
public class JarEntryHandleParser extends AbstractJarEntryParser {

    private static final Logger logger = LoggerFactory.getLogger(JarEntryHandleParser.class);

    /*
        记录已处理过的类名及次数
        key 类名
        value   处理次数
     */
    private final Map<String, JavaCG2Counter> handledClassNameTimesMap = new HashMap<>();

    /*
        已记录的包名
            key 包名
            value   包含包名的jar序号
     */
    private final Map<String, Set<Integer>> recordedPackageNameMap = new HashMap<>();

    private final boolean parseJarCompatibilityMode;
    private final boolean parseOnlyClassMode;

    private UseSpringBeanByAnnotationHandler useSpringBeanByAnnotationHandler;

    private Map<String, Boolean> runnableImplClassMap;
    private Map<String, Boolean> callableImplClassMap;
    private Map<String, Boolean> transactionCallbackImplClassMap;
    private Map<String, Boolean> transactionCallbackWithoutResultChildClassMap;
    private Map<String, Boolean> threadChildClassMap;

    private JavaCG2RecordSink classAnnotationSink;
    private JavaCG2RecordSink classExtImplGenericsTypeSink;
    private JavaCG2RecordSink classInfoSink;
    private JavaCG2RecordSink classReferenceSink;
    private JavaCG2RecordSink dupClassReferenceSink;
    private JavaCG2RecordSink classSignatureGenericsTypeSink;
    private JavaCG2RecordSink dupClassInfoSink;
    private JavaCG2RecordSink dupMethodInfoSink;
    private JavaCG2RecordSink dupFieldInfoSink;
    private JavaCG2RecordSink enumInitArgFieldSink;
    private JavaCG2RecordSink enumInitAssignInfoSink;
    private JavaCG2RecordSink extendsImplSink;
    private JavaCG2RecordSink fieldAnnotationSink;
    private JavaCG2RecordSink fieldGenericsTypeSink;
    private JavaCG2RecordSink fieldInfoSink;
    private JavaCG2RecordSink fieldRelationshipSink;
    private JavaCG2RecordSink fieldUsageOtherSink;
    private JavaCG2RecordSink springDIFieldSink;
    private JavaCG2RecordSink getMethodSink;
    private JavaCG2RecordSink innerClassSink;
    private JavaCG2RecordSink lambdaMethodInfoSink;
    private JavaCG2RecordSink methodAnnotationSink;
    private JavaCG2RecordSink methodArgAnnotationSink;
    private JavaCG2RecordSink methodArgGenericsTypeSink;
    private JavaCG2RecordSink methodArgumentSink;
    private JavaCG2RecordSink methodCallInfoSink;
    private JavaCG2RecordSink methodCallMethodCallReturnSink;
    private JavaCG2RecordSink methodCallStaticFieldSink;
    private JavaCG2RecordSink methodCallNonStaticFieldSink;
    private JavaCG2RecordSink methodCallFieldActualTypeSink;
    private JavaCG2RecordSink methodCallStaticFieldMCRSink;
    private JavaCG2RecordSink methodCallSink;
    private JavaCG2RecordSink methodCallRawCalleeSink;
    private JavaCG2RecordSink methodCatchSink;
    private JavaCG2RecordSink methodFinallySink;
    private JavaCG2RecordSink methodInfoSink;
    private JavaCG2RecordSink methodLineNumberSink;
    private JavaCG2RecordSink methodReturnArgSeqSink;
    private JavaCG2RecordSink methodReturnCallIdSink;
    private JavaCG2RecordSink methodReturnConstValueSink;
    private JavaCG2RecordSink methodReturnFieldInfoSink;
    private JavaCG2RecordSink methodReturnGenericsTypeSink;
    private JavaCG2RecordSink methodThrowSink;
    private JavaCG2RecordSink packageInfoSink;
    private JavaCG2RecordSink setMethodSink;
    private JavaCG2RecordSink staticFinalFieldMethodCallIdSink;

    private WriterSupportSkip logMethodSpendTimeWriter;

    // 扩展类管理类
    private ExtensionsManager extensionsManager;

    private JavaCG2Counter callIdCounter;
    private JavaCG2Counter classNumCounter;
    private JavaCG2Counter methodNumCounter;
    private JavaCG2Counter failCounter;
    private JavaCG2Counter fieldRelationshipCounter;

    private ClassAndJarNum classAndJarNum;

    private AnnotationAttributesFormatterInterface annotationAttributesFormatter;

    public JarEntryHandleParser(JavaCG2InputAndOutput javaCG2InputAndOutput, boolean onlyOneJar) {
        super(javaCG2InputAndOutput, onlyOneJar);
        JavaCG2ConfInfo javaCG2ConfInfo = javaCG2InputAndOutput.getJavaCG2ConfInfo();
        parseJarCompatibilityMode = javaCG2ConfInfo.isParseJarCompatibilityMode();
        parseOnlyClassMode = javaCG2ConfInfo.isParseOnlyClassMode();
    }

    @Override
    protected boolean handleEntry(ZipInputStream zipInputStream, String jarEntryPath) throws IOException {
        if (!JavaCG2FileUtil.checkClassFile(jarEntryPath)) {
            // 非class文件则跳过
            return true;
        }

        JavaClass javaClass = new ClassParser(zipInputStream, jarEntryPath).parse();
//        if (!"xxx".equals(javaClass.getClassName())) {
//            return true;
//        }

        // 处理jar包中的class文件
        return handleClassEntry(javaClass, jarEntryPath);
    }

    @Override
    protected boolean handleClassEntry(JavaClass javaClass, String jarEntryPath) throws IOException {
        if (JavaCG2Constants.MODULE_INFO_CLASS.equals(jarEntryPath) || jarEntryPath.endsWith(JavaCG2Constants.MODULE_INFO_CLASS_SUFFIX)) {
            // 跳过 module-info.class
            return true;
        }

        // 处理Java类
        String className = javaClass.getClassName();
        if (javaCG2ElManager.checkIgnoreParseClass(className)) {
            logger.debug("跳过解析类 {}", className);
            return true;
        }

        JavaCG2Counter classHandledTimes = handledClassNameTimesMap.computeIfAbsent(className, k -> new JavaCG2Counter());
        // 判断是否为重复的类
        boolean duplicateClass = false;
        if (classHandledTimes.getCount() > 0) {
            logger.debug("处理重复同名Class: {}", className);
            duplicateClass = true;
        }
        classHandledTimes.addAndGet();

        logger.debug("处理Class: {}", className);

        int classJarNum;
        if (onlyOneJar) {
            classJarNum = JavaCG2Constants.JAR_NUM_MIN;
        } else {
            classJarNum = JavaCG2JarUtil.getJarNumFromDirName(lastFirstDirName);
        }

        // 记录包名
        recordPackageName(javaClass.getPackageName());

        ClassHandler classHandler = genClassHandler(javaClass, jarEntryPath, classJarNum);
        classNumCounter.addAndGet();
        // 开始处理当前类
        // 处理内部类信息
        handleInnerClass(javaClass);

        // 处理特殊模式
        if (handleSpecialMode(javaClass, duplicateClass, classJarNum, jarEntryPath, classHandler)) {
            return true;
        }

        int failCountBefore = failCounter.getCount();
        boolean success;
        if (duplicateClass) {
            success = true;
            // 处理重复类
            classHandler.handleDuplicateClass();
        } else {
            // 处理非重复的类
            success = classHandler.handleClass();
        }
        if (failCounter.getCount() > failCountBefore) {
            // 将处理失败的类保存到目录中
            saveHandleFailClass(javaClass);
        }

        // 记录类的信息
        recordClassInfo(javaClass, duplicateClass, className, classJarNum, jarEntryPath);

        if (duplicateClass) {
            // 重复类不执行后续处理
            return true;
        }

        // 记录继承及实现相关信息
        recordExtendsAndImplInfo(javaClass, className);

        // 处理类的签名
        handleClassSignature(javaClass, className);
        return success;
    }

    // 记录包名
    private void recordPackageName(String packageName) throws IOException {
        // 执行记录包名
        if (!doRecordPackageName(packageName)) {
            return;
        }
        // 对每个层级的包名进行记录
        int lastIndex = 0;
        while (true) {
            int index = packageName.indexOf(JavaCG2Constants.FLAG_DOT, lastIndex);
            if (index == -1) {
                break;
            }
            String upperPackageName = packageName.substring(0, index);
            lastIndex = index + 1;
            // 记录上层包名
            doRecordPackageName(upperPackageName);
        }
    }

    // 执行记录包名
    private boolean doRecordPackageName(String packageName) throws IOException {
        Set<Integer> containsPackageJarNumSet = recordedPackageNameMap.computeIfAbsent(packageName, k -> new HashSet<>());
        if (containsPackageJarNumSet.add(lastJarNum)) {
            // 未记录时记录包名
            int packageLevel = StringUtils.countMatches(packageName, JavaCG2Constants.FLAG_DOT) + 1;
            packageInfoSink.writeRow(packageName, String.valueOf(packageLevel), String.valueOf(lastJarNum), lastJarFileName);
            return true;
        }
        return false;
    }

    // 处理特殊模式
    private boolean handleSpecialMode(JavaClass javaClass, boolean duplicateClass, int classJarNum, String jarEntryPath, ClassHandler classHandler) throws IOException {
        String className = javaClass.getClassName();
        if (parseOnlyClassMode) {
            // 记录类的信息
            recordClassInfo(javaClass, duplicateClass, className, classJarNum, jarEntryPath);
            // 记录类引用的类
            classHandler.recordReferencedClass(duplicateClass);
            if (!duplicateClass) {
                // 非重复类时，记录类上的注解信息
                recordClassAnnotation(javaClass);
            }
            return true;
        }

        if (parseJarCompatibilityMode) {
            // 使用Jar兼容性检查模式
            // 记录类的信息
            recordClassInfo(javaClass, duplicateClass, className, classJarNum, jarEntryPath);

            // 使用Jar兼容性检查模式，仅解析基础信息时，处理重复类中的方法
            classHandler.parseCompatibilityMode(duplicateClass);

            if (!duplicateClass) {
                // 非重复类时，记录继承及实现相关信息
                recordExtendsAndImplInfo(javaClass, className);

                // 记录类上的注解信息
                recordClassAnnotation(javaClass);
            }
            return true;
        }
        return false;
    }

    // 记录类上的注解信息
    private void recordClassAnnotation(JavaClass javaClass) {
        JavaCG2AnnotationUtil.writeAnnotationInfo(classAnnotationSink, javaClass.getAnnotationEntries(), annotationAttributesFormatter, javaClass.getClassName());
    }

    private ClassHandler genClassHandler(JavaClass javaClass, String jarEntryPath, int classJarNum) {
        ClassHandler classHandler = new ClassHandler(javaClass, jarEntryPath, javaCG2InputAndOutput, classJarNum);
        classHandler.setUseSpringBeanByAnnotationHandler(useSpringBeanByAnnotationHandler);
        classHandler.setRunnableImplClassMap(runnableImplClassMap);
        classHandler.setCallableImplClassMap(callableImplClassMap);
        classHandler.setTransactionCallbackImplClassMap(transactionCallbackImplClassMap);
        classHandler.setTransactionCallbackWithoutResultChildClassMap(transactionCallbackWithoutResultChildClassMap);
        classHandler.setThreadChildClassMap(threadChildClassMap);
        classHandler.setCallIdCounter(callIdCounter);
        classHandler.setClassAnnotationSink(classAnnotationSink);
        classHandler.setClassReferenceSink(classReferenceSink);
        classHandler.setDupClassReferenceSink(dupClassReferenceSink);
        classHandler.setDupMethodInfoSink(dupMethodInfoSink);
        classHandler.setDupFieldInfoSink(dupFieldInfoSink);
        classHandler.setEnumInitArgFieldSink(enumInitArgFieldSink);
        classHandler.setEnumInitAssignInfoSink(enumInitAssignInfoSink);
        classHandler.setFieldAnnotationSink(fieldAnnotationSink);
        classHandler.setFieldGenericsTypeSink(fieldGenericsTypeSink);
        classHandler.setFieldInfoSink(fieldInfoSink);
        classHandler.setFieldRelationshipSink(fieldRelationshipSink);
        classHandler.setFieldUsageOtherSink(fieldUsageOtherSink);
        classHandler.setSpringDIFieldSink(springDIFieldSink);
        classHandler.setGetMethodSink(getMethodSink);
        classHandler.setLambdaMethodInfoSink(lambdaMethodInfoSink);
        classHandler.setLogMethodSpendTimeWriter(logMethodSpendTimeWriter);
        classHandler.setMethodAnnotationSink(methodAnnotationSink);
        classHandler.setMethodArgAnnotationSink(methodArgAnnotationSink);
        classHandler.setMethodArgGenericsTypeSink(methodArgGenericsTypeSink);
        classHandler.setMethodArgumentSink(methodArgumentSink);
        classHandler.setMethodCallInfoSink(methodCallInfoSink);
        classHandler.setMethodCallMethodCallReturnSink(methodCallMethodCallReturnSink);
        classHandler.setMethodCallStaticFieldSink(methodCallStaticFieldSink);
        classHandler.setMethodCallNonStaticFieldSink(methodCallNonStaticFieldSink);
        classHandler.setMethodCallFieldActualTypeSink(methodCallFieldActualTypeSink);
        classHandler.setMethodCallStaticFieldMCRSink(methodCallStaticFieldMCRSink);
        classHandler.setMethodCallSink(methodCallSink);
        classHandler.setMethodCallRawCalleeSink(methodCallRawCalleeSink);
        classHandler.setMethodCatchSink(methodCatchSink);
        classHandler.setMethodFinallySink(methodFinallySink);
        classHandler.setMethodInfoSink(methodInfoSink);
        classHandler.setMethodLineNumberSink(methodLineNumberSink);
        classHandler.setMethodReturnArgSeqSink(methodReturnArgSeqSink);
        classHandler.setMethodReturnCallIdSink(methodReturnCallIdSink);
        classHandler.setMethodReturnConstValueSink(methodReturnConstValueSink);
        classHandler.setMethodReturnFieldInfoSink(methodReturnFieldInfoSink);
        classHandler.setMethodReturnGenericsTypeSink(methodReturnGenericsTypeSink);
        classHandler.setMethodThrowSink(methodThrowSink);
        classHandler.setSetMethodSink(setMethodSink);
        classHandler.setStaticFinalFieldMethodCallIdSink(staticFinalFieldMethodCallIdSink);
        classHandler.recordExtensionsManager(extensionsManager);
        classHandler.setMethodNumCounter(methodNumCounter);
        classHandler.setFailCounter(failCounter);
        classHandler.setFieldRelationshipCounter(fieldRelationshipCounter);
        classHandler.setClassAndJarNum(classAndJarNum);
        return classHandler;
    }

    // 记录类的信息
    private void recordClassInfo(JavaClass javaClass, boolean duplicateClass, String className, int classJarNum, String jarEntryPath) throws IOException {
        String classMd5 = DigestUtils.md5Hex(javaClass.getBytes());
        JavaCG2RecordSink usedClassInfoSink = duplicateClass ? dupClassInfoSink : classInfoSink;
        usedClassInfoSink.writeRow(className, String.valueOf(javaClass.getAccessFlags()), classMd5, String.valueOf(classJarNum), jarEntryPath);
    }

    // 将处理失败的类保存到目录中
    private void saveHandleFailClass(JavaClass javaClass) {
        String saveClassFilePath = javaCG2InputAndOutput.getJavaCG2ConfInfo().getOutputDirPath() + JavaCG2DirEnum.IDE_FAIL_CLASSES + File.separator +
                javaClass.getClassName() + JavaCG2Constants.EXT_CLASS;
        File saveClassFile = new File(saveClassFilePath);
        logger.info("将处理失败的class文件保存到文件 {}", saveClassFile.getAbsolutePath());
        try {
            // 以下文件有创建文件所在目录
            javaClass.dump(saveClassFile);
        } catch (Exception e) {
            logger.error("error ", e);
        }
    }

    // 记录继承及实现相关信息
    private void recordExtendsAndImplInfo(JavaClass javaClass, String className) throws IOException {
        String superClassName = javaClass.getSuperclassName();
        String accessFlagsStr = String.valueOf(javaClass.getAccessFlags());
        if (!JavaCG2ClassMethodUtil.isObjectClass(superClassName)) {
            // 仅处理父类非Object类的情况
            extendsImplSink.writeRow(
                    className,
                    accessFlagsStr,
                    JavaCG2Constants.FILE_KEY_EXTENDS,
                    superClassName);
        }

        for (String interfaceName : javaClass.getInterfaceNames()) {
            // 接口不会是Object类，不需要判断
            extendsImplSink.writeRow(
                    className,
                    accessFlagsStr,
                    JavaCG2Constants.FILE_KEY_IMPLEMENTS,
                    interfaceName);
        }
    }

    // 处理类的签名
    private void handleClassSignature(JavaClass javaClass, String className) throws IOException {
        if (javaClass.isAnnotation()) {
            // 若当前类为注解则不处理
            return;
        }

        Signature signature = JavaCG2ByteCodeUtil.getSignatureOfClass(javaClass);
        if (signature == null) {
            return;
        }

        try {
            SignatureAttribute.ClassSignature signatureAttribute = SignatureAttribute.toClassSignature(signature.getSignature());

            // 类涉及继承与实现的签名信息Map
            SignatureAttribute.TypeParameter[] params = signatureAttribute.getParameters();
            if (ArrayUtils.isNotEmpty(params)) {
                // 处理类签名中的泛型类型
                int seq = -1;
                for (SignatureAttribute.TypeParameter param : params) {
                    seq++;
                    String typeVariablesName = param.getName();
                    SignatureAttribute.ObjectType paramClassBound = param.getClassBound();
                    if (paramClassBound != null) {
                        // 泛型类型继承了特定类的处理
                        String extendsClassName = paramClassBound.jvmTypeName();
                        classSignatureGenericsTypeSink.writeRow(className, String.valueOf(seq), typeVariablesName, extendsClassName);
                        continue;
                    }
                    SignatureAttribute.ObjectType[] paramInterfaceBounds = param.getInterfaceBound();
                    if (ArrayUtils.isNotEmpty(paramInterfaceBounds)) {
                        // 泛型类型继承了特定接口的处理
                        String extendsClassName = paramInterfaceBounds[0].jvmTypeName();
                        classSignatureGenericsTypeSink.writeRow(className, String.valueOf(seq), typeVariablesName, extendsClassName);
                    }
                }
            }

            // 处理父类相关的签名
            SignatureAttribute.ClassType superClassType = signatureAttribute.getSuperClass();
            if (superClassType != null) {
                String superClassName = JavaCG2GenericsTypeUtil.getClassName(superClassType);
                // 记录类继承中的泛型信息
                recordExtImplGenericsInfo(JavaCG2Constants.FILE_KEY_EXTENDS, className, superClassName, superClassType);
            }

            // 处理接口相关的签名
            SignatureAttribute.ClassType[] interfaceClassTypes = signatureAttribute.getInterfaces();
            if (interfaceClassTypes != null) {
                for (SignatureAttribute.ClassType interfaceClassType : interfaceClassTypes) {
                    String interfaceClassName = JavaCG2GenericsTypeUtil.getClassName(interfaceClassType);
                    // 记录类实现中的泛型信息
                    recordExtImplGenericsInfo(JavaCG2Constants.FILE_KEY_IMPLEMENTS, className, interfaceClassName, interfaceClassType);
                }
            }
        } catch (BadBytecode e) {
            logger.error("处理类的签名出现异常 {} ", className, e);
        }
    }

    // 处理内部类信息
    private void handleInnerClass(JavaClass javaClass) throws IOException {
        // 获取类中的内部类信息
        List<InnerClassInfo> innerClassInfoList = JavaCG2ByteCodeUtil.getInnerClassInfo(javaClass);
        for (InnerClassInfo innerClassInfo : innerClassInfoList) {
            innerClassSink.writeRow(innerClassInfo.getInnerClassName(), innerClassInfo.getOuterClassName(),
                    JavaCG2YesNoEnum.parseStrValue(innerClassInfo.isAnonymousClass()));
        }
    }

    /**
     * 记录类继承或实现中的泛型信息
     *
     * @param extendsOrImplStr
     * @param className
     * @param superOrInterfaceName
     * @param classType
     */
    private void recordExtImplGenericsInfo(String extendsOrImplStr, String className, String superOrInterfaceName, SignatureAttribute.ClassType classType) throws IOException {
        SignatureAttribute.TypeArgument[] arguments = classType.getTypeArguments();
        if (arguments == null) {
            return;
        }

        int seq = 0;
        for (SignatureAttribute.TypeArgument typeArgument : arguments) {
            SignatureAttribute.ObjectType objectType = typeArgument.getType();

            List<JavaCG2GenericsType> classGenericsTypeList = new ArrayList<>();
            // 解析类签名中的泛型类型
            JavaCG2GenericsTypeUtil.parseTypeDefineGenericsType(objectType, true, classGenericsTypeList);

            String seqStr = String.valueOf(seq);
            for (int genericsSeq = 0; genericsSeq < classGenericsTypeList.size(); genericsSeq++) {
                JavaCG2GenericsType javaCG2GenericsType = classGenericsTypeList.get(genericsSeq);
                // 记录当前类/接口继承或实现时与父类或接口相同的泛型信息
                classExtImplGenericsTypeSink.writeRow(
                        className,
                        extendsOrImplStr,
                        seqStr,
                        superOrInterfaceName,
                        JavaCG2GenericsTypeUtil.genGenericsTypeStr4ClassExtImpl(genericsSeq, javaCG2GenericsType));
            }
            seq++;
        }
    }

    public void recordExtensionsManager(ExtensionsManager extensionsManager) {
        this.extensionsManager = extensionsManager;
        annotationAttributesFormatter = extensionsManager.getAnnotationAttributesFormatter();
    }

    //
    public void setUseSpringBeanByAnnotationHandler(UseSpringBeanByAnnotationHandler useSpringBeanByAnnotationHandler) {
        this.useSpringBeanByAnnotationHandler = useSpringBeanByAnnotationHandler;
    }

    public void setRunnableImplClassMap(Map<String, Boolean> runnableImplClassMap) {
        this.runnableImplClassMap = runnableImplClassMap;
    }

    public void setCallableImplClassMap(Map<String, Boolean> callableImplClassMap) {
        this.callableImplClassMap = callableImplClassMap;
    }

    public void setTransactionCallbackImplClassMap(Map<String, Boolean> transactionCallbackImplClassMap) {
        this.transactionCallbackImplClassMap = transactionCallbackImplClassMap;
    }

    public void setTransactionCallbackWithoutResultChildClassMap(Map<String, Boolean> transactionCallbackWithoutResultChildClassMap) {
        this.transactionCallbackWithoutResultChildClassMap = transactionCallbackWithoutResultChildClassMap;
    }

    public void setThreadChildClassMap(Map<String, Boolean> threadChildClassMap) {
        this.threadChildClassMap = threadChildClassMap;
    }

    public void setClassAnnotationSink(JavaCG2RecordSink classAnnotationSink) {
        this.classAnnotationSink = classAnnotationSink;
    }

    public void setClassExtImplGenericsTypeSink(JavaCG2RecordSink classExtImplGenericsTypeSink) {
        this.classExtImplGenericsTypeSink = classExtImplGenericsTypeSink;
    }

    public void setClassInfoSink(JavaCG2RecordSink classInfoSink) {
        this.classInfoSink = classInfoSink;
    }

    public void setClassReferenceSink(JavaCG2RecordSink classReferenceSink) {
        this.classReferenceSink = classReferenceSink;
    }

    public void setDupClassReferenceSink(JavaCG2RecordSink dupClassReferenceSink) {
        this.dupClassReferenceSink = dupClassReferenceSink;
    }

    public void setClassSignatureGenericsTypeSink(JavaCG2RecordSink classSignatureGenericsTypeSink) {
        this.classSignatureGenericsTypeSink = classSignatureGenericsTypeSink;
    }

    public void setDupClassInfoSink(JavaCG2RecordSink dupClassInfoSink) {
        this.dupClassInfoSink = dupClassInfoSink;
    }

    public void setDupMethodInfoSink(JavaCG2RecordSink dupMethodInfoSink) {
        this.dupMethodInfoSink = dupMethodInfoSink;
    }

    public void setDupFieldInfoSink(JavaCG2RecordSink dupFieldInfoSink) {
        this.dupFieldInfoSink = dupFieldInfoSink;
    }

    public void setEnumInitArgFieldSink(JavaCG2RecordSink enumInitArgFieldSink) {
        this.enumInitArgFieldSink = enumInitArgFieldSink;
    }

    public void setEnumInitAssignInfoSink(JavaCG2RecordSink enumInitAssignInfoSink) {
        this.enumInitAssignInfoSink = enumInitAssignInfoSink;
    }

    public void setExtendsImplSink(JavaCG2RecordSink extendsImplSink) {
        this.extendsImplSink = extendsImplSink;
    }

    public void setFieldAnnotationSink(JavaCG2RecordSink fieldAnnotationSink) {
        this.fieldAnnotationSink = fieldAnnotationSink;
    }

    public void setFieldGenericsTypeSink(JavaCG2RecordSink fieldGenericsTypeSink) {
        this.fieldGenericsTypeSink = fieldGenericsTypeSink;
    }

    public void setFieldInfoSink(JavaCG2RecordSink fieldInfoSink) {
        this.fieldInfoSink = fieldInfoSink;
    }

    public void setFieldRelationshipSink(JavaCG2RecordSink fieldRelationshipSink) {
        this.fieldRelationshipSink = fieldRelationshipSink;
    }

    public void setFieldUsageOtherSink(JavaCG2RecordSink fieldUsageOtherSink) {
        this.fieldUsageOtherSink = fieldUsageOtherSink;
    }

    public void setSpringDIFieldSink(JavaCG2RecordSink springDIFieldSink) {
        this.springDIFieldSink = springDIFieldSink;
    }

    public void setGetMethodSink(JavaCG2RecordSink getMethodSink) {
        this.getMethodSink = getMethodSink;
    }

    public void setInnerClassSink(JavaCG2RecordSink innerClassSink) {
        this.innerClassSink = innerClassSink;
    }

    public void setLambdaMethodInfoSink(JavaCG2RecordSink lambdaMethodInfoSink) {
        this.lambdaMethodInfoSink = lambdaMethodInfoSink;
    }

    public void setMethodAnnotationSink(JavaCG2RecordSink methodAnnotationSink) {
        this.methodAnnotationSink = methodAnnotationSink;
    }

    public void setMethodArgAnnotationSink(JavaCG2RecordSink methodArgAnnotationSink) {
        this.methodArgAnnotationSink = methodArgAnnotationSink;
    }

    public void setMethodArgGenericsTypeSink(JavaCG2RecordSink methodArgGenericsTypeSink) {
        this.methodArgGenericsTypeSink = methodArgGenericsTypeSink;
    }

    public void setMethodArgumentSink(JavaCG2RecordSink methodArgumentSink) {
        this.methodArgumentSink = methodArgumentSink;
    }

    public void setMethodCallInfoSink(JavaCG2RecordSink methodCallInfoSink) {
        this.methodCallInfoSink = methodCallInfoSink;
    }

    public void setMethodCallMethodCallReturnSink(JavaCG2RecordSink methodCallMethodCallReturnSink) {
        this.methodCallMethodCallReturnSink = methodCallMethodCallReturnSink;
    }

    public void setMethodCallStaticFieldSink(JavaCG2RecordSink methodCallStaticFieldSink) {
        this.methodCallStaticFieldSink = methodCallStaticFieldSink;
    }

    public void setMethodCallNonStaticFieldSink(JavaCG2RecordSink methodCallNonStaticFieldSink) {
        this.methodCallNonStaticFieldSink = methodCallNonStaticFieldSink;
    }

    public void setMethodCallFieldActualTypeSink(JavaCG2RecordSink methodCallFieldActualTypeSink) {
        this.methodCallFieldActualTypeSink = methodCallFieldActualTypeSink;
    }

    public void setMethodCallStaticFieldMCRSink(JavaCG2RecordSink methodCallStaticFieldMCRSink) {
        this.methodCallStaticFieldMCRSink = methodCallStaticFieldMCRSink;
    }

    public void setMethodCallSink(JavaCG2RecordSink methodCallSink) {
        this.methodCallSink = methodCallSink;
    }

    public void setMethodCallRawCalleeSink(JavaCG2RecordSink methodCallRawCalleeSink) {
        this.methodCallRawCalleeSink = methodCallRawCalleeSink;
    }

    public void setMethodCatchSink(JavaCG2RecordSink methodCatchSink) {
        this.methodCatchSink = methodCatchSink;
    }

    public void setMethodFinallySink(JavaCG2RecordSink methodFinallySink) {
        this.methodFinallySink = methodFinallySink;
    }

    public void setMethodInfoSink(JavaCG2RecordSink methodInfoSink) {
        this.methodInfoSink = methodInfoSink;
    }

    public void setMethodLineNumberSink(JavaCG2RecordSink methodLineNumberSink) {
        this.methodLineNumberSink = methodLineNumberSink;
    }

    public void setMethodReturnArgSeqSink(JavaCG2RecordSink methodReturnArgSeqSink) {
        this.methodReturnArgSeqSink = methodReturnArgSeqSink;
    }

    public void setMethodReturnCallIdSink(JavaCG2RecordSink methodReturnCallIdSink) {
        this.methodReturnCallIdSink = methodReturnCallIdSink;
    }

    public void setMethodReturnConstValueSink(JavaCG2RecordSink methodReturnConstValueSink) {
        this.methodReturnConstValueSink = methodReturnConstValueSink;
    }

    public void setMethodReturnFieldInfoSink(JavaCG2RecordSink methodReturnFieldInfoSink) {
        this.methodReturnFieldInfoSink = methodReturnFieldInfoSink;
    }

    public void setMethodReturnGenericsTypeSink(JavaCG2RecordSink methodReturnGenericsTypeSink) {
        this.methodReturnGenericsTypeSink = methodReturnGenericsTypeSink;
    }

    public void setMethodThrowSink(JavaCG2RecordSink methodThrowSink) {
        this.methodThrowSink = methodThrowSink;
    }

    public void setPackageInfoSink(JavaCG2RecordSink packageInfoSink) {
        this.packageInfoSink = packageInfoSink;
    }

    public void setSetMethodSink(JavaCG2RecordSink setMethodSink) {
        this.setMethodSink = setMethodSink;
    }

    public void setStaticFinalFieldMethodCallIdSink(JavaCG2RecordSink staticFinalFieldMethodCallIdSink) {
        this.staticFinalFieldMethodCallIdSink = staticFinalFieldMethodCallIdSink;
    }

    public void setLogMethodSpendTimeWriter(WriterSupportSkip logMethodSpendTimeWriter) {
        this.logMethodSpendTimeWriter = logMethodSpendTimeWriter;
    }

    public void setCallIdCounter(JavaCG2Counter callIdCounter) {
        this.callIdCounter = callIdCounter;
    }

    public void setClassNumCounter(JavaCG2Counter classNumCounter) {
        this.classNumCounter = classNumCounter;
    }

    public void setMethodNumCounter(JavaCG2Counter methodNumCounter) {
        this.methodNumCounter = methodNumCounter;
    }

    public void setFailCounter(JavaCG2Counter failCounter) {
        this.failCounter = failCounter;
    }

    public void setFieldRelationshipCounter(JavaCG2Counter fieldRelationshipCounter) {
        this.fieldRelationshipCounter = fieldRelationshipCounter;
    }

    public void setClassAndJarNum(ClassAndJarNum classAndJarNum) {
        this.classAndJarNum = classAndJarNum;
    }
}
