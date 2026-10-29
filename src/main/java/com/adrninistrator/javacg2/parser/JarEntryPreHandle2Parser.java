package com.adrninistrator.javacg2.parser;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.conf.JavaCG2ConfInfo;
import com.adrninistrator.javacg2.dto.classes.ClassExtendsInfo;
import com.adrninistrator.javacg2.dto.inputoutput.JavaCG2InputAndOutput;
import com.adrninistrator.javacg2.dto.method.MethodArgReturnTypes;
import com.adrninistrator.javacg2.spring.UseSpringBeanByAnnotationHandler;
import com.adrninistrator.javacg2.util.JavaCG2ByteCodeUtil;
import com.adrninistrator.javacg2.util.JavaCG2ClassMethodUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author adrninistrator
 * @date 2022/9/14
 * @description: 解析jar包中的文件，第二次预处理
 */
@Slf4j
public class JarEntryPreHandle2Parser extends AbstractJarEntryParser {

    private final Set<String> classExtendsSet = new HashSet<>(JavaCG2Constants.SIZE_100);

    private final UseSpringBeanByAnnotationHandler useSpringBeanByAnnotationHandler;

    private final boolean parseJarCompatibilityMode;
    private final boolean parseOnlyClassMode;

    @Setter
    private Map<String, Map<MethodArgReturnTypes, Integer>> classExtendsImplMethodWithArgTypesMap;
    @Setter
    private Map<String, Map<MethodArgReturnTypes, Integer>> interfaceMethodWithArgTypesMap;
    @Setter
    private Map<String, String> classAndSuperMap;
    @Setter
    private Map<String, ClassExtendsInfo> classExtendsInfoMap;
    @Setter
    private Map<String, List<String>> childrenClassMap;
    @Setter
    private Set<String> interfaceExtendsSet;
    @Setter
    private Map<String, List<String>> interfaceExtendsInfoMap;
    @Setter
    private Map<String, List<String>> childrenInterfaceMap;
    @Setter
    private Set<String> allClassNameSet;

    public JarEntryPreHandle2Parser(JavaCG2InputAndOutput javaCG2InputAndOutput, boolean onlyOneJar, UseSpringBeanByAnnotationHandler useSpringBeanByAnnotationHandler) {
        super(javaCG2InputAndOutput, onlyOneJar);
        this.useSpringBeanByAnnotationHandler = useSpringBeanByAnnotationHandler;
        JavaCG2ConfInfo javaCG2ConfInfo = javaCG2InputAndOutput.getJavaCG2ConfInfo();
        parseJarCompatibilityMode = javaCG2ConfInfo.isParseJarCompatibilityMode();
        parseOnlyClassMode = javaCG2ConfInfo.isParseOnlyClassMode();
    }

    @Override
    public void init() {
        // 获取涉及继承的类
        for (Map.Entry<String, String> entry : classAndSuperMap.entrySet()) {
            String superClassName = entry.getValue();
            if (JavaCG2ClassMethodUtil.isClassInJdk(superClassName) && !allClassNameSet.contains(superClassName)) {
                // 若父类是JDK中的类，且在指定的jar包中未找到，则不添加
                continue;
            }
            String className = entry.getKey();
            classExtendsSet.add(className);
            classExtendsSet.add(superClassName);
        }
    }

    @Override
    protected boolean handleEntry(ZipInputStream zipInputStream, String jarEntryPath) throws IOException {
        if (parseJarCompatibilityMode || parseOnlyClassMode) {
            // 使用Jar兼容性检查模式，仅解析基础信息时，或仅解析类时，跳过后续处理
            return true;
        }

        // 尝试处理jar包中的class文件
        tryHandleClassEntry(zipInputStream, jarEntryPath);
        // 以上方法返回值不处理
        return true;
    }

    @Override
    protected boolean handleClassEntry(JavaClass javaClass, String jarEntryPath) {
        String className = javaClass.getClassName();
        if (javaCG2ElManager.checkIgnoreParseClass(className)) {
            log.debug("跳过解析类 {}", className);
            return true;
        }

        if (javaClass.isClass()) {
            // 处理类
            // 查找涉及继承的类的信息，需要提前执行，使后续处理方法调用时，classExtendsMethodInfoMap的数据是完整的
            findClassExtendsInfo(javaClass);

            // 记录类中带有Spring相关注解的字段信息（Spring依赖注入字段解析不依赖符号执行，始终执行）
            useSpringBeanByAnnotationHandler.recordClassFieldsWithSpringAnnotation(javaClass);
            return true;
        }

        // 处理接口
        findInterfaceExtendsInfo(javaClass);
        return true;
    }

    // 查找涉及继承的类的信息
    private void findClassExtendsInfo(JavaClass javaClass) {
        String className = javaClass.getClassName();
        if (!classExtendsSet.contains(className) || classExtendsInfoMap.containsKey(className)) {
            // 假如当前类不涉及继承，或当前类已处理过，则不处理
            return;
        }

        String superClassName = javaClass.getSuperclassName();
        List<String> childrenClassList = childrenClassMap.computeIfAbsent(superClassName, k -> new ArrayList<>());
        childrenClassList.add(className);

        classExtendsInfoMap.put(className, new ClassExtendsInfo(javaClass.getAccessFlags(), superClassName));
        // 记录类中可能涉及继承的方法
        Method[] methods = javaClass.getMethods();
        if (ArrayUtils.isNotEmpty(methods)) {
            Map<MethodArgReturnTypes, Integer> methodArgReturnTypesMap = JavaCG2ByteCodeUtil.genExtendsClassMethodWithArgTypes(javaClass.getMethods());
            JavaCG2ClassMethodUtil.copyClassMethodMap(classExtendsImplMethodWithArgTypesMap, className, methodArgReturnTypesMap);
        }
    }

    // 查找涉及继承的接口的信息
    private void findInterfaceExtendsInfo(JavaClass interfaceClass) {
        String interfaceName = interfaceClass.getClassName();
        if (interfaceClass.isAnnotation() ||
                !interfaceExtendsSet.contains(interfaceName) ||
                interfaceExtendsInfoMap.containsKey(interfaceName)) {
            // 假如接口为注解，或当前接口不涉及继承，或当前接口已处理过，则不处理
            return;
        }

        String[] superInterfaceNames = interfaceClass.getInterfaceNames();
        for (String superInterfaceName : superInterfaceNames) {
            List<String> childrenInterfaceList = childrenInterfaceMap.computeIfAbsent(superInterfaceName, k -> new ArrayList<>());
            childrenInterfaceList.add(interfaceName);
        }

        // 记录当前接口的方法信息
        Map<MethodArgReturnTypes, Integer> methodAttributeMap = new HashMap<>();
        for (Method method : interfaceClass.getMethods()) {
            methodAttributeMap.put(new MethodArgReturnTypes(method.getName(), method.getArgumentTypes(), method.getReturnType()), method.getAccessFlags());
        }
        interfaceExtendsInfoMap.put(interfaceName, Arrays.asList(superInterfaceNames));
        interfaceMethodWithArgTypesMap.put(interfaceName, methodAttributeMap);
    }
}
