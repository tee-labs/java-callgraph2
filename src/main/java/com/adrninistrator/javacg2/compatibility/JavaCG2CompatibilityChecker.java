package com.adrninistrator.javacg2.compatibility;

import com.adrninistrator.javacg2.common.JavaCG2CommonNameConstants;
import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.enums.JavaCG2CallTypeEnum;
import com.adrninistrator.javacg2.common.enums.JavaCG2ConstantTypeEnum;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.common.enums.JavaCG2YesNoEnum;
import com.adrninistrator.javacg2.dto.accessflag.JavaCG2AccessFlags;
import com.adrninistrator.javacg2.dto.compatibility.JavaCG2CompatClassInfo;
import com.adrninistrator.javacg2.dto.compatibility.JavaCG2CompatFieldInfo;
import com.adrninistrator.javacg2.dto.compatibility.JavaCG2CompatMethodInfo;
import com.adrninistrator.javacg2.dto.output.JavaCG2OutputInfo;
import com.adrninistrator.javacg2.util.JavaCG2ByteCodeUtil;
import com.adrninistrator.javacg2.util.JavaCG2ClassMethodUtil;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import com.adrninistrator.javacg2.util.JavaCG2Util;
import com.adrninistrator.javacg2.writer.JavaCG2OutputAdapter;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSink;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Jar兼容性检查处理类，在解析完成后读回本次生成的结果文件，在内存中构建全集索引后进行比对检查，不依赖数据库
 * 检查内容包括（逻辑参考java-all-call-graph的Jar兼容性检查）：
 * 1 被引用的类不存在（读回class_reference、dup_class_reference）
 * 2 方法调用中的被调用类/方法不存在、可见性private/protected/default不允许访问、静态与非静态不一致（读回method_call）
 * 3 被使用的字段不存在、可见性不允许访问、静态与非静态不一致（读回field_usage_other）
 * 4 重复同名类中class文件HASH不同，及同名类之间public方法、字段冲突（读回dup_class_info、dup_method_info、dup_field_info）
 * 对JDK中的类（java.、javax.、javafx.、sun.、com.sun.、jdk.等包名前缀）不检查，避免将JDK类误报为缺失
 */
@Slf4j
public class JavaCG2CompatibilityChecker {

    // 除JavaCG2ClassMethodUtil.isClassInJdk方法判断的JDK包名前缀外，额外跳过检查的JDK内置包名前缀
    private static final String[] OTHER_JDK_CLASS_NAME_PREFIXES = new String[]{
            "jdk.", "org.omg.", "org.w3c.dom.", "org.xml.sax."
    };

    // java.lang.Object中定义的方法名及参数，被调用方法沿父类、接口向上未找到时，作为继承自java.lang.Object的方法处理
    private static final Set<String> OBJECT_METHOD_NAME_AND_ARGS_SET = new HashSet<>(Arrays.asList(
            "equals(java.lang.Object)", "getClass()", "hashCode()", "toString()", "notify()", "notifyAll()",
            "wait()", "wait(long)", "wait(long,int)", "clone()", "finalize()"));

    // 重复同名类冲突的问题类型，H:存在多种class文件HASH，M:public方法冲突，F:public字段冲突
    private static final String DUP_CONFLICT_TYPE_HASH = "H";
    private static final String DUP_CONFLICT_TYPE_METHOD = "M";
    private static final String DUP_CONFLICT_TYPE_FIELD = "F";

    // 代表方法定义在java.lang.Object中的标记
    private static final JavaCG2CompatMethodInfo METHOD_INFO_IN_OBJECT = new JavaCG2CompatMethodInfo();

    // 代表方法或字段所在的继承链中存在无法解析的类（JDK类或不在结果文件中的类），不能判断方法或字段是否存在的标记
    private static final JavaCG2CompatMethodInfo METHOD_INFO_UNKNOWN = new JavaCG2CompatMethodInfo();
    private static final JavaCG2CompatFieldInfo FIELD_INFO_UNKNOWN = new JavaCG2CompatFieldInfo();

    private final JavaCG2OutputInfo javaCG2OutputInfo;
    private final JavaCG2OutputAdapter outputAdapter;

    /*
        以下为读回结果文件后构建的索引，构建完成后只读
        key均为完整类名
     */
    // 类的信息，来自class_info
    private Map<String, JavaCG2CompatClassInfo> classInfoMap;
    // 重复同名类的信息，来自dup_class_info
    private Map<String, List<JavaCG2CompatClassInfo>> dupClassInfoListMap;
    // 类中定义的方法，来自method_info，内层key为完整方法
    private Map<String, Map<String, JavaCG2CompatMethodInfo>> classMethodMap;
    // 重复同名类中定义的方法，来自dup_method_info，内层key为完整方法
    private Map<String, Map<String, JavaCG2CompatMethodInfo>> dupClassMethodMap;
    // 类中定义的字段，来自field_info，内层key为完整类名+冒号+字段名称
    private Map<String, Map<String, JavaCG2CompatFieldInfo>> classFieldMap;
    // 重复同名类中定义的字段，来自dup_field_info，内层key为完整类名+冒号+字段名称
    private Map<String, Map<String, JavaCG2CompatFieldInfo>> dupClassFieldMap;
    // 类的父类与实现的接口，来自extends_impl
    private Map<String, List<String>> superClassesAndInterfacesMap;
    /*
        jar文件信息，来自jar_info
        key     jar文件序号
        value   jar文件路径及jar文件的内部路径，下标0为jar文件路径，下标1为jar文件的内部路径
     */
    private Map<Integer, String[]> jarInfoMap;

    public JavaCG2CompatibilityChecker(JavaCG2OutputInfo javaCG2OutputInfo, JavaCG2OutputAdapter outputAdapter) {
        this.javaCG2OutputInfo = javaCG2OutputInfo;
        this.outputAdapter = outputAdapter;
    }

    // 执行Jar兼容性检查，生成检查报告结果文件（未发现问题时生成内容为空的文件）
    public boolean check() {
        long startTime = System.currentTimeMillis();
        try {
            // 读回结果文件，构建索引
            buildIndexes();

            int problemCount;
            try (JavaCG2RecordSink classReferenceMissingSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_CLASS_REFERENCE_MISSING);
                 JavaCG2RecordSink methodCallMissingSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_METHOD_CALL_MISSING);
                 JavaCG2RecordSink fieldUsageMissingSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_FIELD_USAGE_MISSING);
                 JavaCG2RecordSink dupClassConflictSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_DUP_CLASS_CONFLICT)
            ) {
                problemCount = checkClassReference(classReferenceMissingSink)
                        + checkMethodCall(methodCallMissingSink)
                        + checkFieldUsage(fieldUsageMissingSink)
                        + checkDupClass(dupClassConflictSink);
            }
            log.info("Jar兼容性检查执行完毕，记录的问题数量: {} 耗时: {} 秒", problemCount, JavaCG2Util.getSpendSeconds(startTime));
            return true;
        } catch (Exception e) {
            log.error("Jar兼容性检查出现异常 ", e);
            return false;
        }
    }

    // 读回结果文件，构建索引
    private void buildIndexes() throws IOException {
        classInfoMap = new HashMap<>(JavaCG2Constants.SIZE_1000);
        dupClassInfoListMap = new HashMap<>(JavaCG2Constants.SIZE_100);
        classMethodMap = new HashMap<>(JavaCG2Constants.SIZE_1000);
        dupClassMethodMap = new HashMap<>(JavaCG2Constants.SIZE_100);
        classFieldMap = new HashMap<>(JavaCG2Constants.SIZE_1000);
        dupClassFieldMap = new HashMap<>(JavaCG2Constants.SIZE_100);
        superClassesAndInterfacesMap = new HashMap<>(JavaCG2Constants.SIZE_1000);
        jarInfoMap = new HashMap<>(JavaCG2Constants.SIZE_100);

        buildClassInfoMap();
        buildDupClassInfoListMap();
        buildClassMethodMap();
        buildDupClassMethodMap();
        buildClassFieldMap();
        buildDupClassFieldMap();
        buildSuperClassesAndInterfacesMap();
        buildJarInfoMap();
    }

    // 读回class_info
    private void buildClassInfoMap() throws IOException {
        try (BufferedReader br = genBufferedReader4SourceFile(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO)) {
            if (br == null) {
                return;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < 5) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO.getFileName(), line);
                    continue;
                }
                JavaCG2CompatClassInfo classInfo = new JavaCG2CompatClassInfo();
                classInfo.setClassName(columns[0]);
                classInfo.setAccessFlags(Integer.parseInt(columns[1]));
                classInfo.setClassFileHash(columns[2]);
                classInfo.setJarNum(Integer.parseInt(columns[3]));
                classInfo.setClassPathInJar(columns[4]);
                classInfoMap.put(classInfo.getClassName(), classInfo);
            }
        }
    }

    // 读回dup_class_info
    private void buildDupClassInfoListMap() throws IOException {
        try (BufferedReader br = genBufferedReader4SourceFile(JavaCG2OutPutFileTypeEnum.OPFTE_DUP_CLASS_INFO)) {
            if (br == null) {
                return;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < 5) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_DUP_CLASS_INFO.getFileName(), line);
                    continue;
                }
                JavaCG2CompatClassInfo classInfo = new JavaCG2CompatClassInfo();
                classInfo.setClassName(columns[0]);
                classInfo.setAccessFlags(Integer.parseInt(columns[1]));
                classInfo.setClassFileHash(columns[2]);
                classInfo.setJarNum(Integer.parseInt(columns[3]));
                classInfo.setClassPathInJar(columns[4]);
                List<JavaCG2CompatClassInfo> dupClassInfoList = dupClassInfoListMap.computeIfAbsent(classInfo.getClassName(), k -> new ArrayList<>());
                dupClassInfoList.add(classInfo);
            }
        }
    }

    // 读回method_info
    private void buildClassMethodMap() throws IOException {
        try (BufferedReader br = genBufferedReader4SourceFile(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_INFO)) {
            if (br == null) {
                return;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < 8) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_INFO.getFileName(), line);
                    continue;
                }
                JavaCG2CompatMethodInfo methodInfo = new JavaCG2CompatMethodInfo();
                methodInfo.setFullMethod(columns[0]);
                methodInfo.setAccessFlags(Integer.parseInt(columns[1]));
                methodInfo.setReturnType(columns[2]);
                methodInfo.setJarNum(Integer.parseInt(columns[7]));
                Map<String, JavaCG2CompatMethodInfo> methodMap = classMethodMap.computeIfAbsent(
                        JavaCG2ClassMethodUtil.getClassNameFromMethod(methodInfo.getFullMethod()), k -> new HashMap<>());
                methodMap.putIfAbsent(methodInfo.getFullMethod(), methodInfo);
            }
        }
    }

    // 读回dup_method_info
    private void buildDupClassMethodMap() throws IOException {
        try (BufferedReader br = genBufferedReader4SourceFile(JavaCG2OutPutFileTypeEnum.OPFTE_DUP_METHOD_INFO)) {
            if (br == null) {
                return;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < 8) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_DUP_METHOD_INFO.getFileName(), line);
                    continue;
                }
                JavaCG2CompatMethodInfo methodInfo = new JavaCG2CompatMethodInfo();
                methodInfo.setFullMethod(columns[0]);
                methodInfo.setAccessFlags(Integer.parseInt(columns[1]));
                methodInfo.setReturnType(columns[2]);
                methodInfo.setJarNum(Integer.parseInt(columns[7]));
                Map<String, JavaCG2CompatMethodInfo> methodMap = dupClassMethodMap.computeIfAbsent(
                        JavaCG2ClassMethodUtil.getClassNameFromMethod(methodInfo.getFullMethod()), k -> new HashMap<>());
                methodMap.putIfAbsent(methodInfo.getFullMethod(), methodInfo);
            }
        }
    }

    // 读回field_info
    private void buildClassFieldMap() throws IOException {
        try (BufferedReader br = genBufferedReader4SourceFile(JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_INFO)) {
            if (br == null) {
                return;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < 13) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_INFO.getFileName(), line);
                    continue;
                }
                JavaCG2CompatFieldInfo fieldInfo = genCompatFieldInfo(columns);
                Map<String, JavaCG2CompatFieldInfo> fieldMap = classFieldMap.computeIfAbsent(fieldInfo.getClassName(), k -> new HashMap<>());
                fieldMap.putIfAbsent(genFieldKey(fieldInfo.getClassName(), fieldInfo.getFieldName()), fieldInfo);
            }
        }
    }

    // 读回dup_field_info
    private void buildDupClassFieldMap() throws IOException {
        try (BufferedReader br = genBufferedReader4SourceFile(JavaCG2OutPutFileTypeEnum.OPFTE_DUP_FIELD_INFO)) {
            if (br == null) {
                return;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < 13) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_DUP_FIELD_INFO.getFileName(), line);
                    continue;
                }
                JavaCG2CompatFieldInfo fieldInfo = genCompatFieldInfo(columns);
                Map<String, JavaCG2CompatFieldInfo> fieldMap = dupClassFieldMap.computeIfAbsent(fieldInfo.getClassName(), k -> new HashMap<>());
                fieldMap.putIfAbsent(genFieldKey(fieldInfo.getClassName(), fieldInfo.getFieldName()), fieldInfo);
            }
        }
    }

    private JavaCG2CompatFieldInfo genCompatFieldInfo(String[] columns) {
        JavaCG2CompatFieldInfo fieldInfo = new JavaCG2CompatFieldInfo();
        fieldInfo.setClassName(columns[0]);
        fieldInfo.setFieldName(columns[1]);
        fieldInfo.setFieldType(columns[2]);
        fieldInfo.setModifiers(columns[5]);
        fieldInfo.setStaticFlag(columns[7]);
        fieldInfo.setJarNum(Integer.parseInt(columns[12]));
        return fieldInfo;
    }

    // 读回extends_impl
    private void buildSuperClassesAndInterfacesMap() throws IOException {
        try (BufferedReader br = genBufferedReader4SourceFile(JavaCG2OutPutFileTypeEnum.OPFTE_EXTENDS_IMPL)) {
            if (br == null) {
                return;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < 4) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_EXTENDS_IMPL.getFileName(), line);
                    continue;
                }
                List<String> superClassList = superClassesAndInterfacesMap.computeIfAbsent(columns[0], k -> new ArrayList<>());
                if (!superClassList.contains(columns[3])) {
                    superClassList.add(columns[3]);
                }
            }
        }
    }

    // 读回jar_info
    private void buildJarInfoMap() throws IOException {
        try (BufferedReader br = genBufferedReader4SourceFile(JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO)) {
            if (br == null) {
                return;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < 4) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO.getFileName(), line);
                    continue;
                }
                jarInfoMap.put(Integer.parseInt(columns[1]), new String[]{columns[2], columns[3]});
            }
        }
    }

    // 检查被引用的类是否存在
    private int checkClassReference(JavaCG2RecordSink sink) throws IOException {
        Set<String> recordedKeySet = new HashSet<>();
        int count = checkOneClassReferenceFile(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_REFERENCE, sink, recordedKeySet);
        count += checkOneClassReferenceFile(JavaCG2OutPutFileTypeEnum.OPFTE_DUP_CLASS_REFERENCE, sink, recordedKeySet);
        return count;
    }

    // 检查一个类引用关系结果文件中被引用的类是否存在
    private int checkOneClassReferenceFile(JavaCG2OutPutFileTypeEnum fileTypeEnum, JavaCG2RecordSink sink, Set<String> recordedKeySet) throws IOException {
        int count = 0;
        try (BufferedReader br = genBufferedReader4SourceFile(fileTypeEnum)) {
            if (br == null) {
                return 0;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < 3) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", fileTypeEnum.getFileName(), line);
                    continue;
                }
                String className = columns[0];
                String referencedClassName = columns[1];
                if (existsClass(referencedClassName) || skipCheckClass(referencedClassName)) {
                    continue;
                }
                // 被引用的类不存在，去重后记录
                String key = className + ">" + referencedClassName;
                if (!recordedKeySet.add(key)) {
                    continue;
                }
                String[] jarPathAndInnerPath = queryJarPathAndInnerPath(columns[2]);
                sink.writeRow(className, referencedClassName, columns[2], jarPathAndInnerPath[0], jarPathAndInnerPath[1]);
                count++;
            }
        }
        return count;
    }

    // 检查方法调用中的被调用类/方法是否存在、可见性、静态与非静态一致性
    private int checkMethodCall(JavaCG2RecordSink sink) throws IOException {
        int count = 0;
        try (BufferedReader br = genBufferedReader4SourceFile(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL)) {
            if (br == null) {
                return 0;
            }
            List<String> existsInstructionCallTypeList = JavaCG2CallTypeEnum.getExistsInstructionList();
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < 12) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL.getFileName(), line);
                    continue;
                }
                if (!JavaCG2YesNoEnum.isYes(columns[1])) {
                    // 未启用的方法调用不检查
                    continue;
                }
                // 第4列为"(方法调用类型)被调用方，完整方法"
                String calleeWithCallType = columns[3];
                String callType = StringUtils.substringBetween(calleeWithCallType, JavaCG2Constants.FLAG_LEFT_BRACKET, JavaCG2Constants.FLAG_RIGHT_BRACKET);
                if (!existsInstructionCallTypeList.contains(callType)) {
                    // 仅检查存在指令的方法调用类型
                    continue;
                }
                String callerFullMethod = columns[2];
                String calleeFullMethod = StringUtils.substringAfter(calleeWithCallType, JavaCG2Constants.FLAG_RIGHT_BRACKET);
                String calleeClassName = JavaCG2ClassMethodUtil.getClassNameFromMethod(calleeFullMethod);
                if (calleeClassName.contains(JavaCG2Constants.FLAG_ARRAY)) {
                    // 被调用类为数组形式（如方法引用产生的数组构造方法调用）不检查
                    continue;
                }
                if (JavaCG2ConstantTypeEnum.isPrimitiveType(calleeClassName) || skipCheckClass(calleeClassName)) {
                    continue;
                }

                // 检查被调用类是否存在
                if (!existsClass(calleeClassName)) {
                    recordMethodCallProblem(sink, "方法调用中的被调用类未找到", callerFullMethod, columns[4], callType, calleeFullMethod, columns[8], columns[10], null);
                    count++;
                    continue;
                }

                // 检查被调用方法是否存在
                String methodNameAndArgs = StringUtils.substringAfter(calleeFullMethod, JavaCG2Constants.FLAG_COLON);
                JavaCG2CompatMethodInfo methodInfo = findMethodInHierarchy(calleeClassName, methodNameAndArgs);
                if (methodInfo == METHOD_INFO_UNKNOWN) {
                    // 继承链中存在无法解析的类（JDK类或不在结果文件中的类），不判断方法是否存在
                    continue;
                }
                if (methodInfo == null) {
                    recordMethodCallProblem(sink, "未找到被调用方法", callerFullMethod, columns[4], callType, calleeFullMethod, columns[8], columns[10], null);
                    count++;
                    continue;
                }
                // 检查存在的方法
                count += checkExistedMethod(sink, callerFullMethod, columns[4], callType, calleeFullMethod, columns[8], columns[10], methodInfo);
            }
        }
        return count;
    }

    // 检查存在的方法
    private int checkExistedMethod(JavaCG2RecordSink sink, String callerFullMethod, String lineNumber, String callType, String calleeFullMethod,
                                   String rawReturnType, String callerJarNum, JavaCG2CompatMethodInfo methodInfo) throws IOException {
        int count = 0;
        JavaCG2AccessFlags methodAccessFlags = new JavaCG2AccessFlags(methodInfo.getAccessFlags());
        String callerClassName = JavaCG2ClassMethodUtil.getClassNameFromMethod(callerFullMethod);
        String calleeClassName = JavaCG2ClassMethodUtil.getClassNameFromMethod(calleeFullMethod);
        String methodNameAndArgs = StringUtils.substringAfter(calleeFullMethod, JavaCG2Constants.FLAG_COLON);

        // 检查方法可见性
        if (methodAccessFlags.isPrivate()) {
            if (!callerClassName.equals(calleeClassName) && !checkInnerClassRelated(callerClassName, calleeClassName)) {
                // 记录存在问题的被调用方法
                recordMethodCallProblem(sink, "被调用方法为private方法，无法被其他类调用", callerFullMethod, lineNumber, callType, calleeFullMethod, rawReturnType, callerJarNum,
                        methodInfo);
                count++;
            }
        } else if (methodAccessFlags.isProtected()) {
            // 方法实际定义在的类
            String methodInClassName = JavaCG2ClassMethodUtil.getClassNameFromMethod(methodInfo.getFullMethod());
            if (!callerClassName.equals(calleeClassName)
                    && !JavaCG2ClassMethodUtil.checkSamePackage(callerClassName, methodInClassName)
                    && !checkExtendsImplClass(calleeClassName, callerClassName)
                    && !checkExtendsImplClass(methodInClassName, callerClassName)
                    // 被调用方法为clone()方法时不检查
                    && !"clone()".equals(methodNameAndArgs)) {
                // 记录存在问题的被调用方法
                recordMethodCallProblem(sink, "被调用方法为protected方法，调用方法不满足以下任意情况：1、相同的类，2、相同包中的类，3、子类或实现类", callerFullMethod, lineNumber, callType,
                        calleeFullMethod, rawReturnType, callerJarNum, methodInfo);
                count++;
            }
        } else if (!methodAccessFlags.isPublic()) {
            if (!callerClassName.equals(calleeClassName) && !JavaCG2ClassMethodUtil.checkSamePackage(callerClassName, calleeClassName)) {
                // 记录存在问题的被调用方法
                recordMethodCallProblem(sink, "被调用方法修饰符为default，调用方法不满足以下任意情况：1、相同的类，2、相同包中的类", callerFullMethod, lineNumber, callType, calleeFullMethod,
                        rawReturnType, callerJarNum, methodInfo);
                count++;
            }
        }

        // 检查方法是否静态/非静态，INVOKEDYNAMIC指令调用的方法（如Lambda表达式对应的方法）不做静态性检查
        if (!JavaCG2CallTypeEnum.CTE_RAW_INVOKE_DYNAMIC.getType().equals(callType)) {
            if (JavaCG2CallTypeEnum.CTE_RAW_INVOKE_STATIC.getType().equals(callType) && !methodAccessFlags.isStatic()) {
                recordMethodCallProblem(sink, "被调用方法编译时属于静态方法，但目前是非静态方法", callerFullMethod, lineNumber, callType, calleeFullMethod, rawReturnType, callerJarNum,
                        methodInfo);
                count++;
            } else if (!JavaCG2CallTypeEnum.CTE_RAW_INVOKE_STATIC.getType().equals(callType) && methodAccessFlags.isStatic()) {
                recordMethodCallProblem(sink, "被调用方法编译时属于非静态方法，但目前是静态方法", callerFullMethod, lineNumber, callType, calleeFullMethod, rawReturnType, callerJarNum,
                        methodInfo);
                count++;
            }
        }
        return count;
    }

    // 检查被使用的字段是否存在、可见性、静态与非静态一致性
    private int checkFieldUsage(JavaCG2RecordSink sink) throws IOException {
        int count = 0;
        try (BufferedReader br = genBufferedReader4SourceFile(JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_USAGE_OTHER)) {
            if (br == null) {
                return 0;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < 10) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_USAGE_OTHER.getFileName(), line);
                    continue;
                }
                String callerFullMethod = columns[0];
                String fieldInClassName = columns[4];
                if (fieldInClassName.contains(JavaCG2Constants.FLAG_ARRAY)) {
                    // 被使用的字段所在类为数组形式时不检查
                    continue;
                }
                if (JavaCG2ConstantTypeEnum.isPrimitiveType(fieldInClassName) || skipCheckClass(fieldInClassName)) {
                    continue;
                }

                // 检查字段所在的类是否存在
                if (!existsClass(fieldInClassName)) {
                    recordFieldUsageProblem(sink, "被使用字段所在的类未找到", callerFullMethod, columns[3], fieldInClassName, columns[5], columns[6], columns[8], null);
                    count++;
                    continue;
                }

                // 检查被使用的字段是否存在
                JavaCG2CompatFieldInfo fieldInfo = findFieldInHierarchy(fieldInClassName, columns[5], JavaCG2ClassMethodUtil.removeAllArrayFlag(columns[6]));
                if (fieldInfo == FIELD_INFO_UNKNOWN) {
                    // 继承链中存在无法解析的类（JDK类或不在结果文件中的类），不判断字段是否存在
                    continue;
                }
                if (fieldInfo == null) {
                    recordFieldUsageProblem(sink, "未找到被使用的字段", callerFullMethod, columns[3], fieldInClassName, columns[5], columns[6], columns[8], null);
                    count++;
                    continue;
                }
                // 检查存在的字段
                count += checkExistedField(sink, callerFullMethod, columns, fieldInfo);
            }
        }
        return count;
    }

    // 检查存在的字段
    private int checkExistedField(JavaCG2RecordSink sink, String callerFullMethod, String[] columns, JavaCG2CompatFieldInfo fieldInfo) throws IOException {
        int count = 0;
        String callerClassName = JavaCG2ClassMethodUtil.getClassNameFromMethod(callerFullMethod);
        String fieldInClassName = columns[4];
        String fieldName = columns[5];
        String fieldType = columns[6];

        // 检查字段可见性
        if (JavaCG2ByteCodeUtil.isPrivate(fieldInfo.getModifiers())) {
            if (!callerClassName.equals(fieldInClassName) && !checkInnerClassRelated(callerClassName, fieldInClassName)) {
                recordFieldUsageProblem(sink, "被使用的字段为private字段，无法被其他类使用", callerFullMethod, columns[3], fieldInClassName, fieldName, fieldType, columns[8], fieldInfo);
                count++;
            }
        } else if (JavaCG2ByteCodeUtil.isProtected(fieldInfo.getModifiers())) {
            if (!callerClassName.equals(fieldInClassName)
                    && !JavaCG2ClassMethodUtil.checkSamePackage(callerClassName, fieldInfo.getClassName())
                    && !checkExtendsImplClass(fieldInClassName, callerClassName)
                    && !checkExtendsImplClass(fieldInfo.getClassName(), callerClassName)) {
                recordFieldUsageProblem(sink, "被使用的字段为protected字段，使用字段的类不满足以下任意情况：1、相同的类，2、相同包中的类，3、子类或实现类", callerFullMethod, columns[3],
                        fieldInClassName, fieldName, fieldType, columns[8], fieldInfo);
                count++;
            }
        } else if (!JavaCG2ByteCodeUtil.isPublic(fieldInfo.getModifiers())) {
            if (!callerClassName.equals(fieldInClassName) && !JavaCG2ClassMethodUtil.checkSamePackage(callerClassName, fieldInClassName)) {
                recordFieldUsageProblem(sink, "被使用的字段修饰符为default，使用字段的类不满足以下任意情况：1、相同的类，2、相同包中的类", callerFullMethod, columns[3], fieldInClassName, fieldName,
                        fieldType, columns[8], fieldInfo);
                count++;
            }
        }

        // 检查字段是否静态/非静态
        if (!columns[2].equals(fieldInfo.getStaticFlag())) {
            if (JavaCG2YesNoEnum.isYes(columns[2])) {
                recordFieldUsageProblem(sink, "被使用的字段编译时属于静态字段，但目前是非静态字段", callerFullMethod, columns[3], fieldInClassName, fieldName, fieldType, columns[8], fieldInfo);
            } else {
                recordFieldUsageProblem(sink, "被使用的字段编译时属于非静态字段，但目前是静态字段", callerFullMethod, columns[3], fieldInClassName, fieldName, fieldType, columns[8], fieldInfo);
            }
            count++;
        }
        return count;
    }

    // 检查重复同名类，包括class文件HASH不同的类，及同名类之间public方法、字段冲突
    private int checkDupClass(JavaCG2RecordSink sink) throws IOException {
        int count = 0;
        for (Map.Entry<String, List<JavaCG2CompatClassInfo>> entry : dupClassInfoListMap.entrySet()) {
            String className = entry.getKey();
            JavaCG2CompatClassInfo primaryClassInfo = classInfoMap.get(className);
            if (primaryClassInfo == null) {
                continue;
            }
            for (JavaCG2CompatClassInfo dupClassInfo : entry.getValue()) {
                if (StringUtils.equals(dupClassInfo.getClassFileHash(), primaryClassInfo.getClassFileHash())) {
                    // 重复同名类与原始类文件的HASH相同，跳过
                    continue;
                }
                // 记录存在多种class文件HASH
                recordDupClassConflict(sink, DUP_CONFLICT_TYPE_HASH, className, primaryClassInfo, dupClassInfo, "", "", "两个版本的class文件HASH不同");
                count++;

                // 检查同名类之间public方法冲突
                count += checkDupClassMethod(sink, className, primaryClassInfo, dupClassInfo);

                // 检查同名类之间public字段冲突
                count += checkDupClassField(sink, className, primaryClassInfo, dupClassInfo);
            }
        }
        return count;
    }

    // 检查重复同名类之间public方法冲突
    private int checkDupClassMethod(JavaCG2RecordSink sink, String className, JavaCG2CompatClassInfo primaryClassInfo, JavaCG2CompatClassInfo dupClassInfo) throws IOException {
        Map<String, JavaCG2CompatMethodInfo> primaryMethodMap = genPublicMethodMap(classMethodMap.get(className));
        Map<String, JavaCG2CompatMethodInfo> dupMethodMap = genPublicMethodMap(dupClassMethodMap.get(className));
        if (primaryMethodMap.isEmpty() && dupMethodMap.isEmpty()) {
            return 0;
        }

        int count = 0;
        // 原始类中存在的public方法在重复同名类中未找到
        for (Map.Entry<String, JavaCG2CompatMethodInfo> entry : primaryMethodMap.entrySet()) {
            if (dupMethodMap.containsKey(entry.getKey())) {
                continue;
            }
            JavaCG2CompatMethodInfo methodInfo = entry.getValue();
            recordDupClassConflict(sink, DUP_CONFLICT_TYPE_METHOD, className, primaryClassInfo, dupClassInfo,
                    StringUtils.substringAfter(methodInfo.getFullMethod(), JavaCG2Constants.FLAG_COLON), methodInfo.getReturnType(),
                    "类版本1存在对应的public方法而类版本2不存在");
            count++;
        }
        // 重复同名类中存在的public方法在原始类中未找到
        for (Map.Entry<String, JavaCG2CompatMethodInfo> entry : dupMethodMap.entrySet()) {
            if (primaryMethodMap.containsKey(entry.getKey())) {
                continue;
            }
            JavaCG2CompatMethodInfo methodInfo = entry.getValue();
            recordDupClassConflict(sink, DUP_CONFLICT_TYPE_METHOD, className, dupClassInfo, primaryClassInfo,
                    StringUtils.substringAfter(methodInfo.getFullMethod(), JavaCG2Constants.FLAG_COLON), methodInfo.getReturnType(),
                    "类版本2存在对应的public方法而类版本1不存在");
            count++;
        }
        return count;
    }

    // 检查重复同名类之间public字段冲突
    private int checkDupClassField(JavaCG2RecordSink sink, String className, JavaCG2CompatClassInfo primaryClassInfo, JavaCG2CompatClassInfo dupClassInfo) throws IOException {
        Map<String, JavaCG2CompatFieldInfo> primaryFieldMap = genPublicFieldMap(classFieldMap.get(className));
        Map<String, JavaCG2CompatFieldInfo> dupFieldMap = genPublicFieldMap(dupClassFieldMap.get(className));
        if (primaryFieldMap.isEmpty() && dupFieldMap.isEmpty()) {
            return 0;
        }

        int count = 0;
        // 原始类中存在的public字段在重复同名类中未找到
        for (Map.Entry<String, JavaCG2CompatFieldInfo> entry : primaryFieldMap.entrySet()) {
            if (dupFieldMap.containsKey(entry.getKey())) {
                continue;
            }
            JavaCG2CompatFieldInfo fieldInfo = entry.getValue();
            recordDupClassConflict(sink, DUP_CONFLICT_TYPE_FIELD, className, primaryClassInfo, dupClassInfo, fieldInfo.getFieldName(), fieldInfo.getFieldType(),
                    "类版本1存在对应的public字段而类版本2不存在");
            count++;
        }
        // 重复同名类中存在的public字段在原始类中未找到
        for (Map.Entry<String, JavaCG2CompatFieldInfo> entry : dupFieldMap.entrySet()) {
            if (primaryFieldMap.containsKey(entry.getKey())) {
                continue;
            }
            JavaCG2CompatFieldInfo fieldInfo = entry.getValue();
            recordDupClassConflict(sink, DUP_CONFLICT_TYPE_FIELD, className, dupClassInfo, primaryClassInfo, fieldInfo.getFieldName(), fieldInfo.getFieldType(),
                    "类版本2存在对应的public字段而类版本1不存在");
            count++;
        }
        return count;
    }

    // 从类的所有方法中获取public方法，key为用于比较兼容性的方法串
    private Map<String, JavaCG2CompatMethodInfo> genPublicMethodMap(Map<String, JavaCG2CompatMethodInfo> methodMap) {
        Map<String, JavaCG2CompatMethodInfo> publicMethodMap = new HashMap<>();
        if (methodMap == null) {
            return publicMethodMap;
        }
        for (JavaCG2CompatMethodInfo methodInfo : methodMap.values()) {
            if (!JavaCG2ByteCodeUtil.isPublicFlag(methodInfo.getAccessFlags())) {
                continue;
            }
            publicMethodMap.put(genMethodCompatibilityKey(methodInfo), methodInfo);
        }
        return publicMethodMap;
    }

    // 从类的所有字段中获取public字段，key为用于比较兼容性的字段串
    private Map<String, JavaCG2CompatFieldInfo> genPublicFieldMap(Map<String, JavaCG2CompatFieldInfo> fieldMap) {
        Map<String, JavaCG2CompatFieldInfo> publicFieldMap = new HashMap<>();
        if (fieldMap == null) {
            return publicFieldMap;
        }
        for (JavaCG2CompatFieldInfo fieldInfo : fieldMap.values()) {
            if (!JavaCG2ByteCodeUtil.isPublic(fieldInfo.getModifiers())) {
                continue;
            }
            publicFieldMap.put(genFieldCompatibilityKey(fieldInfo), fieldInfo);
        }
        return publicFieldMap;
    }

    // 生成用于比较重复同名类方法兼容性的方法串，方法名及参数+返回类型+是否静态
    private String genMethodCompatibilityKey(JavaCG2CompatMethodInfo methodInfo) {
        String methodNameAndArgs = StringUtils.substringAfter(methodInfo.getFullMethod(), JavaCG2Constants.FLAG_COLON);
        return methodNameAndArgs + JavaCG2Constants.FLAG_COLON + methodInfo.getReturnType() + JavaCG2Constants.FLAG_COLON
                + JavaCG2YesNoEnum.parseStrValue(new JavaCG2AccessFlags(methodInfo.getAccessFlags()).isStatic());
    }

    // 生成用于比较重复同名类字段兼容性的字段串，字段名称+字段类型+是否静态
    private String genFieldCompatibilityKey(JavaCG2CompatFieldInfo fieldInfo) {
        return fieldInfo.getFieldName() + JavaCG2Constants.FLAG_COLON + fieldInfo.getFieldType() + JavaCG2Constants.FLAG_COLON + fieldInfo.getStaticFlag();
    }

    /**
     * 沿父类与接口向上查找方法
     *
     * @param className          被调用方法所在的类名
     * @param methodNameAndArgs  方法名及参数
     * @return 找到的方法信息；java.lang.Object中定义的方法返回METHOD_INFO_IN_OBJECT；继承链中存在无法解析的类时返回METHOD_INFO_UNKNOWN；整条继承链可解析但未找到方法时返回null
     */
    private JavaCG2CompatMethodInfo findMethodInHierarchy(String className, String methodNameAndArgs) {
        Set<String> handledClassSet = new HashSet<>();
        Deque<String> classNameDeque = new ArrayDeque<>();
        classNameDeque.add(className);
        handledClassSet.add(className);
        // 继承链中的类是否全部可解析
        boolean allResolved = true;
        while (!classNameDeque.isEmpty()) {
            String currentClassName = classNameDeque.removeFirst();
            JavaCG2CompatMethodInfo methodInfo = queryMethodByFullMethod(currentClassName, methodNameAndArgs);
            if (methodInfo != null) {
                // 在当前类中找到方法
                return methodInfo;
            }
            if (!existsClass(currentClassName)) {
                // 当前类为JDK类或不在结果文件中的类，无法继续判断
                allResolved = false;
                continue;
            }
            List<String> superClassList = superClassesAndInterfacesMap.get(currentClassName);
            if (superClassList == null) {
                continue;
            }
            for (String superClassName : superClassList) {
                if (handledClassSet.add(superClassName)) {
                    classNameDeque.add(superClassName);
                }
            }
        }
        if (allResolved && OBJECT_METHOD_NAME_AND_ARGS_SET.contains(methodNameAndArgs)) {
            // 方法为java.lang.Object中定义的方法
            return METHOD_INFO_IN_OBJECT;
        }
        return allResolved ? null : METHOD_INFO_UNKNOWN;
    }

    /**
     * 沿父类与接口向上查找字段
     *
     * @param className      被使用的字段所在的类名
     * @param fieldName      字段名称
     * @param fieldType      字段类型（不包含数组标志）
     * @return 找到的字段信息；继承链中存在无法解析的类时返回FIELD_INFO_UNKNOWN；整条继承链可解析但未找到字段时返回null
     */
    private JavaCG2CompatFieldInfo findFieldInHierarchy(String className, String fieldName, String fieldType) {
        Set<String> handledClassSet = new HashSet<>();
        Deque<String> classNameDeque = new ArrayDeque<>();
        classNameDeque.add(className);
        handledClassSet.add(className);
        // 继承链中的类是否全部可解析
        boolean allResolved = true;
        while (!classNameDeque.isEmpty()) {
            String currentClassName = classNameDeque.removeFirst();
            Map<String, JavaCG2CompatFieldInfo> fieldMap = classFieldMap.get(currentClassName);
            if (fieldMap != null) {
                JavaCG2CompatFieldInfo fieldInfo = fieldMap.get(genFieldKey(currentClassName, fieldName));
                if (fieldInfo != null && fieldInfo.getFieldType().equals(fieldType)) {
                    // 在当前类中找到字段
                    return fieldInfo;
                }
            }
            if (!existsClass(currentClassName)) {
                // 当前类为JDK类或不在结果文件中的类，无法继续判断
                allResolved = false;
                continue;
            }
            List<String> superClassList = superClassesAndInterfacesMap.get(currentClassName);
            if (superClassList == null) {
                continue;
            }
            for (String superClassName : superClassList) {
                if (handledClassSet.add(superClassName)) {
                    classNameDeque.add(superClassName);
                }
            }
        }
        return allResolved ? null : FIELD_INFO_UNKNOWN;
    }

    // 根据类名与方法名及参数查询方法信息
    private JavaCG2CompatMethodInfo queryMethodByFullMethod(String className, String methodNameAndArgs) {
        Map<String, JavaCG2CompatMethodInfo> methodMap = classMethodMap.get(className);
        if (methodMap == null) {
            return null;
        }
        return methodMap.get(className + JavaCG2Constants.FLAG_COLON + methodNameAndArgs);
    }

    // 判断指定的类是否在class_info或dup_class_info结果文件中存在
    private boolean existsClass(String className) {
        return classInfoMap.containsKey(className) || dupClassInfoListMap.containsKey(className);
    }

    // 判断指定的类是否跳过检查（JDK中的类）
    private boolean skipCheckClass(String className) {
        return JavaCG2ClassMethodUtil.isClassInJdk(className) || StringUtils.startsWithAny(className, OTHER_JDK_CLASS_NAME_PREFIXES);
    }

    /**
     * 判断两个类是否存在内部类与外部类的关系（包含类自身）
     *
     * @param className1 类名1
     * @param className2 类名2
     * @return true: 存在内部类与外部类的关系
     */
    private boolean checkInnerClassRelated(String className1, String className2) {
        return genAllOuterClassNameSet(className1).contains(className2) || genAllOuterClassNameSet(className2).contains(className1);
    }

    // 获取指定类及全部外部类的类名Set
    private Set<String> genAllOuterClassNameSet(String className) {
        Set<String> outerClassNameSet = new HashSet<>();
        outerClassNameSet.add(className);
        String currentClassName = className;
        while (JavaCG2ClassMethodUtil.isInnerClass(currentClassName)) {
            currentClassName = JavaCG2ClassMethodUtil.parseOuterClassName(currentClassName);
            if (!outerClassNameSet.add(currentClassName)) {
                break;
            }
        }
        return outerClassNameSet;
    }

    /**
     * 判断一个类是否间接或直接继承或实现了另一个类
     *
     * @param upwardClassName   父类或接口
     * @param downwardClassName 子类或实现类
     * @return true: 子类或实现类间接或直接继承或实现了指定的父类或接口
     */
    private boolean checkExtendsImplClass(String upwardClassName, String downwardClassName) {
        if (JavaCG2CommonNameConstants.CLASS_NAME_OBJECT.equals(upwardClassName)) {
            // 判断是否为Object的子类时，默认为是
            return true;
        }

        Set<String> handledClassSet = new HashSet<>();
        Deque<String> classNameDeque = new ArrayDeque<>();
        classNameDeque.add(downwardClassName);
        handledClassSet.add(downwardClassName);
        while (!classNameDeque.isEmpty()) {
            String currentClassName = classNameDeque.removeFirst();
            if (currentClassName.equals(upwardClassName)) {
                return true;
            }
            List<String> superClassList = superClassesAndInterfacesMap.get(currentClassName);
            if (superClassList == null) {
                continue;
            }
            for (String superClassName : superClassList) {
                if (handledClassSet.add(superClassName)) {
                    classNameDeque.add(superClassName);
                }
            }
        }
        return false;
    }

    // 生成字段在字段信息Map中的key
    private String genFieldKey(String className, String fieldName) {
        return className + JavaCG2Constants.FLAG_COLON + fieldName;
    }

    // 记录方法调用存在的问题
    private void recordMethodCallProblem(JavaCG2RecordSink sink, String problemDesc, String callerFullMethod, String lineNumber, String callType, String calleeFullMethod,
                                         String rawReturnType, String callerJarNum, JavaCG2CompatMethodInfo methodInfo) throws IOException {
        // 检查明细
        String detail = "";
        if (methodInfo != null) {
            detail = "找到方法定义: " + methodInfo.getFullMethod() + " access_flags: " + methodInfo.getAccessFlags();
        }
        String[] jarPathAndInnerPath = queryJarPathAndInnerPath(callerJarNum);
        sink.writeRow(problemDesc, callerFullMethod, lineNumber, callType, calleeFullMethod, rawReturnType, callerJarNum, jarPathAndInnerPath[0], jarPathAndInnerPath[1],
                detail);
    }

    // 记录字段使用存在的问题
    private void recordFieldUsageProblem(JavaCG2RecordSink sink, String problemDesc, String callerFullMethod, String getOrPut, String fieldInClassName, String fieldName,
                                         String fieldType, String classJarNum, JavaCG2CompatFieldInfo fieldInfo) throws IOException {
        // 检查明细
        String detail = "";
        if (fieldInfo != null) {
            detail = "找到字段定义: " + JavaCG2ClassMethodUtil.formatClassAndField(fieldInfo.getClassName(), fieldInfo.getFieldName())
                    + " 修饰符: " + fieldInfo.getModifiers() + " static标志: " + fieldInfo.getStaticFlag();
        }
        String[] jarPathAndInnerPath = queryJarPathAndInnerPath(classJarNum);
        sink.writeRow(problemDesc, callerFullMethod, getOrPut, fieldInClassName, fieldName, fieldType, classJarNum, jarPathAndInnerPath[0], jarPathAndInnerPath[1], detail);
    }

    // 记录重复同名类冲突
    private void recordDupClassConflict(JavaCG2RecordSink sink, String conflictType, String className, JavaCG2CompatClassInfo classInfo1, JavaCG2CompatClassInfo classInfo2,
                                        String methodOrFieldName, String methodOrFieldType, String desc) throws IOException {
        String[] jarPathAndInnerPath1 = queryJarPathAndInnerPath(String.valueOf(classInfo1.getJarNum()));
        String[] jarPathAndInnerPath2 = queryJarPathAndInnerPath(String.valueOf(classInfo2.getJarNum()));
        sink.writeRow(conflictType,
                className,
                classInfo1.getClassPathInJar(),
                String.valueOf(classInfo1.getJarNum()),
                jarPathAndInnerPath1[0],
                classInfo2.getClassPathInJar(),
                String.valueOf(classInfo2.getJarNum()),
                jarPathAndInnerPath2[0],
                methodOrFieldName,
                methodOrFieldType,
                desc);
    }

    // 根据jar文件序号查询jar文件路径及jar文件的内部路径
    private String[] queryJarPathAndInnerPath(String jarNumStr) {
        int jarNum;
        try {
            jarNum = Integer.parseInt(jarNumStr);
        } catch (NumberFormatException e) {
            // jar文件序号为"-"等非数字内容时返回空
            return new String[]{"", ""};
        }
        String[] jarPathAndInnerPath = jarInfoMap.get(jarNum);
        return jarPathAndInnerPath == null ? new String[]{"", ""} : jarPathAndInnerPath;
    }

    // 为读取需要检查的结果文件生成BufferedReader，结果文件不存在时返回null
    private BufferedReader genBufferedReader4SourceFile(JavaCG2OutPutFileTypeEnum fileTypeEnum) throws IOException {
        String filePath = javaCG2OutputInfo.getMainFilePath(fileTypeEnum);
        if (!new File(filePath).exists()) {
            // 结果文件不存在（例如使用自定义的输出适配器时），跳过对应检查
            log.warn("Jar兼容性检查需要读取的结果文件不存在，跳过 {} {}", fileTypeEnum.getFileName(), filePath);
            return null;
        }
        return JavaCG2FileUtil.genBufferedReader(filePath);
    }
}
