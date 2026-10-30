package com.adrninistrator.javacg2.spring;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.SpringAnnotationConstants;
import com.adrninistrator.javacg2.common.enums.JavaCG2YesNoEnum;
import com.adrninistrator.javacg2.dto.counter.JavaCG2Counter;
import com.adrninistrator.javacg2.dto.spring.SpringControllerInJava;
import com.adrninistrator.javacg2.dto.spring.SpringTaskInJava;
import com.adrninistrator.javacg2.util.JavaCG2ClassMethodUtil;
import com.adrninistrator.javacg2.util.JavaCG2SpringUtil;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSink;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.ArrayElementValue;
import org.apache.bcel.classfile.ElementValue;
import org.apache.bcel.classfile.ElementValuePair;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.Type;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 从类及方法的注解中派生Spring Controller与Spring定时任务信息的处理器（内置默认实现）
 * 派生规则与java-all-call-graph的WriteDbHandler4ClassAnnotation、WriteDbHandler4MethodAnnotation中的对应逻辑保持一致：
 * 1 类上存在@Controller或@RestController注解时，对应的类为Spring MVC的Controller类
 * 2 类上存在@RequestMapping及@GetMapping等RequestMapping相关注解时记录类上的路径；与java-all-call-graph一致，类上只有@RequestMapping相关注解
 * 而没有Controller相关注解时，该类也会被视为Spring MVC相关的类
 * 3 Spring MVC相关类的方法上存在@RequestMapping相关注解时，将类上的路径与方法上的路径拼接后生成用于显示的URI
 * 4 方法上存在@Scheduled注解时，对应的方法为Spring定时任务方法
 * 派生的信息通过writeSpringController2Files、writeSpringTask2Files方法分别写入spring_controller、spring_task_java结果文件
 * 列格式与java-all-call-graph的jacg_spring_controller表、jacg_spring_task表在Java代码中定义时的记录保持一致
 */
public class SpringDefineControllerTaskHandler {

    // 方法可能用于文件下载判断时涉及的返回类型
    private static final String CLASS_NAME_HTTP_SERVLET_RESPONSE = "javax.servlet.http.HttpServletResponse";
    private static final String CLASS_NAME_HTTP_ENTITY = "org.springframework.http.HttpEntity";
    private static final String CLASS_NAME_RESPONSE_ENTITY = "org.springframework.http.ResponseEntity";
    private static final String CLASS_NAME_MULTI_PART_FILE = "org.springframework.web.multipart.MultipartFile";
    private static final String CLASS_NAME_COMMONS_MULTI_PART_FILE = "org.springframework.web.multipart.commons.CommonsMultipartFile";

    /*
        保存Spring MVC相关类名及类上的@RequestMapping相关注解的路径列表
        key     完整类名
        value   类上的@RequestMapping相关注解的路径属性值列表，类上没有@RequestMapping相关注解或有注解但未指定路径时为空列表
     */
    private final Map<String, List<String>> classRequestMappingMap = new HashMap<>();

    // 派生的Spring Controller信息
    private final List<SpringControllerInJava> springControllerList = new ArrayList<>();

    // 派生的Spring定时任务信息
    private final List<SpringTaskInJava> springTaskList = new ArrayList<>();

    /*
        简单类名与使用该简单类名的完整类名数量的Map
        key     简单类名
        value   使用该简单类名的完整类名数量
        在需要写文件时才生成
     */
    private Map<String, Integer> simpleClassNameCountMap;

    /*
        完整类名与对应的唯一类名的Map
        key     完整类名
        value   唯一类名，简单类名只对应一个完整类名时为简单类名，否则为完整类名
        在需要写文件时才生成
     */
    private Map<String, String> simpleClassNameMap;

    // 本次解析涉及的所有的类名，在写文件时用于判断唯一类名
    @Setter
    private Set<String> allClassNameSet;

    /**
     * 处理类上的注解，记录Spring Controller相关的类及类上的路径
     * 仅对非重复的类执行，与java-all-call-graph处理class_annotation文件的逻辑保持一致
     *
     * @param className         完整类名
     * @param annotationEntries 类上的注解
     */
    public void handleClassAnnotation(String className, AnnotationEntry[] annotationEntries) {
        if (annotationEntries == null || annotationEntries.length == 0) {
            return;
        }

        for (AnnotationEntry annotationEntry : annotationEntries) {
            String annotationName = genAnnotationName(annotationEntry);
            if (JavaCG2SpringUtil.checkControllerAnnotation(annotationName)) {
                // 假如某个类上有@Controller相关类注解，则尝试向Map中先加入空的数据，避免类上没有@RequestMapping相关类注解时不被认为属于Controller类
                classRequestMappingMap.putIfAbsent(className, Collections.<String>emptyList());
            } else if (JavaCG2SpringUtil.checkRequestMappingAnnotation(annotationName)) {
                ElementValuePair[] elementValuePairs = annotationEntry.getElementValuePairs();
                if (elementValuePairs == null || elementValuePairs.length == 0) {
                    // 类上的RequestMapping相关注解未指定任何属性，等价于路径属性值为空
                    classRequestMappingMap.put(className, Collections.<String>emptyList());
                    continue;
                }

                for (ElementValuePair elementValuePair : elementValuePairs) {
                    if (!JavaCG2SpringUtil.checkRequestMappingPathAttribute(elementValuePair.getNameString())) {
                        // 只处理RequestMapping相关注解中代表路径的属性
                        continue;
                    }
                    // 类上的RequestMapping相关注解的路径属性值
                    classRequestMappingMap.put(className, parsePathList(elementValuePair.getValue()));
                    break;
                }
            }
        }
    }

    /**
     * 处理方法上的注解，派生Spring Controller与Spring定时任务信息
     * 与java-all-call-graph处理method_annotation文件的逻辑保持一致
     *
     * @param className         方法所在类的完整类名
     * @param fullMethod        完整方法（类名+方法名+参数）
     * @param returnType        方法返回类型，包含数组标志
     * @param classJarNum       方法所在的jar文件序号
     * @param argTypes          方法参数类型
     * @param annotationEntries 方法上的注解
     */
    public void handleMethodAnnotation(String className, String fullMethod, String returnType, int classJarNum, Type[] argTypes,
                                       AnnotationEntry[] annotationEntries) {
        if (annotationEntries == null || annotationEntries.length == 0) {
            return;
        }

        String methodName = null;
        String methodHash = null;

        for (AnnotationEntry annotationEntry : annotationEntries) {
            String annotationName = genAnnotationName(annotationEntry);

            if (JavaCG2SpringUtil.checkTaskAnnotation(annotationName)) {
                // 方法上存在@Scheduled注解，记录Spring定时任务信息
                if (methodName == null) {
                    methodName = JavaCG2ClassMethodUtil.getMethodNameFromFull(fullMethod);
                    methodHash = JavaCG2ClassMethodUtil.genMethodHashWithLen(fullMethod, returnType);
                }
                SpringTaskInJava springTaskInJava = new SpringTaskInJava();
                springTaskInJava.setMethodHash(methodHash);
                springTaskInJava.setClassName(className);
                springTaskInJava.setMethodName(methodName);
                springTaskInJava.setFullMethod(fullMethod);
                springTaskInJava.setReturnType(returnType);
                springTaskList.add(springTaskInJava);
            }

            if (!JavaCG2SpringUtil.checkRequestMappingAnnotation(annotationName)) {
                // 当前方法上的注解不是RequestMapping相关注解
                continue;
            }

            List<String> classRequestMappingPathList = classRequestMappingMap.get(className);
            if (classRequestMappingPathList == null) {
                // 当前类与Spring Controller无关
                continue;
            }

            // 处理当前方法注解中代表路径的属性，每个属性单独处理，与java-all-call-graph按行处理method_annotation文件的逻辑保持一致
            List<ElementValuePair> pathPairList = getPathPairList(annotationEntry);
            if (pathPairList.isEmpty()) {
                // 当前方法注解未指定路径属性，等价于只有一个空字符串的方法路径
                handleSpringControllerAnnotation(className, fullMethod, returnType, classJarNum, annotationName, classRequestMappingPathList,
                        Collections.<String>emptyList(), argTypes);
                continue;
            }

            for (ElementValuePair pathPair : pathPairList) {
                List<String> methodPathList = parsePathList(pathPair.getValue());
                handleSpringControllerAnnotation(className, fullMethod, returnType, classJarNum, annotationName, classRequestMappingPathList, methodPathList,
                        argTypes);
            }
        }
    }

    // 将Spring Controller信息保存到列表中
    private void handleSpringControllerAnnotation(String className, String fullMethod, String returnType, int classJarNum, String annotationName,
                                                  List<String> classRequestMappingPathList, List<String> methodPathList, Type[] argTypes) {
        List<String> usedClassRequestMappingPathList = classRequestMappingPathList;
        if (usedClassRequestMappingPathList.isEmpty()) {
            // 假如类的path列表为空，则使用只有一个空字符串的列表
            usedClassRequestMappingPathList = Collections.singletonList("");
        }

        List<String> usedMethodPathList = methodPathList;
        if (usedMethodPathList.isEmpty()) {
            // 假如方法的path列表为空，则使用只有一个空字符串的列表
            usedMethodPathList = Collections.singletonList("");
        }

        // 方法可能用于文件上传/下载标志，同一个方法的取值相同，只判断一次
        int maybeFileUpload = JavaCG2YesNoEnum.NO.getIntValue();
        int maybeFileDownload = JavaCG2YesNoEnum.NO.getIntValue();

        int seq = 0;
        for (String classRequestMappingPath : usedClassRequestMappingPathList) {
            for (String methodPath : usedMethodPathList) {
                SpringControllerInJava springControllerInJava = new SpringControllerInJava();
                springControllerInJava.setMethodHash(JavaCG2ClassMethodUtil.genMethodHashWithLen(fullMethod, returnType));
                springControllerInJava.setClassName(className);
                springControllerInJava.setSeq(seq);
                springControllerInJava.setShowUri(JavaCG2SpringUtil.genSpringControllerShowUri(classRequestMappingPath, methodPath));
                springControllerInJava.setClassPath(classRequestMappingPath);
                springControllerInJava.setMethodPath(methodPath);
                springControllerInJava.setAnnotationName(annotationName);
                springControllerInJava.setJarNum(classJarNum);
                if (seq == 0) {
                    maybeFileUpload = checkMaybeFileUpload(argTypes);
                    maybeFileDownload = checkMaybeFileDownload(argTypes, returnType);
                }
                springControllerInJava.setMaybeFileUpload(maybeFileUpload);
                springControllerInJava.setMaybeFileDownload(maybeFileDownload);
                springControllerInJava.setFullMethod(fullMethod);
                springControllerInJava.setReturnType(returnType);
                springControllerList.add(springControllerInJava);
                seq++;
            }
        }
    }

    // 将派生的Spring Controller信息写入spring_controller文件
    public void writeSpringController2Files(JavaCG2RecordSink springControllerSink) throws IOException {
        if (springControllerList.isEmpty()) {
            return;
        }

        genSimpleClassNameMaps();
        JavaCG2Counter recordIdCounter = new JavaCG2Counter(JavaCG2Constants.RECORD_ID_MIN_BEFORE);
        for (SpringControllerInJava springControllerInJava : springControllerList) {
            springControllerSink.writeRow(
                    String.valueOf(recordIdCounter.addAndGet()),
                    springControllerInJava.getMethodHash(),
                    String.valueOf(springControllerInJava.getSeq()),
                    springControllerInJava.getShowUri(),
                    springControllerInJava.getClassPath(),
                    springControllerInJava.getMethodPath(),
                    springControllerInJava.getAnnotationName(),
                    querySimpleClassName(springControllerInJava.getClassName()),
                    String.valueOf(springControllerInJava.getJarNum()),
                    String.valueOf(springControllerInJava.getMaybeFileUpload()),
                    String.valueOf(springControllerInJava.getMaybeFileDownload()),
                    springControllerInJava.getFullMethod(),
                    springControllerInJava.getReturnType()
            );
        }
    }

    // 将派生的Spring定时任务信息写入spring_task_java文件
    public void writeSpringTask2Files(JavaCG2RecordSink springTaskJavaSink) throws IOException {
        if (springTaskList.isEmpty()) {
            return;
        }

        genSimpleClassNameMaps();
        JavaCG2Counter recordIdCounter = new JavaCG2Counter(JavaCG2Constants.RECORD_ID_MIN_BEFORE);
        for (SpringTaskInJava springTaskInJava : springTaskList) {
            springTaskJavaSink.writeRow(
                    String.valueOf(recordIdCounter.addAndGet()),
                    springTaskInJava.getMethodHash(),
                    // 通过注解定义的定时任务没有对应的Spring Bean名称
                    "",
                    springTaskInJava.getClassName(),
                    springTaskInJava.getMethodName(),
                    JavaCG2Constants.FILE_KEY_SPRING_DEFINE_IN_JAVA,
                    springTaskInJava.getFullMethod(),
                    springTaskInJava.getReturnType(),
                    // 在Java代码中定义时所在的类名
                    springTaskInJava.getClassName()
            );
        }
    }

    // 处理Spring Controller信息时判断方法是否可能用于文件上传，通过方法参数判断
    private int checkMaybeFileUpload(Type[] argTypes) {
        if (argTypes == null) {
            return JavaCG2YesNoEnum.NO.getIntValue();
        }

        for (Type argType : argTypes) {
            String argTypeStr = JavaCG2ClassMethodUtil.removeAllArrayFlag(argType.toString());
            if (CLASS_NAME_MULTI_PART_FILE.equals(argTypeStr) || CLASS_NAME_COMMONS_MULTI_PART_FILE.equals(argTypeStr)) {
                // 方法参数为MultipartFile时，解析阶段无法确认参数是否被使用，记录为不确定
                return JavaCG2YesNoEnum.NOT_SURE.getIntValue();
            }
        }
        return JavaCG2YesNoEnum.NO.getIntValue();
    }

    // 处理Spring Controller信息时判断方法是否可能用于文件下载，通过方法参数与返回类型判断
    private int checkMaybeFileDownload(Type[] argTypes, String returnType) {
        if (argTypes != null) {
            for (Type argType : argTypes) {
                String argTypeStr = JavaCG2ClassMethodUtil.removeAllArrayFlag(argType.toString());
                if (CLASS_NAME_HTTP_SERVLET_RESPONSE.equals(argTypeStr)) {
                    // 方法参数为HttpServletResponse时，解析阶段无法确认参数是否被使用，记录为不确定
                    return JavaCG2YesNoEnum.NOT_SURE.getIntValue();
                }
            }
        }

        // java-all-call-graph还会结合方法返回值的泛型类型进一步判断，解析阶段无法获得，返回类型相关时记录为不确定
        String returnTypeNad = JavaCG2ClassMethodUtil.removeAllArrayFlag(returnType);
        if (CLASS_NAME_HTTP_ENTITY.equals(returnTypeNad) || CLASS_NAME_RESPONSE_ENTITY.equals(returnTypeNad)) {
            return JavaCG2YesNoEnum.NOT_SURE.getIntValue();
        }
        return JavaCG2YesNoEnum.NO.getIntValue();
    }

    // 获取注解中代表路径的属性列表
    private List<ElementValuePair> getPathPairList(AnnotationEntry annotationEntry) {
        List<ElementValuePair> pathPairList = new ArrayList<>(1);
        ElementValuePair[] elementValuePairs = annotationEntry.getElementValuePairs();
        if (elementValuePairs == null) {
            return pathPairList;
        }

        for (ElementValuePair elementValuePair : elementValuePairs) {
            if (JavaCG2SpringUtil.checkRequestMappingPathAttribute(elementValuePair.getNameString())) {
                pathPairList.add(elementValuePair);
            }
        }
        return pathPairList;
    }

    // 从注解属性值中解析路径列表，数组类型的属性值取每个元素，其他类型的属性值作为只有一个元素的列表
    private List<String> parsePathList(ElementValue elementValue) {
        List<String> pathList = new ArrayList<>(1);
        if (elementValue instanceof ArrayElementValue) {
            ArrayElementValue arrayElementValue = (ArrayElementValue) elementValue;
            for (ElementValue elementValueOfArray : arrayElementValue.getElementValuesArray()) {
                pathList.add(elementValueOfArray.toString());
            }
        } else {
            pathList.add(elementValue.toString());
        }
        return pathList;
    }

    // 获取Java风格的注解完整类名
    private String genAnnotationName(AnnotationEntry annotationEntry) {
        return Utility.typeSignatureToString(annotationEntry.getAnnotationType(), false);
    }

    // 生成简单类名与完整类名的对应关系Map
    private void genSimpleClassNameMaps() {
        if (simpleClassNameCountMap != null) {
            return;
        }

        simpleClassNameCountMap = new HashMap<>(JavaCG2Constants.SIZE_100);
        simpleClassNameMap = new HashMap<>(JavaCG2Constants.SIZE_100);
        if (allClassNameSet == null) {
            return;
        }

        for (String className : allClassNameSet) {
            String simpleClassName = JavaCG2ClassMethodUtil.getSimpleNameFromClassName(className);
            Integer count = simpleClassNameCountMap.get(simpleClassName);
            simpleClassNameCountMap.put(simpleClassName, count == null ? 1 : count + 1);
        }
    }

    // 获取唯一类名，简单类名只对应一个完整类名时返回简单类名，否则返回完整类名，与java-all-call-graph的DbOperWrapper.querySimpleClassName方法语义保持一致
    private String querySimpleClassName(String className) {
        String simpleClassName = JavaCG2ClassMethodUtil.getSimpleNameFromClassName(className);
        if (simpleClassNameCountMap == null) {
            return simpleClassName;
        }

        String simpleClassNameInMap = simpleClassNameMap.get(className);
        if (simpleClassNameInMap != null) {
            return simpleClassNameInMap;
        }

        Integer count = simpleClassNameCountMap.get(simpleClassName);
        String uniqueClassName = (count == null || count == 1) ? simpleClassName : className;
        simpleClassNameMap.put(className, uniqueClassName);
        return uniqueClassName;
    }
}
