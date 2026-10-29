package com.adrninistrator.javacg2.entry;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.enums.JavaCG2CallTypeEnum;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.common.enums.JavaCG2YesNoEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfInfo;
import com.adrninistrator.javacg2.conf.JavaCG2ConfManager;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseSetEnum;
import com.adrninistrator.javacg2.conf.enums.interfaces.MainConfigInterface;
import com.adrninistrator.javacg2.dto.classes.ClassExtendsInfo;
import com.adrninistrator.javacg2.dto.counter.JavaCG2Counter;
import com.adrninistrator.javacg2.dto.inputoutput.JavaCG2InputAndOutput;
import com.adrninistrator.javacg2.dto.jar.ClassAndJarNum;
import com.adrninistrator.javacg2.dto.jar.MergeJarResult;
import com.adrninistrator.javacg2.dto.jar.OuterInnerJarPath;
import com.adrninistrator.javacg2.dto.method.MethodArgReturnTypes;
import com.adrninistrator.javacg2.dto.output.JavaCG2OtherRunResult;
import com.adrninistrator.javacg2.dto.output.JavaCG2OutputInfo;
import com.adrninistrator.javacg2.dto.spring.SpringAopAdviceInfo;
import com.adrninistrator.javacg2.dto.spring.SpringAopPointcutInfo;
import com.adrninistrator.javacg2.dto.spring.SpringBeanInJava;
import com.adrninistrator.javacg2.dto.spring.SpringBeanInXml;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.el.manager.JavaCG2ElManager;
import com.adrninistrator.javacg2.exceptions.JavaCG2RuntimeException;
import com.adrninistrator.javacg2.extensions.codeparser.SpringXmlBeanParserInterface;
import com.adrninistrator.javacg2.extensions.manager.ExtensionsManager;
import com.adrninistrator.javacg2.handler.ExtendsImplHandler;
import com.adrninistrator.javacg2.handler.MergeJarHandler;
import com.adrninistrator.javacg2.parser.JarEntryHandleParser;
import com.adrninistrator.javacg2.parser.JarEntryPreHandle1Parser;
import com.adrninistrator.javacg2.parser.JarEntryPreHandle2Parser;
import com.adrninistrator.javacg2.spring.SpringDefineAopHandler;
import com.adrninistrator.javacg2.spring.SpringDefineBeanHandler;
import com.adrninistrator.javacg2.spring.SpringDefineComponentScanHandler;
import com.adrninistrator.javacg2.spring.UseSpringBeanByAnnotationHandler;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import com.adrninistrator.javacg2.util.JavaCG2JarUtil;
import com.adrninistrator.javacg2.util.JavaCG2Util;
import com.adrninistrator.javacg2.writer.JavaCG2FileOutputAdapter;
import com.adrninistrator.javacg2.writer.JavaCG2OutputAdapter;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSink;
import com.adrninistrator.javacg2.writer.WriterSupportSkip;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author adrninistrator
 * @date 2021/8/21
 * @description: 入口类
 */
public class JavaCG2Entry {
    private static final Logger logger = LoggerFactory.getLogger(JavaCG2Entry.class);

    public static final String SIMPLE_CLASS_NAME = JavaCG2Entry.class.getSimpleName();

    private final JavaCG2Counter callIdCounter = new JavaCG2Counter(JavaCG2Constants.METHOD_CALL_ID_MIN_BEFORE);
    private final JavaCG2Counter classNumCounter = new JavaCG2Counter();
    private final JavaCG2Counter methodNumCounter = new JavaCG2Counter();
    private final JavaCG2Counter failCounter = new JavaCG2Counter();
    private final JavaCG2Counter fieldRelationshipCounter = new JavaCG2Counter(JavaCG2Constants.RECORD_ID_MIN_BEFORE);

    private final ExtensionsManager extensionsManager = new ExtensionsManager();

    private final AtomicBoolean runFlag = new AtomicBoolean(false);

    private final JavaCG2InputAndOutput javaCG2InputAndOutput = new JavaCG2InputAndOutput();

    private final JavaCG2ConfigureWrapper javaCG2ConfigureWrapper;

    /*
        输出适配器，用于生成每种结果文件对应的记录Sink
        未通过setOutputAdapter方法指定时，使用JavaCG2FileOutputAdapter将记录写入文件
     */
    private JavaCG2OutputAdapter outputAdapter;

    /**
     * 设置输出适配器
     * 需要在调用run()方法之前调用当前方法
     *
     * @param outputAdapter
     */
    public void setOutputAdapter(JavaCG2OutputAdapter outputAdapter) {
        this.outputAdapter = outputAdapter;
    }

    private JarEntryPreHandle1Parser jarEntryPreHandle1Parser;
    private JarEntryPreHandle2Parser jarEntryPreHandle2Parser;
    private JarEntryHandleParser jarEntryHandleParser;

    private ExtendsImplHandler extendsImplHandler;

    /*
        保存需要处理的jar文件/目录的路径及对应的序号
        key     jar文件/目录路径
        value   jar文件/目录对应的序号
     */
    private Map<String, Integer> jarPathNumMap;

    private SpringDefineBeanHandler springDefineBeanHandler;

    private UseSpringBeanByAnnotationHandler useSpringBeanByAnnotationHandler;

    private SpringDefineComponentScanHandler springDefineComponentScanHandler;

    private SpringDefineAopHandler springDefineAopHandler;

    public JavaCG2Entry(JavaCG2ConfigureWrapper javaCG2ConfigureWrapper) {
        this.javaCG2ConfigureWrapper = javaCG2ConfigureWrapper;
    }

    public boolean run() {
        if (!runFlag.compareAndSet(false, true)) {
            logger.error("当前类的实例在创建后只能使用一次，假如需要再次执行，请创建新的实例 {}", this.getClass().getSimpleName());
            return false;
        }

        if (javaCG2ConfigureWrapper == null) {
            throw new JavaCG2RuntimeException("配置参数包装对象不允许为null");
        }

        long startTime = System.currentTimeMillis();

        JavaCG2ConfInfo javaCG2ConfInfo = JavaCG2ConfManager.getConfInfo(javaCG2ConfigureWrapper);

        // 处理输出文件路径
        String outputDirPath = handleOutputDir();
        if (outputDirPath == null) {
            return false;
        }

        javaCG2ConfInfo.setOutputDirPath(outputDirPath);
        try (JavaCG2ElManager javaCG2ElManager = new JavaCG2ElManager(javaCG2ConfigureWrapper, JavaCG2ElConfigEnum.values(), outputDirPath)) {
            JavaCG2Counter jarNumCounter = new JavaCG2Counter(JavaCG2Constants.JAR_NUM_MIN_BEFORE);

            // 处理参数中指定的jar文件
            MergeJarResult mergeJarResult = handleJarInConf(jarNumCounter, javaCG2ElManager);
            if (mergeJarResult == null) {
                return false;
            }

            try {
                boolean success1 = parse(mergeJarResult, javaCG2ElManager, javaCG2ConfInfo);
                logger.info("执行完毕，处理数量，类： {} 方法: {} 方法调用: {} 耗时: {} 秒", classNumCounter.getCount(), methodNumCounter.getCount(), callIdCounter.getCount(),
                        JavaCG2Util.getSpendSeconds(startTime));
                // 打印所有的配置参数
                boolean success2 = javaCG2ConfigureWrapper.printAllConfigInfo(SIMPLE_CLASS_NAME, outputDirPath, JavaCG2Constants.FILE_JAVACG2_ALL_CONFIG_MD);
                // 打印当前使用的配置参数
                boolean success3 = javaCG2ConfigureWrapper.printUsedConfigInfo(SIMPLE_CLASS_NAME, outputDirPath, JavaCG2Constants.FILE_JAVACG2_USED_CONFIG_MD);
                return success1 && success2 && success3;
            } catch (Exception e) {
                logger.error("error ", e);
                return false;
            }
        }
    }

    private boolean parse(MergeJarResult mergeJarResult, JavaCG2ElManager javaCG2ElManager, JavaCG2ConfInfo javaCG2ConfInfo) {
        // 初始化
        if (!init(javaCG2ConfInfo, javaCG2ElManager)) {
            return false;
        }

        JavaCG2OutputInfo javaCG2OutputInfo = javaCG2InputAndOutput.getJavaCG2OutputInfo();
        // 打印生成的文件信息
        printOutputFileInfo(javaCG2OutputInfo);

        try (JavaCG2RecordSink classAnnotationSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION);
             JavaCG2RecordSink javaCG2ConfigSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_JAVACG2_CONFIG);
             JavaCG2RecordSink classInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO);
             JavaCG2RecordSink dupClassInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_DUP_CLASS_INFO);
             JavaCG2RecordSink classReferenceSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_REFERENCE);
             JavaCG2RecordSink dupClassReferenceSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_DUP_CLASS_REFERENCE);
             JavaCG2RecordSink classSignatureGenericsTypeSink = outputAdapter.genMainRecordSink(
                     JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_SIGNATURE_GENERICS_TYPE);
             JavaCG2RecordSink classExtImplGenericsTypeSink = outputAdapter.genMainRecordSink(
                     JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_EXT_IMPL_GENERICS_TYPE);
             JavaCG2RecordSink extendsImplSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_EXTENDS_IMPL);
             JavaCG2RecordSink fieldAnnotationSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_ANNOTATION);
             JavaCG2RecordSink fieldInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_INFO);
             JavaCG2RecordSink fieldGenericsTypeSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_GENERICS_TYPE);
             JavaCG2RecordSink fieldRelationshipSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_RELATIONSHIP);
             JavaCG2RecordSink fieldUsageOtherSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_USAGE_OTHER);
             JavaCG2RecordSink getMethodSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_GET_METHOD);
             JavaCG2RecordSink innerClassSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_INNER_CLASS);
             JavaCG2RecordSink jarInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO);
             JavaCG2RecordSink lambdaMethodInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_LAMBDA_METHOD_INFO);
             JavaCG2RecordSink methodAnnotationSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_ANNOTATION);
             JavaCG2RecordSink methodArgumentSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_ARGUMENT);
             JavaCG2RecordSink methodArgAnnotationSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_ARG_ANNOTATION);
             JavaCG2RecordSink methodArgGenericsTypeSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_ARG_GENERICS_TYPE);
             JavaCG2RecordSink methodCallInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_INFO);
             JavaCG2RecordSink methodCallMethodCallReturnSink = outputAdapter.genMainRecordSink(
                     JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_METHOD_CALL_RETURN);
             JavaCG2RecordSink methodCallStaticFieldSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_STATIC_FIELD);
             JavaCG2RecordSink methodCallNonStaticFieldSink = outputAdapter.genMainRecordSink(
                     JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_NON_STATIC_FIELD);
             JavaCG2RecordSink methodCallFieldActualTypeSink = outputAdapter.genMainRecordSink(
                     JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_FIELD_ACTUAL_TYPE);
             JavaCG2RecordSink methodCallStaticFieldMCRSink = outputAdapter.genMainRecordSink(
                     JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_STATIC_FIELD_MCR);
             JavaCG2RecordSink methodReturnArgSeqSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_RETURN_ARG_SEQ);
             JavaCG2RecordSink methodReturnCallIdSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_RETURN_CALL_ID);
             JavaCG2RecordSink methodReturnConstValueSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_RETURN_CONST_VALUE);
             JavaCG2RecordSink methodReturnFieldInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_RETURN_FIELD_INFO);
             JavaCG2RecordSink methodCallSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL);
             JavaCG2RecordSink methodCallRawCalleeSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_RAW_CALLEE);
             JavaCG2RecordSink methodInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_INFO);
             JavaCG2RecordSink dupMethodInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_DUP_METHOD_INFO);
             JavaCG2RecordSink dupFieldInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_DUP_FIELD_INFO);
             JavaCG2RecordSink enumInitArgFieldSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_ENUM_INIT_ARG_FIELD);
             JavaCG2RecordSink enumInitAssignInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_ENUM_INIT_ASSIGN_INFO);
             JavaCG2RecordSink methodLineNumberSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_LINE_NUMBER);
             JavaCG2RecordSink methodReturnGenericsTypeSink = outputAdapter.genMainRecordSink(
                     JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_RETURN_GENERICS_TYPE);
             JavaCG2RecordSink methodCatchSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CATCH);
             JavaCG2RecordSink methodFinallySink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_FINALLY);
             JavaCG2RecordSink methodThrowSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_THROW);
             JavaCG2RecordSink packageInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_PACKAGE_INFO);
             JavaCG2RecordSink setMethodSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_SET_METHOD);
             JavaCG2RecordSink springBeanSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_BEAN);
             JavaCG2RecordSink springScanPackageSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_SCAN_PACKAGE_JAVA);
             JavaCG2RecordSink springAopAspectSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ASPECT_JAVA);
             JavaCG2RecordSink springAopPointcutSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_POINTCUT_JAVA);
             JavaCG2RecordSink springAopAdviceSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ADVICE_JAVA);
             JavaCG2RecordSink springDIFieldSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_DI_FIELD);
             JavaCG2RecordSink staticFinalFieldMethodCallIdSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_SF_FIELD_METHOD_CALL);
             WriterSupportSkip logMethodSpendTimeWriter = new WriterSupportSkip(javaCG2OutputInfo.getMainFilePath(JavaCG2OutPutFileTypeEnum.OPFTE_LOG_METHOD_SPEND_TIME), false)
        ) {
            jarEntryHandleParser.setClassAnnotationSink(classAnnotationSink);
            jarEntryHandleParser.setClassInfoSink(classInfoSink);
            jarEntryHandleParser.setDupClassInfoSink(dupClassInfoSink);
            jarEntryHandleParser.setClassReferenceSink(classReferenceSink);
            jarEntryHandleParser.setDupClassReferenceSink(dupClassReferenceSink);
            jarEntryHandleParser.setClassSignatureGenericsTypeSink(classSignatureGenericsTypeSink);
            jarEntryHandleParser.setClassExtImplGenericsTypeSink(classExtImplGenericsTypeSink);
            jarEntryHandleParser.setExtendsImplSink(extendsImplSink);
            jarEntryHandleParser.setFieldAnnotationSink(fieldAnnotationSink);
            jarEntryHandleParser.setFieldInfoSink(fieldInfoSink);
            jarEntryHandleParser.setFieldGenericsTypeSink(fieldGenericsTypeSink);
            jarEntryHandleParser.setFieldRelationshipSink(fieldRelationshipSink);
            jarEntryHandleParser.setFieldUsageOtherSink(fieldUsageOtherSink);
            jarEntryHandleParser.setSpringDIFieldSink(springDIFieldSink);
            jarEntryHandleParser.setGetMethodSink(getMethodSink);
            jarEntryHandleParser.setInnerClassSink(innerClassSink);
            jarEntryHandleParser.setLambdaMethodInfoSink(lambdaMethodInfoSink);
            jarEntryHandleParser.setMethodAnnotationSink(methodAnnotationSink);
            jarEntryHandleParser.setMethodArgumentSink(methodArgumentSink);
            jarEntryHandleParser.setMethodArgAnnotationSink(methodArgAnnotationSink);
            jarEntryHandleParser.setMethodArgGenericsTypeSink(methodArgGenericsTypeSink);
            jarEntryHandleParser.setMethodCallInfoSink(methodCallInfoSink);
            jarEntryHandleParser.setMethodCallMethodCallReturnSink(methodCallMethodCallReturnSink);
            jarEntryHandleParser.setMethodCallStaticFieldSink(methodCallStaticFieldSink);
            jarEntryHandleParser.setMethodCallNonStaticFieldSink(methodCallNonStaticFieldSink);
            jarEntryHandleParser.setMethodCallFieldActualTypeSink(methodCallFieldActualTypeSink);
            jarEntryHandleParser.setMethodCallStaticFieldMCRSink(methodCallStaticFieldMCRSink);
            jarEntryHandleParser.setMethodReturnArgSeqSink(methodReturnArgSeqSink);
            jarEntryHandleParser.setMethodReturnCallIdSink(methodReturnCallIdSink);
            jarEntryHandleParser.setMethodReturnConstValueSink(methodReturnConstValueSink);
            jarEntryHandleParser.setMethodReturnFieldInfoSink(methodReturnFieldInfoSink);
            jarEntryHandleParser.setMethodCallSink(methodCallSink);
            jarEntryHandleParser.setMethodCallRawCalleeSink(methodCallRawCalleeSink);
            jarEntryHandleParser.setMethodInfoSink(methodInfoSink);
            jarEntryHandleParser.setDupMethodInfoSink(dupMethodInfoSink);
            jarEntryHandleParser.setDupFieldInfoSink(dupFieldInfoSink);
            jarEntryHandleParser.setEnumInitArgFieldSink(enumInitArgFieldSink);
            jarEntryHandleParser.setEnumInitAssignInfoSink(enumInitAssignInfoSink);
            jarEntryHandleParser.setMethodLineNumberSink(methodLineNumberSink);
            jarEntryHandleParser.setMethodReturnGenericsTypeSink(methodReturnGenericsTypeSink);
            jarEntryHandleParser.setMethodCatchSink(methodCatchSink);
            jarEntryHandleParser.setMethodFinallySink(methodFinallySink);
            jarEntryHandleParser.setMethodThrowSink(methodThrowSink);
            jarEntryHandleParser.setPackageInfoSink(packageInfoSink);
            jarEntryHandleParser.setSetMethodSink(setMethodSink);
            jarEntryHandleParser.setStaticFinalFieldMethodCallIdSink(staticFinalFieldMethodCallIdSink);

            jarEntryHandleParser.setLogMethodSpendTimeWriter(logMethodSpendTimeWriter);

            String mergeJarFilePath = JavaCG2FileUtil.getCanonicalPath(mergeJarResult.getMergeJarFile());
            // 处理jar文件
            if (!handleJar(mergeJarFilePath, methodCallSink, springBeanSink, springScanPackageSink, springAopAspectSink, springAopPointcutSink, springAopAdviceSink,
                    javaCG2ElManager)
                    || failCounter.getCount() > 0) {
                logger.error("处理失败，出错次数 {}", failCounter.getCount());
                return false;
            }

            // 记录处理的jar文件信息
            int maxJarNum = 0;
            Map<Integer, String> jarNumPathMap = new HashMap<>(jarPathNumMap.size());
            for (Map.Entry<String, Integer> entry : jarPathNumMap.entrySet()) {
                int jarNum = entry.getValue();
                maxJarNum = Math.max(maxJarNum, jarNum);
                jarNumPathMap.put(jarNum, entry.getKey());
            }
            List<Integer> jarNumList = new ArrayList<>(jarNumPathMap.keySet());
            Collections.sort(jarNumList);
            for (Integer jarNum : jarNumList) {
                String jarDirPath = jarNumPathMap.get(jarNum);
                if (jarDirPath == null) {
                    // 对应序号的jar文件被排除掉，跳过
                    continue;
                }
                String jarDirType;
                OuterInnerJarPath outerInnerJarPath = JavaCG2JarUtil.parseOuterInnerJarPath(jarDirPath);
                String outerJarDirPath;
                String innerJarPath = outerInnerJarPath.getInnerJarPath();
                if (innerJarPath != null) {
                    outerJarDirPath = outerInnerJarPath.getOuterJarPath();
                    jarDirType = JavaCG2Constants.FILE_KEY_JAR_IN_JAR;
                } else {
                    outerJarDirPath = jarDirPath;
                    boolean isJar = new File(jarDirPath).isFile();
                    jarDirType = isJar ? JavaCG2Constants.FILE_KEY_JAR : JavaCG2Constants.FILE_KEY_DIR;
                }
                jarInfoSink.writeRow(jarDirType, String.valueOf(jarNum), outerJarDirPath, innerJarPath);
            }

            String resultDirPath = JavaCG2FileUtil.getCanonicalPath(javaCG2OutputInfo.getOutputDirPath());
            jarInfoSink.writeRow(JavaCG2Constants.FILE_KEY_RESULT_DIR, String.valueOf(++maxJarNum), resultDirPath, "");
            jarInfoSink.writeRow(JavaCG2Constants.FILE_KEY_PARSE_JAR_PATH, String.valueOf(++maxJarNum), mergeJarFilePath, "");
            if (mergeJarResult.getFatJarFile() != null) {
                String fatJarFilePath = JavaCG2FileUtil.getCanonicalPath(mergeJarResult.getFatJarFile());
                jarInfoSink.writeRow(JavaCG2Constants.FILE_KEY_FAT_JAR_PATH, String.valueOf(++maxJarNum),
                        fatJarFilePath, "");
            }

            // 记录java-callgraph2组件使用的配置参数
            return recordJavaCG2Config(javaCG2ConfigSink);
        } catch (Exception e) {
            logger.error("出现异常 ", e);
            return false;
        } finally {
            // 关闭扩展类管理类
            extensionsManager.close();
        }
    }

    // 处理输出文件路径
    private String handleOutputDir() {
        MergeJarHandler mergeJarHandler = new MergeJarHandler(javaCG2ConfigureWrapper, null, null, null, true);
        MergeJarResult mergeJarResult = mergeJarHandler.mergeJar();
        if (mergeJarResult == null) {
            logger.error("获取处理需要解析的jar文件/目录的输出文件路径失败");
            return null;
        }

        File mergeJarFile = mergeJarResult.getMergeJarFile();
        String newJarFilePath = JavaCG2FileUtil.getCanonicalPath(mergeJarFile);
        String outputRootPath = javaCG2ConfigureWrapper.getMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH);
        String outputDirPath;
        if (StringUtils.isBlank(outputRootPath)) {
            // 配置参数中未指定生成文件的根目录，生成在jar文件所在目录
            outputDirPath = newJarFilePath + JavaCG2Constants.DIR_TAIL_OUTPUT + File.separator;
        } else {
            // 配置参数中有指定生成文件的根目录，生成在指定目录
            outputDirPath = JavaCG2FileUtil.addSeparator4FilePath(outputRootPath) + mergeJarFile.getName() + JavaCG2Constants.DIR_TAIL_OUTPUT + File.separator;
        }
        logger.info("当前输出的根目录: {}", outputDirPath);
        if (!JavaCG2FileUtil.isDirectoryExists(outputDirPath, true)) {
            logger.error("创建输出目录失败 {}", outputRootPath);
            return null;
        }
        return outputDirPath;
    }

    // 处理配置参数中指定的jar文件
    private MergeJarResult handleJarInConf(JavaCG2Counter jarNumCounter, JavaCG2ElManager javaCG2ElManager) {
        jarPathNumMap = new HashMap<>();

        // 对指定的jar文件进行处理
        MergeJarHandler mergeJarHandler = new MergeJarHandler(javaCG2ConfigureWrapper, jarPathNumMap, jarNumCounter, javaCG2ElManager, false);
        MergeJarResult mergeJarResult = mergeJarHandler.mergeJar();
        if (mergeJarResult == null) {
            logger.error("处理需要解析的jar文件/目录失败");
            return null;
        }

        String mergeJarFilePath = JavaCG2FileUtil.getCanonicalPath(mergeJarResult.getMergeJarFile());
        logger.info("实际处理的jar文件路径: {}", mergeJarFilePath);
        if (mergeJarResult.getFatJarFile() != null) {
            String fatJarFilePath = JavaCG2FileUtil.getCanonicalPath(mergeJarResult.getFatJarFile());
            logger.info("生成的fat jar文件路径: {}", fatJarFilePath);
        }
        return mergeJarResult;
    }

    // 初始化
    private boolean init(JavaCG2ConfInfo javaCG2ConfInfo, JavaCG2ElManager javaCG2ElManager) {
        // 检查方法调用枚举类型是否重复定义
        JavaCG2CallTypeEnum.checkRepeat();

        String outputFileExt = javaCG2ConfigureWrapper.getMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_FILE_EXT);
        // 处理结果信息相关
        JavaCG2OutputInfo javaCG2OutputInfo = new JavaCG2OutputInfo(javaCG2ConfInfo.getOutputDirPath(), outputFileExt);

        JavaCG2OtherRunResult javaCG2OtherRunResult = new JavaCG2OtherRunResult();
        javaCG2InputAndOutput.setJavaCG2ConfInfo(javaCG2ConfInfo);
        javaCG2InputAndOutput.setJavaCG2ConfigureWrapper(javaCG2ConfigureWrapper);
        javaCG2InputAndOutput.setJavaCG2ElManager(javaCG2ElManager);
        javaCG2InputAndOutput.setJavaCG2OutputInfo(javaCG2OutputInfo);
        javaCG2InputAndOutput.setJavaCG2OtherRunResult(javaCG2OtherRunResult);

        // 初始化输出适配器，未指定时使用将记录写入文件的实现
        if (outputAdapter == null) {
            outputAdapter = new JavaCG2FileOutputAdapter(javaCG2OutputInfo);
        }
        extensionsManager.setOutputAdapter(outputAdapter);

        // 扩展类管理类初始化
        extensionsManager.setJavaCG2InputAndOutput(javaCG2InputAndOutput);
        if (!extensionsManager.init()) {
            return false;
        }

        // 第一次预处理相关
        // Runnable实现类Map
        Map<String, Boolean> runnableImplClassMap = new HashMap<>(JavaCG2Constants.SIZE_100);
        // Callable实现类Map
        Map<String, Boolean> callableImplClassMap = new HashMap<>(JavaCG2Constants.SIZE_100);
        // TransactionCallback实现类Map
        Map<String, Boolean> transactionCallbackImplClassMap = new HashMap<>(JavaCG2Constants.SIZE_10);
        // TransactionCallbackWithoutResult子类Map
        Map<String, Boolean> transactionCallbackWithoutResultChildClassMap = new HashMap<>(JavaCG2Constants.SIZE_10);
        // Thread子类Map
        Map<String, Boolean> threadChildClassMap = new HashMap<>(JavaCG2Constants.SIZE_100);
        // 类名及对应的父类（父类非Object时记录）
        Map<String, String> classAndSuperMap = new HashMap<>(JavaCG2Constants.SIZE_100);
        // 涉及继承的接口名Set
        Set<String> interfaceExtendsSet = new HashSet<>(JavaCG2Constants.SIZE_100);
        // 所有在jar文件中出现的类名Set
        Set<String> allClassNameSet = new HashSet<>(JavaCG2Constants.SIZE_1000);
  
        /*
           类名及所在的jar文件序号Map
           key    类名
           value  类所在的jar文件序号
        */
        ClassAndJarNum classAndJarNum = new ClassAndJarNum();

        /*
            类实现的接口，及类中的方法信息
            key     类名
            value   类实现的接口
         */
        Map<String, List<String>> classImplementsInfoMap = new HashMap<>(JavaCG2Constants.SIZE_200);

        /*
            涉及继承（父类非Object）、实现的类中，可能涉及继承（父类与子类）、实现（实现类）的方法
            key     类名
            value   相关方法
                key     方法名、参数、返回值
                value   方法access_flags
         */
        Map<String, Map<MethodArgReturnTypes, Integer>> classExtendsImplMethodWithArgTypesMap = new HashMap<>(JavaCG2Constants.SIZE_100);

        /*
            接口中的方法信息
            key     接口名
            value   接口中的方法信息
        */
        Map<String, Map<MethodArgReturnTypes, Integer>> interfaceMethodWithArgTypesMap = new HashMap<>(JavaCG2Constants.SIZE_200);

        // Spring依赖注入字段解析不依赖符号执行，此处无条件创建相关handler，使spring_di_field等文件在任何开关状态下都能生成
        springDefineBeanHandler = new SpringDefineBeanHandler(javaCG2InputAndOutput, failCounter);
        springDefineComponentScanHandler = new SpringDefineComponentScanHandler();
        springDefineAopHandler = new SpringDefineAopHandler();
        boolean onlyOneJar = jarPathNumMap.size() == 1;
        jarEntryPreHandle1Parser = new JarEntryPreHandle1Parser(javaCG2InputAndOutput, onlyOneJar);
        jarEntryPreHandle1Parser.setClassImplementsInfoMap(classImplementsInfoMap);
        jarEntryPreHandle1Parser.setClassExtendsImplMethodWithArgTypesMap(classExtendsImplMethodWithArgTypesMap);
        jarEntryPreHandle1Parser.setInterfaceMethodWithArgTypesMap(interfaceMethodWithArgTypesMap);
        jarEntryPreHandle1Parser.setRunnableImplClassMap(runnableImplClassMap);
        jarEntryPreHandle1Parser.setCallableImplClassMap(callableImplClassMap);
        jarEntryPreHandle1Parser.setTransactionCallbackImplClassMap(transactionCallbackImplClassMap);
        jarEntryPreHandle1Parser.setTransactionCallbackWithoutResultChildClassMap(transactionCallbackWithoutResultChildClassMap);
        jarEntryPreHandle1Parser.setThreadChildClassMap(threadChildClassMap);
        jarEntryPreHandle1Parser.setClassAndSuperMap(classAndSuperMap);
        jarEntryPreHandle1Parser.setInterfaceExtendsSet(interfaceExtendsSet);
        jarEntryPreHandle1Parser.setAllClassNameSet(allClassNameSet);
        jarEntryPreHandle1Parser.setClassAndJarNum(classAndJarNum);
        jarEntryPreHandle1Parser.setSpringDefineBeanHandler(springDefineBeanHandler);
        jarEntryPreHandle1Parser.setExtensionsManager(extensionsManager);
        jarEntryPreHandle1Parser.setSpringDefineComponentScanHandler(springDefineComponentScanHandler);
        jarEntryPreHandle1Parser.setSpringDefineAopHandler(springDefineAopHandler);

        // 第二次预处理相关
        /*
            类涉及继承的信息
            key     类名
            value   类涉及继承的信息，包含类的accessFlags，父类
        */
        Map<String, ClassExtendsInfo> classExtendsInfoMap = new HashMap<>(JavaCG2Constants.SIZE_100);

        /*
            父类对应的子类信息
            key     父类类名
            value   子类类名列表
         */
        Map<String, List<String>> childrenClassMap = new HashMap<>(JavaCG2Constants.SIZE_100);

        /*
            接口涉及继承的信息
            key     类名
            value   接口继承的信息
        */
        Map<String, List<String>> interfaceExtendsInfoMap = new HashMap<>(JavaCG2Constants.SIZE_100);

        /*
            父接口对应的子接口信息
            key     父接口类名
            value   子接口类名列表
         */
        Map<String, List<String>> childrenInterfaceMap = new HashMap<>(JavaCG2Constants.SIZE_100);

        // Spring依赖注入字段解析不依赖符号执行，此处无条件创建相关handler
        useSpringBeanByAnnotationHandler = new UseSpringBeanByAnnotationHandler(
                classExtendsInfoMap,
                classImplementsInfoMap,
                interfaceExtendsInfoMap,
                springDefineBeanHandler,
                extensionsManager.getSpringXmlBeanParser());
        jarEntryPreHandle2Parser = new JarEntryPreHandle2Parser(javaCG2InputAndOutput, onlyOneJar, useSpringBeanByAnnotationHandler);
        jarEntryPreHandle2Parser.setClassExtendsImplMethodWithArgTypesMap(classExtendsImplMethodWithArgTypesMap);
        jarEntryPreHandle2Parser.setInterfaceMethodWithArgTypesMap(interfaceMethodWithArgTypesMap);
        jarEntryPreHandle2Parser.setClassAndSuperMap(classAndSuperMap);
        jarEntryPreHandle2Parser.setClassExtendsInfoMap(classExtendsInfoMap);
        jarEntryPreHandle2Parser.setChildrenClassMap(childrenClassMap);
        jarEntryPreHandle2Parser.setInterfaceExtendsSet(interfaceExtendsSet);
        jarEntryPreHandle2Parser.setAllClassNameSet(allClassNameSet);
        jarEntryPreHandle2Parser.setInterfaceExtendsInfoMap(interfaceExtendsInfoMap);
        jarEntryPreHandle2Parser.setChildrenInterfaceMap(childrenInterfaceMap);

        // 正式处理相关
        jarEntryHandleParser = new JarEntryHandleParser(javaCG2InputAndOutput, onlyOneJar);
        jarEntryHandleParser.setUseSpringBeanByAnnotationHandler(useSpringBeanByAnnotationHandler);
        jarEntryHandleParser.setRunnableImplClassMap(runnableImplClassMap);
        jarEntryHandleParser.setCallableImplClassMap(callableImplClassMap);
        jarEntryHandleParser.setTransactionCallbackImplClassMap(transactionCallbackImplClassMap);
        jarEntryHandleParser.setTransactionCallbackWithoutResultChildClassMap(transactionCallbackWithoutResultChildClassMap);
        jarEntryHandleParser.setThreadChildClassMap(threadChildClassMap);
        jarEntryHandleParser.recordExtensionsManager(extensionsManager);
        jarEntryHandleParser.setCallIdCounter(callIdCounter);
        jarEntryHandleParser.setClassNumCounter(classNumCounter);
        jarEntryHandleParser.setMethodNumCounter(methodNumCounter);
        jarEntryHandleParser.setFailCounter(failCounter);
        jarEntryHandleParser.setFieldRelationshipCounter(fieldRelationshipCounter);
        jarEntryHandleParser.setClassAndJarNum(classAndJarNum);

        // 继承及实现相关的方法处理相关
        extendsImplHandler = new ExtendsImplHandler(javaCG2InputAndOutput);
        extendsImplHandler.setCallIdCounter(callIdCounter);
        extendsImplHandler.setInterfaceMethodWithArgTypesMap(interfaceMethodWithArgTypesMap);
        extendsImplHandler.setClassExtendsImplMethodWithArgTypesMap(classExtendsImplMethodWithArgTypesMap);
        extendsImplHandler.setChildrenClassMap(childrenClassMap);
        extendsImplHandler.setInterfaceExtendsInfoMap(interfaceExtendsInfoMap);
        extendsImplHandler.setChildrenInterfaceMap(childrenInterfaceMap);
        extendsImplHandler.setClassImplementsInfoMap(classImplementsInfoMap);
        extendsImplHandler.setClassExtendsInfoMap(classExtendsInfoMap);
        extendsImplHandler.setAllClassNameSet(allClassNameSet);
        extendsImplHandler.setClassAndJarNum(classAndJarNum);
        return true;
    }

    // 打印生成的文件信息
    private void printOutputFileInfo(JavaCG2OutputInfo javaCG2OutputInfo) {
        for (JavaCG2OutPutFileTypeEnum javaCG2OutPutFileTypeEnum : JavaCG2OutPutFileTypeEnum.values()) {
            if (javaCG2OutPutFileTypeEnum == JavaCG2OutPutFileTypeEnum.OPFTE_ILLEGAL) {
                continue;
            }
            logger.info("写入文件: {}", javaCG2OutputInfo.getMainFilePath(javaCG2OutPutFileTypeEnum));
        }

        Set<String> otherFileNameSet = javaCG2OutputInfo.getOtherFileNameSet();
        if (otherFileNameSet.isEmpty()) {
            logger.info("不写入其他类型的文件");
            return;
        }

        for (String otherFileName : otherFileNameSet) {
            logger.info("写入其他类型的文件 {} {}", otherFileName, javaCG2OutputInfo.getOtherFilePath(otherFileName));
        }
    }

    // 处理一个jar文件
    private boolean handleJar(String jarFilePath, JavaCG2RecordSink methodCallSink, JavaCG2RecordSink springBeanSink, JavaCG2RecordSink springScanPackageSink, JavaCG2RecordSink springAopAspectSink,
                              JavaCG2RecordSink springAopPointcutSink, JavaCG2RecordSink springAopAdviceSink, JavaCG2ElManager javaCG2ElManager) {
        try {
            // 对Class进行预处理
            if (!preHandleClasses1(jarFilePath)) {
                return false;
            }

            // 对Class进行第二次预处理
            if (!preHandleClasses2(jarFilePath)) {
                return false;
            }

            // 处理当前jar文件中的class文件
            if (!jarEntryHandleParser.parse(jarFilePath)) {
                return false;
            }

            // 处理继承及实现相关的方法
            extendsImplHandler.setMethodCallSink(methodCallSink);
            extendsImplHandler.handle();

            // 记录Spring Bean的名称及类型
            recordSpringBeanNameAndType(javaCG2ElManager, springBeanSink);

            // 记录Spring包扫描路径
            recordSpringScanPackage(springScanPackageSink);

            // 记录Spring AOP信息
            recordSpringAop(springAopAspectSink, springAopPointcutSink, springAopAdviceSink);
            return true;
        } catch (Exception e) {
            logger.error("处理jar文件出现异常 {} ", jarFilePath, e);
            return false;
        }
    }

    // 对Class进行预处理
    private boolean preHandleClasses1(String jarFilePath) {
        return jarEntryPreHandle1Parser.parse(jarFilePath);
    }

    // 对Class进行第二次预处理
    private boolean preHandleClasses2(String jarFilePath) {
        return jarEntryPreHandle2Parser.parse(jarFilePath);
    }

    // 记录Spring Bean的名称及类型
    private void recordSpringBeanNameAndType(JavaCG2ElManager javaCG2ElManager, JavaCG2RecordSink springBeanSink) throws IOException {
        if (springDefineBeanHandler != null) {
            Map<String, SpringBeanInJava> springBeanInJavaMap = springDefineBeanHandler.getSpringBeanInJavaMap();
            if (!springBeanInJavaMap.isEmpty()) {
                List<String> springBeanNameList = new ArrayList<>(springBeanInJavaMap.keySet());
                Collections.sort(springBeanNameList);
                for (String springBeanName : springBeanNameList) {
                    SpringBeanInJava springBeanInJava = springBeanInJavaMap.get(springBeanName);
                    List<String> springBeanTypeList = springBeanInJava.getClassNameList();
                    for (int i = 0; i < springBeanTypeList.size(); i++) {
                        springBeanSink.writeRow(
                                springBeanName,
                                String.valueOf(i),
                                springBeanTypeList.get(i),
                                "",
                                JavaCG2Constants.FILE_KEY_SPRING_DEFINE_IN_JAVA,
                                springBeanInJava.getAnnotationClassName(),
                                springBeanInJava.getDefineClassName()
                        );
                    }
                }
            }
        }

        /*
            记录XML中定义的Spring Bean信息
            SpringXmlBeanParserInterface.getBeanClass方法会在UseSpringBeanByAnnotationHandler.getSpringBeanTypeList方法中被调用
            不只是为了将XML中定义的Spring Bean信息写入文件
         */
        SpringXmlBeanParserInterface springXmlBeanParser = extensionsManager.getSpringXmlBeanParser();
        if (springXmlBeanParser != null) {
            List<SpringBeanInXml> springBeanListInXml = springXmlBeanParser.getBeanInXml();
            for (SpringBeanInXml springBeanNameInXml : springBeanListInXml) {
                if (javaCG2ElManager.checkIgnoreSpringBeanInXml(springBeanNameInXml.getSpringBeanName(), springBeanNameInXml.getClassName(), springBeanNameInXml.getProfile())) {
                    // 通过表达式判断需要忽略对应的Spring Bean
                    continue;
                }

                springBeanSink.writeRow(
                        springBeanNameInXml.getSpringBeanName(),
                        "0",
                        springBeanNameInXml.getClassName(),
                        springBeanNameInXml.getProfile(),
                        JavaCG2Constants.FILE_KEY_SPRING_DEFINE_IN_XML,
                        "",
                        springBeanNameInXml.getXmlFilePath()
                );
            }
        }
    }

    // 记录Spring包扫描路径
    private void recordSpringScanPackage(JavaCG2RecordSink springScanPackageSink) throws IOException {
        Map<String, List<String>> springComponentScanMap = springDefineComponentScanHandler.getSpringComponentScanMap();
        if (springComponentScanMap.isEmpty()) {
            return;
        }

        List<String> componentScanClassNameList = new ArrayList<>(springComponentScanMap.keySet());
        Collections.sort(componentScanClassNameList);
        for (String componentScanClassName : componentScanClassNameList) {
            List<String> componentScanPathList = springComponentScanMap.get(componentScanClassName);
            for (int i = 0; i < componentScanPathList.size(); i++) {
                springScanPackageSink.writeRow(JavaCG2Constants.FILE_KEY_SPRING_DEFINE_IN_JAVA, String.valueOf(i), componentScanPathList.get(i),
                        componentScanClassName);
            }
        }
    }

    // 记录Spring AOP信息
    private void recordSpringAop(JavaCG2RecordSink springAopAspectSink, JavaCG2RecordSink springAopPointcutSink, JavaCG2RecordSink springAopAdviceSink) throws IOException {
        Map<String, Integer> aspectMap = springDefineAopHandler.getAspectMap();
        if (!aspectMap.isEmpty()) {
            List<String> aspectClassNameList = new ArrayList<>(aspectMap.keySet());
            Collections.sort(aspectClassNameList);
            for (String aspectClassName : aspectClassNameList) {
                Integer orderValue = aspectMap.get(aspectClassName);
                springAopAspectSink.writeRow(String.valueOf(orderValue), aspectClassName);
            }
        }

        Map<String, List<SpringAopPointcutInfo>> pointcutMap = springDefineAopHandler.getPointcutMap();
        if (!pointcutMap.isEmpty()) {
            List<String> pointcutAspectClassNameList = new ArrayList<>(pointcutMap.keySet());
            Collections.sort(pointcutAspectClassNameList);
            for (String pointcutAspectClassName : pointcutAspectClassNameList) {
                List<SpringAopPointcutInfo> pointcutList = pointcutMap.get(pointcutAspectClassName);
                for (SpringAopPointcutInfo springAopPointcutInfo : pointcutList) {
                    String expression = springAopPointcutInfo.getExpression();
                    JavaCG2YesNoEnum base64 = JavaCG2YesNoEnum.NO;
                    if (JavaCG2Util.checkNeedBase64(expression)) {
                        base64 = JavaCG2YesNoEnum.YES;
                        expression = JavaCG2Util.base64Encode(expression);
                    }
                    springAopPointcutSink.writeRow(base64.getStrValue(), expression, springAopPointcutInfo.getFullMethod());
                }
            }
        }

        Map<String, List<SpringAopAdviceInfo>> adviceMap = springDefineAopHandler.getAdviceMap();
        if (!adviceMap.isEmpty()) {
            List<String> adviceAspectClassNameList = new ArrayList<>(adviceMap.keySet());
            Collections.sort(adviceAspectClassNameList);
            for (String adviceAspectClassName : adviceAspectClassNameList) {
                List<SpringAopAdviceInfo> adviceList = adviceMap.get(adviceAspectClassName);
                for (SpringAopAdviceInfo springAopAdviceInfo : adviceList) {
                    String expression = springAopAdviceInfo.getExpression();
                    JavaCG2YesNoEnum base64 = JavaCG2YesNoEnum.NO;
                    if (JavaCG2Util.checkNeedBase64(expression)) {
                        base64 = JavaCG2YesNoEnum.YES;
                        expression = JavaCG2Util.base64Encode(expression);
                    }
                    springAopAdviceSink.writeRow(springAopAdviceInfo.getFullMethod(), springAopAdviceInfo.getReturnType(),
                            springAopAdviceInfo.getAnnotationSimpleClassName(), base64.getStrValue(), expression, String.valueOf(springAopAdviceInfo.getAspectOrder()));
                }
            }
        }
    }

    // 记录java-callgraph2组件使用的配置参数
    private boolean recordJavaCG2Config(JavaCG2RecordSink javaCG2ConfigSink) {
        MainConfigInterface[][] mainConfigs = new MainConfigInterface[][]{JavaCG2ConfigKeyEnum.values()};
        return javaCG2ConfigureWrapper.recordAllConfigToFile(javaCG2ConfigSink, mainConfigs, JavaCG2OtherConfigFileUseListEnum.values(),
                JavaCG2OtherConfigFileUseSetEnum.values(), JavaCG2ElConfigEnum.values());
    }

    // 获取扩展类管理类
    public ExtensionsManager getExtensionsManager() {
        return extensionsManager;
    }

    // 获取java-callgraph2的输入与输出数据
    public JavaCG2InputAndOutput getJavaCG2InputAndOutput() {
        return javaCG2InputAndOutput;
    }
}
