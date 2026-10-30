package com.adrninistrator.javacg2.util;

import com.adrninistrator.javacg2.common.SpringAnnotationConstants;
import org.apache.commons.lang3.StringUtils;

/**
 * @author adrninistrator
 * @date 2025/6/24
 * @description:
 */
public class JavaCG2SpringUtil {

    /**
     * 判断是否为Spring Component相关注解，或@Named注解
     *
     * @param annotationType 注解的原始类型
     * @return
     */
    public static boolean checkComponentAnnotation(String annotationType) {
        for (String annotation : SpringAnnotationConstants.ANNOTATION_TYPE_COMPONENTS) {
            if (annotation.equals(annotationType)) {
                return true;
            }
        }
        // 再判断是否有@Named注解
        return SpringAnnotationConstants.ANNOTATION_TYPE_NAMED.equals(annotationType);
    }

    /**
     * 判断是否为Spring ComponentScan注解
     *
     * @param annotationType 注解的原始类型
     * @return
     */
    public static boolean checkComponentScanAnnotation(String annotationType) {
        return SpringAnnotationConstants.ANNOTATION_TYPE_COMPONENT_SCAN.equals(annotationType);
    }

    /**
     * 判断是否为Configuration注解
     *
     * @param annotationType
     * @return
     */
    public static boolean checkConfigurationAnnotation(String annotationType) {
        return SpringAnnotationConstants.ANNOTATION_TYPE_CONFIGURATION.equals(annotationType);
    }

    /**
     * 判断是否为Bean注解
     *
     * @param annotationType
     * @return
     */
    public static boolean checkBeanAnnotation(String annotationType) {
        return SpringAnnotationConstants.ANNOTATION_TYPE_BEAN.equals(annotationType);
    }

    /**
     * 判断是否为Aspect注解
     *
     * @param annotationType
     * @return
     */
    public static boolean checkAspectAnnotation(String annotationType) {
        return SpringAnnotationConstants.ANNOTATION_TYPE_ASPECT.equals(annotationType);
    }

    /**
     * 判断是否为Order注解
     *
     * @param annotationType
     * @return
     */
    public static boolean checkOrderAnnotation(String annotationType) {
        return SpringAnnotationConstants.ANNOTATION_TYPE_ORDER.equals(annotationType);
    }

    /**
     * 判断是否为Pointcut注解
     *
     * @param annotationType
     * @return
     */
    public static boolean checkPointcutAnnotation(String annotationType) {
        return SpringAnnotationConstants.ANNOTATION_TYPE_POINTCUT.equals(annotationType);
    }

    /**
     * 判断是否为Advice相关注解
     *
     * @param annotationType
     * @return
     */
    public static boolean checkAdviceAnnotation(String annotationType) {
        return StringUtils.equalsAny(annotationType, SpringAnnotationConstants.ANNOTATION_TYPE_ADVICES);
    }

    /**
     * 判断是否为Spring MVC的Controller相关注解
     * 判断规则与java-all-call-graph的JACGSpringUtil.isControllerAnnotation方法保持一致
     *
     * @param annotationName Java风格的注解完整类名
     * @return
     */
    public static boolean checkControllerAnnotation(String annotationName) {
        return StringUtils.equalsAny(annotationName, SpringAnnotationConstants.ANNOTATION_NAME_CONTROLLERS);
    }

    /**
     * 判断是否为Spring MVC的RequestMapping相关注解
     * 判断规则与java-all-call-graph的JACGSpringUtil.isRequestMappingAnnotation方法保持一致
     *
     * @param annotationName Java风格的注解完整类名
     * @return
     */
    public static boolean checkRequestMappingAnnotation(String annotationName) {
        return StringUtils.equalsAny(annotationName, SpringAnnotationConstants.ANNOTATION_NAME_REQUEST_MAPPINGS);
    }

    /**
     * 判断是否为Spring MVC的RequestMapping相关注解中代表路径的属性名称
     * 判断规则与java-all-call-graph的JACGSpringUtil.isRequestMappingPathAttribute方法保持一致
     *
     * @param attributeName 注解属性名称
     * @return
     */
    public static boolean checkRequestMappingPathAttribute(String attributeName) {
        return StringUtils.equalsAny(attributeName, SpringAnnotationConstants.ANNOTATION_ATTRIBUTE_VALUE, SpringAnnotationConstants.ANNOTATION_ATTRIBUTE_PATH);
    }

    /**
     * 判断是否为Spring定时任务的Scheduled注解
     * 判断规则与java-all-call-graph的JACGSpringUtil.isTaskAnnotation方法保持一致
     *
     * @param annotationName Java风格的注解完整类名
     * @return
     */
    public static boolean checkTaskAnnotation(String annotationName) {
        return SpringAnnotationConstants.ANNOTATION_NAME_SCHEDULED.equals(annotationName);
    }

    /**
     * 生成Spring Controller用于显示的URI
     * 格式为"/classPath/methodPath"
     * 生成规则与java-all-call-graph的JACGSpringUtil.genSpringControllerShowUri方法保持一致
     *
     * @param classPath  类上的注解path属性值
     * @param methodPath 方法上的注解path属性值
     * @return
     */
    public static String genSpringControllerShowUri(String classPath, String methodPath) {
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(classPath)) {
            if (!classPath.startsWith("/")) {
                stringBuilder.append("/");
            }
            stringBuilder.append(classPath);
        }

        if (StringUtils.isNotBlank(methodPath)) {
            if (!methodPath.startsWith("/")) {
                stringBuilder.append("/");
            }
            stringBuilder.append(methodPath);
        }
        String uri = stringBuilder.toString();
        // 去掉重复的/
        while (uri.contains("//")) {
            uri = StringUtils.replace(uri, "//", "/");
        }
        // 去掉最后的/
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - "/".length());
        }
        return uri;
    }

    private JavaCG2SpringUtil() {
        throw new IllegalStateException("illegal");
    }
}
