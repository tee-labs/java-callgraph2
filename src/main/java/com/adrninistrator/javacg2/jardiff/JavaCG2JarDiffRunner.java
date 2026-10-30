package com.adrninistrator.javacg2.jardiff;

import com.adrninistrator.javacg2.common.JavaCG2CommonNameConstants;
import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.dto.jardiff.JavaCG2JarDiffModifiedClassInfo;
import com.adrninistrator.javacg2.dto.jardiff.JavaCG2JarDiffModifiedMethodInfo;
import com.adrninistrator.javacg2.util.JavaCG2ClassMethodUtil;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import com.adrninistrator.javacg2.util.JavaCG2Util;
import com.adrninistrator.javacg2.writer.JavaCG2FileRecordSink;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSink;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 比较两次java-callgraph2解析生成的输出目录（旧、新），获得发生变化的类、方法的独立库API
 * 比对逻辑参考java-all-call-graph的JarDiff功能（AbstractRunnerGenJarDiffCallGraph），不依赖数据库，直接读回两次解析生成的结果文件在内存中比对
 * 比对规则：
 * 1 jar文件按“文件名去掉版本号的前缀+文件名后缀”配对（忽略“-数字及后面的内容”），jar文件HASH相同则认为未变化，整体跳过
 * 2 jar文件HASH不同，或旧目录中不存在对应前缀的jar文件（新增的jar文件）时，进入类级比对
 * 3 类级比对：新旧目录中均存在该类且class文件HASH相同则跳过；否则记录为发生变化的类，并进入方法级比对
 * 4 方法级比对：通过method_info中的方法指令HASH比较；旧方法不存在则为新增的方法，指令HASH不同则为修改的方法；旧目录中存在但新目录中不存在的方法（删除的方法）不会记录
 * 5 方法名为静态代码块（clinit）的方法、方法指令HASH为空的方法（如abstract、native方法），在旧类存在时跳过比较；新增的jar文件中的全部方法均会记录
 * 已知限制：
 * 1 java-all-call-graph对MyBatis Mapper方法使用Mapper XML中SQL语句的HASH代替方法指令HASH进行比较；当前项目不解析MyBatis XML，
 * 因此Mapper接口方法指令未变化但Mapper XML中SQL变化的情况无法检出
 * 2 被删除的jar文件、类、方法不会记录（与java-all-call-graph行为一致）
 * 3 重复同名类的信息记录在dup_class_info、dup_method_info等文件中，不参与比较（与java-all-call-graph行为一致）
 * 当前类的实例在创建后只能执行一次run方法
 * 使用示例：
 *     JavaCG2JarDiffRunner jarDiffRunner = new JavaCG2JarDiffRunner("D:/out/old", "D:/out/new", "D:/out/jar_diff");
 *     jarDiffRunner.setOutputFileExt(".md");
 *     boolean success = jarDiffRunner.run();
 */
@Slf4j
public class JavaCG2JarDiffRunner {

    /*
        jar文件名中版本号部分的匹配模式，与java-all-call-graph的JAR_VERSION_PATTERN一致
        例如“jar-diff-2.0.0.jar”的文件名前缀为“jar-diff”
     */
    private static final Pattern JAR_VERSION_PATTERN = Pattern.compile("-[0-9]");

    // jar_info文件中jar/war文件的类型标记，仅该类型的记录参与比较，与java-all-call-graph行为一致
    private static final String JAR_TYPE_JAR = JavaCG2Constants.FILE_KEY_JAR;

    // 发生变化的类的变更类型，M:class文件HASH发生变化的类，N:新增的类
    private static final String CLASS_CHANGE_TYPE_MODIFIED = "M";
    private static final String CLASS_CHANGE_TYPE_NEW = "N";

    // jar_info文件各列的列数
    private static final int JAR_INFO_COLUMN_NUM = 4;

    // class_info文件各列的列数
    private static final int CLASS_INFO_COLUMN_NUM = 5;

    // method_info文件各列的列数
    private static final int METHOD_INFO_COLUMN_NUM = 8;

    // 发生变化的类的结果文件名
    public static final String FILE_NAME_JAR_DIFF_MODIFIED_CLASSES = "jar_diff_modified_classes";

    // 发生变化的方法的结果文件名
    public static final String FILE_NAME_JAR_DIFF_MODIFIED_METHODS = "jar_diff_modified_methods";

    private final AtomicBoolean runFlag = new AtomicBoolean(false);

    private final String oldOutputDirPath;

    private final String newOutputDirPath;

    private final String diffOutputDirPath;

    // 读回结果文件及生成结果文件使用的后缀名，默认为.txt，需要与生成旧、新输出目录时使用的output.file.ext参数一致
    @Getter
    @Setter
    private String outputFileExt = JavaCG2Constants.EXT_TXT;

    /*
        发生变化的jar文件中发生变化的类信息
        key     新jar文件名称
        value   发生变化的类信息列表
     */
    @Getter
    private final Map<String, List<JavaCG2JarDiffModifiedClassInfo>> jarModifiedClassInfoMap = new TreeMap<>();

    /*
        发生变化的jar文件中发生变化的方法信息
        key     新jar文件名称
        value   发生变化的方法信息列表
     */
    @Getter
    private final Map<String, List<JavaCG2JarDiffModifiedMethodInfo>> jarModifiedMethodInfoMap = new TreeMap<>();

    // 发生变化的jar文件信息列表
    private List<ModifiedJarPair> modifiedJarPairList = new ArrayList<>();

    /**
     * @param oldOutputDirPath  旧版本jar文件对应的解析输出目录
     * @param newOutputDirPath  新版本jar文件对应的解析输出目录
     * @param diffOutputDirPath 比较结果文件输出目录
     */
    public JavaCG2JarDiffRunner(String oldOutputDirPath, String newOutputDirPath, String diffOutputDirPath) {
        this.oldOutputDirPath = oldOutputDirPath;
        this.newOutputDirPath = newOutputDirPath;
        this.diffOutputDirPath = diffOutputDirPath;
    }

    /**
     * 执行JarDiff比较，始终生成比较结果文件（未发现变化的类、方法时生成内容为空的文件）
     *
     * @return true: 执行成功 false: 执行失败
     */
    public boolean run() {
        if (!runFlag.compareAndSet(false, true)) {
            log.error("当前类的实例在创建后只能使用一次，假如需要再次执行，请创建新的实例 {}", this.getClass().getSimpleName());
            return false;
        }

        long startTime = System.currentTimeMillis();
        try {
            if (!checkArgs()) {
                return false;
            }

            // 读回新旧目录的jar_info文件
            List<JarInfoLine> jarInfoListOld = readJarInfo(oldOutputDirPath);
            if (jarInfoListOld == null) {
                return false;
            }
            List<JarInfoLine> jarInfoListNew = readJarInfo(newOutputDirPath);
            if (jarInfoListNew == null) {
                return false;
            }

            // 读回class_info文件，key1为jar文件序号，key2为完整类名
            Map<Integer, Map<String, ClassInfoLine>> oldClassInfoMap = readClassInfo(oldOutputDirPath);
            if (oldClassInfoMap == null) {
                return false;
            }
            Map<Integer, Map<String, ClassInfoLine>> newClassInfoMap = readClassInfo(newOutputDirPath);
            if (newClassInfoMap == null) {
                return false;
            }

            // 读回旧目录的method_info文件，key为完整方法+冒号+返回类型，value为方法指令HASH
            Map<String, String> oldMethodHashMap = readOldMethodHashMap(oldOutputDirPath);
            if (oldMethodHashMap == null) {
                return false;
            }

            // 读回新目录的method_info文件，key1为jar文件序号，key2为完整类名
            Map<Integer, Map<String, List<MethodInfoLine>>> newMethodInfoMap = readNewMethodInfo(newOutputDirPath);
            if (newMethodInfoMap == null) {
                return false;
            }

            // 对新旧目录的jar文件进行配对
            modifiedJarPairList = chooseModifiedJarPairList(jarInfoListOld, jarInfoListNew);
            if (modifiedJarPairList.isEmpty()) {
                log.info("未发现发生变化的jar文件");
            }

            // 对发生变化的jar文件中的类、方法进行比较
            for (ModifiedJarPair modifiedJarPair : modifiedJarPairList) {
                compareModifiedJar(modifiedJarPair, oldClassInfoMap, newClassInfoMap, oldMethodHashMap, newMethodInfoMap);
            }

            // 对比较结果排序
            sortResult();

            // 写出比较结果文件
            return writeResultFiles();
        } catch (Exception e) {
            log.error("JarDiff比较出现异常 ", e);
            return false;
        } finally {
            log.info("JarDiff比较执行完毕 耗时: {} 秒", JavaCG2Util.getSpendSeconds(startTime));
        }
    }

    // 检查参数
    private boolean checkArgs() {
        if (StringUtils.isBlank(outputFileExt) || !outputFileExt.startsWith(JavaCG2Constants.FLAG_DOT)) {
            log.error("结果文件后缀名需要以\"{}\"开头 {}", JavaCG2Constants.FLAG_DOT, outputFileExt);
            return false;
        }

        if (!JavaCG2FileUtil.isDirectoryExists(oldOutputDirPath, false)) {
            log.error("旧版本jar文件对应的解析输出目录不存在 {}", oldOutputDirPath);
            return false;
        }

        if (!JavaCG2FileUtil.isDirectoryExists(newOutputDirPath, false)) {
            log.error("新版本jar文件对应的解析输出目录不存在 {}", newOutputDirPath);
            return false;
        }

        if (!JavaCG2FileUtil.isDirectoryExists(diffOutputDirPath, true)) {
            log.error("创建比较结果文件输出目录失败 {}", diffOutputDirPath);
            return false;
        }
        return true;
    }

    // 读回jar_info文件
    private List<JarInfoLine> readJarInfo(String dirPath) throws IOException {
        List<JarInfoLine> jarInfoLineList = new ArrayList<>();
        try (BufferedReader br = genBufferedReader4SourceFile(dirPath, JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO.getFileName())) {
            if (br == null) {
                return null;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < JAR_INFO_COLUMN_NUM) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO.getFileName(), line);
                    continue;
                }
                jarInfoLineList.add(new JarInfoLine(columns[0], Integer.parseInt(columns[1]), columns[2]));
            }
        }
        // 按jar文件序号排序，保证处理顺序稳定
        jarInfoLineList.sort(Comparator.comparingInt(jarInfoLine -> jarInfoLine.jarNum));
        return jarInfoLineList;
    }

    // 读回class_info文件
    private Map<Integer, Map<String, ClassInfoLine>> readClassInfo(String dirPath) throws IOException {
        Map<Integer, Map<String, ClassInfoLine>> classInfoMap = new HashMap<>();
        try (BufferedReader br = genBufferedReader4SourceFile(dirPath, JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO.getFileName())) {
            if (br == null) {
                return null;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < CLASS_INFO_COLUMN_NUM) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO.getFileName(), line);
                    continue;
                }
                ClassInfoLine classInfoLine = new ClassInfoLine(columns[0], columns[2], Integer.parseInt(columns[3]), columns[4]);
                Map<String, ClassInfoLine> jarClassInfoMap = classInfoMap.computeIfAbsent(classInfoLine.jarNum, k -> new HashMap<>());
                jarClassInfoMap.put(classInfoLine.className, classInfoLine);
            }
        }
        return classInfoMap;
    }

    // 读回旧目录的method_info文件，生成完整方法+冒号+返回类型对应方法指令HASH的Map
    private Map<String, String> readOldMethodHashMap(String dirPath) throws IOException {
        Map<String, String> methodHashMap = new HashMap<>();
        try (BufferedReader br = genBufferedReader4SourceFile(dirPath, JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_INFO.getFileName())) {
            if (br == null) {
                return null;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < METHOD_INFO_COLUMN_NUM) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_INFO.getFileName(), line);
                    continue;
                }
                methodHashMap.put(genMethodHashKey(columns[0], columns[2]), columns[6]);
            }
        }
        return methodHashMap;
    }

    // 读回新目录的method_info文件，生成jar文件序号、完整类名对应方法列表的Map
    private Map<Integer, Map<String, List<MethodInfoLine>>> readNewMethodInfo(String dirPath) throws IOException {
        Map<Integer, Map<String, List<MethodInfoLine>>> methodInfoMap = new HashMap<>();
        try (BufferedReader br = genBufferedReader4SourceFile(dirPath, JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_INFO.getFileName())) {
            if (br == null) {
                return null;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                if (columns.length < METHOD_INFO_COLUMN_NUM) {
                    log.warn("结果文件中一行的列数小于预期 {} {}", JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_INFO.getFileName(), line);
                    continue;
                }
                MethodInfoLine methodInfoLine = new MethodInfoLine(columns[0], columns[2], columns[6], Integer.parseInt(columns[7]));
                String className = JavaCG2ClassMethodUtil.getClassNameFromMethod(methodInfoLine.fullMethod);
                Map<String, List<MethodInfoLine>> classMethodInfoMap = methodInfoMap.computeIfAbsent(methodInfoLine.jarNum, k -> new HashMap<>());
                List<MethodInfoLine> methodInfoLineList = classMethodInfoMap.computeIfAbsent(className, k -> new ArrayList<>());
                methodInfoLineList.add(methodInfoLine);
            }
        }
        return methodInfoMap;
    }

    // 对新旧目录的jar文件进行配对，获得发生变化的jar文件列表
    private List<ModifiedJarPair> chooseModifiedJarPairList(List<JarInfoLine> jarInfoListOld, List<JarInfoLine> jarInfoListNew) {
        List<ModifiedJarPair> modifiedJarPairList = new ArrayList<>();
        for (JarInfoLine jarInfoNew : jarInfoListNew) {
            if (!JAR_TYPE_JAR.equals(jarInfoNew.jarType)) {
                // 仅比较类型为jar/war文件的记录
                continue;
            }
            log.info("处理新的jar文件 {}", jarInfoNew.jarFullPath);
            String jarFileNameNew = JavaCG2FileUtil.getFileNameFromPath(jarInfoNew.jarFullPath);
            String jarFileNameHeadNew = getJarFileNameHead(jarFileNameNew);
            String jarFileNameExtNew = JavaCG2FileUtil.getFileExt(jarFileNameNew);
            String jarFileHashNew = genJarFileHash(jarInfoNew.jarFullPath);
            boolean findOldJar = false;
            for (JarInfoLine jarInfoOld : jarInfoListOld) {
                if (!JAR_TYPE_JAR.equals(jarInfoOld.jarType)) {
                    continue;
                }
                String jarFileNameOld = JavaCG2FileUtil.getFileNameFromPath(jarInfoOld.jarFullPath);
                if (!jarFileNameHeadNew.equals(getJarFileNameHead(jarFileNameOld)) || !jarFileNameExtNew.equals(JavaCG2FileUtil.getFileExt(jarFileNameOld))) {
                    continue;
                }
                // 找到文件名前缀与后缀均相同的旧jar文件，比较jar文件HASH
                String jarFileHashOld = genJarFileHash(jarInfoOld.jarFullPath);
                if (jarFileHashNew != null && jarFileHashOld != null && jarFileHashNew.equals(jarFileHashOld)) {
                    log.info("找到对应的旧jar文件，内容未发生变化 {} {}", jarFileNameNew, jarFileNameOld);
                    findOldJar = true;
                    // 继续查找，与java-all-call-graph行为一致，存在多个同名前缀的旧jar文件时，以内容发生变化的为准
                    continue;
                }
                log.info("找到对应的旧jar文件，内容发生变化 {} {}", jarFileNameNew, jarFileNameOld);
                modifiedJarPairList.add(new ModifiedJarPair(jarInfoNew, jarInfoOld, jarFileNameHeadNew, jarFileNameNew, jarFileNameOld));
                findOldJar = true;
                break;
            }
            if (!findOldJar) {
                // 新jar文件未找到对应的旧jar文件，作为新增的jar文件处理，其中的全部类、方法均会记录
                log.info("未找到对应的旧jar文件，作为新增的jar文件处理 {}", jarFileNameNew);
                modifiedJarPairList.add(new ModifiedJarPair(jarInfoNew, null, jarFileNameHeadNew, jarFileNameNew, ""));
            }
        }
        return modifiedJarPairList;
    }

    // 对发生变化的jar文件中的类、方法进行比较
    private void compareModifiedJar(ModifiedJarPair modifiedJarPair, Map<Integer, Map<String, ClassInfoLine>> oldClassInfoMap,
                                    Map<Integer, Map<String, ClassInfoLine>> newClassInfoMap, Map<String, String> oldMethodHashMap,
                                    Map<Integer, Map<String, List<MethodInfoLine>>> newMethodInfoMap) {
        boolean oldJarExists = modifiedJarPair.jarInfoOld != null;

        Map<String, ClassInfoLine> oldJarClassInfoMap = oldJarExists ?
                oldClassInfoMap.getOrDefault(modifiedJarPair.jarInfoOld.jarNum, Collections.<String, ClassInfoLine>emptyMap()) :
                Collections.<String, ClassInfoLine>emptyMap();
        Map<String, ClassInfoLine> newJarClassInfoMap = newClassInfoMap.getOrDefault(modifiedJarPair.jarInfoNew.jarNum,
                Collections.<String, ClassInfoLine>emptyMap());
        if (newJarClassInfoMap.isEmpty()) {
            log.info("未找到jar文件对应的class {}", modifiedJarPair.jarFileNameNew);
            return;
        }

        Map<String, List<MethodInfoLine>> newJarMethodInfoMap = newMethodInfoMap.getOrDefault(modifiedJarPair.jarInfoNew.jarNum,
                Collections.<String, List<MethodInfoLine>>emptyMap());

        List<JavaCG2JarDiffModifiedClassInfo> modifiedClassInfoList = new ArrayList<>();
        List<JavaCG2JarDiffModifiedMethodInfo> modifiedMethodInfoList = new ArrayList<>();

        List<String> classNameList = new ArrayList<>(newJarClassInfoMap.keySet());
        Collections.sort(classNameList);
        for (String className : classNameList) {
            ClassInfoLine classInfoNew = newJarClassInfoMap.get(className);
            ClassInfoLine classInfoOld = oldJarClassInfoMap.get(className);
            boolean oldClassExists = classInfoOld != null;
            if (oldClassExists && classInfoOld.classFileHash.equals(classInfoNew.classFileHash)) {
                // 旧的class文件HASH相同，跳过当前类
                log.debug("class文件HASH没有变化，跳过 {} {}", modifiedJarPair.jarFileNameNew, className);
                continue;
            }

            // class文件有变化，或旧class文件不存在，记录发生变化的类
            log.debug("class文件HASH发生变化，或不存在旧的class {} {}", modifiedJarPair.jarFileNameNew, className);
            modifiedClassInfoList.add(new JavaCG2JarDiffModifiedClassInfo(className, oldClassExists, classInfoNew.classPathInJar, classInfoNew.jarNum,
                    oldClassExists ? classInfoOld.classPathInJar : null, oldClassExists ? classInfoOld.jarNum : null));

            // 方法级比对
            List<MethodInfoLine> methodInfoLineList = newJarMethodInfoMap.get(className);
            if (JavaCG2Util.isCollectionEmpty(methodInfoLineList)) {
                log.debug("未找到class对应的method {} {}", modifiedJarPair.jarFileNameNew, className);
                continue;
            }

            List<MethodInfoLine> sortedMethodInfoLineList = new ArrayList<>(methodInfoLineList);
            sortedMethodInfoLineList.sort(Comparator.comparing(methodInfoLine -> methodInfoLine.fullMethod));
            for (MethodInfoLine methodInfoNew : sortedMethodInfoLineList) {
                boolean oldMethodExists = false;
                if (oldClassExists) {
                    String methodName = StringUtils.substringBetween(methodInfoNew.fullMethod, JavaCG2Constants.FLAG_COLON, JavaCG2Constants.FLAG_LEFT_BRACKET);
                    if (JavaCG2CommonNameConstants.METHOD_NAME_CLINIT.equals(methodName) || StringUtils.isBlank(methodInfoNew.methodInstructionsHash)) {
                        // 方法名为静态代码块（不会被直接调用），或方法指令HASH为空（如abstract、native方法），跳过当前方法
                        log.debug("跳过当前方法的处理 {} {}", modifiedJarPair.jarFileNameNew, methodInfoNew.fullMethod);
                        continue;
                    }

                    String oldMethodHash = oldMethodHashMap.get(genMethodHashKey(methodInfoNew.fullMethod, methodInfoNew.returnType));
                    if (oldMethodHash == null) {
                        // 不存在旧的方法，为新增的方法
                        log.debug("不存在旧的方法 {} {}", modifiedJarPair.jarFileNameNew, methodInfoNew.fullMethod);
                    } else if (!oldMethodHash.equals(methodInfoNew.methodInstructionsHash)) {
                        // 存在旧的方法，方法指令HASH发生变化，为修改的方法
                        log.debug("方法指令HASH发生变化，存在旧的方法 {} {}", modifiedJarPair.jarFileNameNew, methodInfoNew.fullMethod);
                        oldMethodExists = true;
                    } else {
                        // 存在旧的方法，方法指令HASH未发生变化，跳过当前方法
                        log.debug("方法指令HASH未发生变化，存在旧的方法 {} {}", modifiedJarPair.jarFileNameNew, methodInfoNew.fullMethod);
                        continue;
                    }
                }
                // 新增的jar文件中的方法，或方法指令有变化、旧方法不存在的方法，记录对应方法
                modifiedMethodInfoList.add(new JavaCG2JarDiffModifiedMethodInfo(methodInfoNew.fullMethod, methodInfoNew.returnType, oldMethodExists));
            }
        }

        if (modifiedClassInfoList.isEmpty() && modifiedMethodInfoList.isEmpty()) {
            return;
        }
        jarModifiedClassInfoMap.put(modifiedJarPair.jarFileNameNew, modifiedClassInfoList);
        jarModifiedMethodInfoMap.put(modifiedJarPair.jarFileNameNew, modifiedMethodInfoList);
    }

    // 对比较结果排序，保证输出稳定
    private void sortResult() {
        for (List<JavaCG2JarDiffModifiedClassInfo> modifiedClassInfoList : jarModifiedClassInfoMap.values()) {
            modifiedClassInfoList.sort(Comparator.comparing(JavaCG2JarDiffModifiedClassInfo::getClassName));
        }
        for (List<JavaCG2JarDiffModifiedMethodInfo> modifiedMethodInfoList : jarModifiedMethodInfoMap.values()) {
            modifiedMethodInfoList.sort(Comparator.comparing(JavaCG2JarDiffModifiedMethodInfo::getFullMethod));
        }
    }

    // 写出比较结果文件，未发现变化的类、方法时生成内容为空的文件
    private boolean writeResultFiles() throws IOException {
        int modifiedClassCount = 0;
        int modifiedMethodCount = 0;
        int newMethodCount = 0;

        String diffOutputDirPathWithSeparator = JavaCG2FileUtil.addSeparator4FilePath(diffOutputDirPath);
        String modifiedClassesFilePath = JavaCG2FileUtil.genFilePath(diffOutputDirPathWithSeparator, FILE_NAME_JAR_DIFF_MODIFIED_CLASSES, outputFileExt);
        String modifiedMethodsFilePath = JavaCG2FileUtil.genFilePath(diffOutputDirPathWithSeparator, FILE_NAME_JAR_DIFF_MODIFIED_METHODS, outputFileExt);
        try (JavaCG2RecordSink modifiedClassesSink = new JavaCG2FileRecordSink(JavaCG2FileUtil.genBufferedWriter(modifiedClassesFilePath));
             JavaCG2RecordSink modifiedMethodsSink = new JavaCG2FileRecordSink(JavaCG2FileUtil.genBufferedWriter(modifiedMethodsFilePath))
        ) {
            // 写出发生变化的类
            for (ModifiedJarPair modifiedJarPair : modifiedJarPairList) {
                List<JavaCG2JarDiffModifiedClassInfo> modifiedClassInfoList = jarModifiedClassInfoMap.get(modifiedJarPair.jarFileNameNew);
                if (modifiedClassInfoList == null) {
                    continue;
                }
                for (JavaCG2JarDiffModifiedClassInfo modifiedClassInfo : modifiedClassInfoList) {
                    modifiedClassesSink.writeRow(
                            modifiedJarPair.jarFileNameHead,
                            modifiedJarPair.jarFileNameNew,
                            modifiedJarPair.jarFileNameOld,
                            modifiedClassInfo.getClassName(),
                            modifiedClassInfo.isOldClassExists() ? CLASS_CHANGE_TYPE_MODIFIED : CLASS_CHANGE_TYPE_NEW,
                            modifiedClassInfo.getNewClassPathInJar(),
                            String.valueOf(modifiedClassInfo.getNewJarNum()),
                            StringUtils.defaultString(modifiedClassInfo.getOldClassPathInJar()),
                            modifiedClassInfo.getOldJarNum() == null ? "" : String.valueOf(modifiedClassInfo.getOldJarNum())
                    );
                    modifiedClassCount++;
                }
            }

            // 写出发生变化的方法
            for (ModifiedJarPair modifiedJarPair : modifiedJarPairList) {
                List<JavaCG2JarDiffModifiedMethodInfo> modifiedMethodInfoList = jarModifiedMethodInfoMap.get(modifiedJarPair.jarFileNameNew);
                if (modifiedMethodInfoList == null) {
                    continue;
                }
                for (JavaCG2JarDiffModifiedMethodInfo modifiedMethodInfo : modifiedMethodInfoList) {
                    modifiedMethodsSink.writeRow(
                            modifiedJarPair.jarFileNameHead,
                            modifiedJarPair.jarFileNameNew,
                            modifiedJarPair.jarFileNameOld,
                            modifiedMethodInfo.getFullMethod(),
                            modifiedMethodInfo.getMethodReturnType(),
                            String.valueOf(modifiedMethodInfo.isOldMethodExists())
                    );
                    modifiedMethodCount++;
                    if (!modifiedMethodInfo.isOldMethodExists()) {
                        newMethodCount++;
                    }
                }
            }
        }

        log.info("JarDiff比较完成，发生变化的jar文件数量: {} 发生变化的类数量: {} 发生变化的方法数量: {}（其中新增的方法数量: {}）",
                modifiedJarPairList.size(), modifiedClassCount, modifiedMethodCount, newMethodCount);
        return true;
    }

    // 生成读回结果文件的BufferedReader对象，结果文件不存在时返回null
    private BufferedReader genBufferedReader4SourceFile(String dirPath, String fileName) throws IOException {
        String filePath = JavaCG2FileUtil.genFilePath(JavaCG2FileUtil.addSeparator4FilePath(dirPath), fileName, outputFileExt);
        if (!new File(filePath).exists()) {
            log.error("JarDiff需要读取的结果文件不存在 {} {}", fileName, filePath);
            return null;
        }
        return JavaCG2FileUtil.genBufferedReader(filePath);
    }

    // 获取jar文件名不包含后缀及版本号的内容，与java-all-call-graph的getJarFileHead方法一致
    private String getJarFileNameHead(String jarFileName) {
        String fileNameWithOutExt = jarFileName;
        int lastDotIndex = jarFileName.lastIndexOf(JavaCG2Constants.FLAG_DOT);
        if (lastDotIndex != -1) {
            fileNameWithOutExt = jarFileName.substring(0, lastDotIndex);
        }

        Matcher matcher = JAR_VERSION_PATTERN.matcher(fileNameWithOutExt);
        if (!matcher.find()) {
            // 未找到版本号
            return fileNameWithOutExt;
        }
        return fileNameWithOutExt.substring(0, matcher.start());
    }

    // 计算jar文件的MD5，文件不存在或计算失败时返回null
    private String genJarFileHash(String jarFullPath) {
        File jarFile = new File(jarFullPath);
        if (!jarFile.isFile()) {
            log.warn("jar文件不存在，无法计算文件HASH，将其视为内容发生变化的jar文件 {}", jarFullPath);
            return null;
        }

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(jarFile))) {
            return DigestUtils.md5Hex(inputStream);
        } catch (IOException e) {
            log.error("计算jar文件HASH出现异常 {}", jarFullPath, e);
            return null;
        }
    }

    // 生成比较方法时使用的方法HASH的key，完整方法+冒号+返回类型
    private String genMethodHashKey(String fullMethod, String returnType) {
        return fullMethod + JavaCG2Constants.FLAG_COLON + returnType;
    }

    // jar_info文件中的一行记录
    private static class JarInfoLine {

        // jar文件类型
        private final String jarType;

        // jar文件序号
        private final int jarNum;

        // jar文件完整路径
        private final String jarFullPath;

        private JarInfoLine(String jarType, int jarNum, String jarFullPath) {
            this.jarType = jarType;
            this.jarNum = jarNum;
            this.jarFullPath = jarFullPath;
        }
    }

    // class_info文件中的一行记录
    private static class ClassInfoLine {

        // 完整类名
        private final String className;

        // class文件HASH
        private final String classFileHash;

        // 类所在的jar文件序号
        private final int jarNum;

        // class文件在jar文件中的路径
        private final String classPathInJar;

        private ClassInfoLine(String className, String classFileHash, int jarNum, String classPathInJar) {
            this.className = className;
            this.classFileHash = classFileHash;
            this.jarNum = jarNum;
            this.classPathInJar = classPathInJar;
        }
    }

    // method_info文件中的一行记录
    private static class MethodInfoLine {

        // 完整方法
        private final String fullMethod;

        // 方法返回类型
        private final String returnType;

        // 方法指令HASH
        private final String methodInstructionsHash;

        // 方法所在的jar文件序号
        private final int jarNum;

        private MethodInfoLine(String fullMethod, String returnType, String methodInstructionsHash, int jarNum) {
            this.fullMethod = fullMethod;
            this.returnType = returnType;
            this.methodInstructionsHash = methodInstructionsHash;
            this.jarNum = jarNum;
        }
    }

    // 发生变化的jar文件信息
    private static class ModifiedJarPair {

        // 新jar文件信息
        private final JarInfoLine jarInfoNew;

        // 旧jar文件信息，为null时代表新增的jar文件
        private final JarInfoLine jarInfoOld;

        // jar文件名称前缀
        private final String jarFileNameHead;

        // 新jar文件名称
        private final String jarFileNameNew;

        // 旧jar文件名称，新增的jar文件为空字符串
        private final String jarFileNameOld;

        private ModifiedJarPair(JarInfoLine jarInfoNew, JarInfoLine jarInfoOld, String jarFileNameHead, String jarFileNameNew, String jarFileNameOld) {
            this.jarInfoNew = jarInfoNew;
            this.jarInfoOld = jarInfoOld;
            this.jarFileNameHead = jarFileNameHead;
            this.jarFileNameNew = jarFileNameNew;
            this.jarFileNameOld = jarFileNameOld;
        }
    }
}
