package test.spring;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import test.base.TestBase;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 测试从类及方法的注解中派生Spring Controller与Spring定时任务信息的内置能力
 * javacg2及测试环境没有Spring依赖，因此通过编译后的伪Spring注解（org.springframework等包名下的同名注解定义，无任何依赖）标注的夹具类
 * 现场打成jar文件作为解析输入，验证spring_controller、spring_task_java文件的内容
 */
public class TestSpringControllerTask extends TestBase {

    private static final String OUT_ROOT = "out/test_spring_controller_task";

    // 现场构造的jar文件路径
    private static final String TEST_JAR_PATH = OUT_ROOT + "/spring-controller-task-test.jar";

    // 夹具类所在包名
    private static final String FIXTURE_PACKAGE_PATH = "fixtures/spring/controller";

    private static final String ANNOTATION_GET_MAPPING = "org.springframework.web.bind.annotation.GetMapping";
    private static final String ANNOTATION_POST_MAPPING = "org.springframework.web.bind.annotation.PostMapping";
    private static final String ANNOTATION_PUT_MAPPING = "org.springframework.web.bind.annotation.PutMapping";
    private static final String ANNOTATION_REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping";

    @Before
    public void init() throws IOException {
        // 删除上次生成的目录
        FileUtils.deleteDirectory(new File(OUT_ROOT));
        Files.createDirectories(new File(OUT_ROOT).toPath());
    }

    /**
     * 验证通过类及方法注解派生的Spring Controller信息被写入spring_controller文件，通过@Scheduled注解定义的定时任务被写入spring_task_java文件
     * 列数及各列含义与java-all-call-graph的jacg_spring_controller表、jacg_spring_task表在Java代码中定义时的记录保持一致
     */
    @Test
    public void testSpringControllerAndTask() throws IOException {
        // 将夹具类及伪Spring注解类打成jar文件
        genFixtureJar();

        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(TEST_JAR_PATH);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, OUT_ROOT);
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue(javaCG2Entry.run());

        checkSpringController(javaCG2Entry);
        checkSpringTask(javaCG2Entry);
    }

    // 验证spring_controller文件
    private void checkSpringController(JavaCG2Entry javaCG2Entry) {
        List<String> rowList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_CONTROLLER);
        Map<String, String[]> rowIndex = genRowIndex(rowList, 13);

        // 记录id应为从1开始的自然数
        Set<String> recordIdSet = new HashSet<>();
        for (String row : rowList) {
            recordIdSet.add(row.split("\t", -1)[0]);
        }
        Assert.assertEquals("spring_controller文件的记录id应为从1开始的自然数", rowList.size(), recordIdSet.size());
        for (int i = 1; i <= rowList.size(); i++) {
            Assert.assertTrue("spring_controller文件缺少记录id " + i, recordIdSet.contains(String.valueOf(i)));
        }

        // 类路径+方法路径的普通拼接
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringController:get1()",
                ANNOTATION_GET_MAPPING, "0", "/test/controller/get1", "/test/controller", "/get1",
                "TestSpringController", "1", "0", "0", "java.lang.String");

        // 方法路径为数组时生成多条记录，seq递增
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringController:post1()",
                ANNOTATION_POST_MAPPING, "0", "/test/controller/post1", "/test/controller", "/post1",
                "TestSpringController", "1", "0", "0", "void");
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringController:post1()",
                ANNOTATION_POST_MAPPING, "1", "/test/controller/post2", "/test/controller", "/post2",
                "TestSpringController", "1", "0", "0", "void");

        // 方法路径使用value属性指定
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringController:req1()",
                ANNOTATION_REQUEST_MAPPING, "0", "/test/controller/req1", "/test/controller", "/req1",
                "TestSpringController", "1", "0", "0", "java.lang.String");

        // 方法注解的非路径属性不参与URI拼接
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringController:req2()",
                ANNOTATION_REQUEST_MAPPING, "0", "/test/controller/req2", "/test/controller", "/req2",
                "TestSpringController", "1", "0", "0", "java.lang.String");

        // 方法上未指定路径属性时，方法路径记录为空字符串，URI为类路径
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringController:put1()",
                ANNOTATION_PUT_MAPPING, "0", "/test/controller", "/test/controller", "",
                "TestSpringController", "1", "0", "0", "void");

        // 同一方法上存在多个RequestMapping相关注解时，每个注解各自生成记录
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringController:multiAnnotationMethod()",
                ANNOTATION_GET_MAPPING, "0", "/test/controller/multi-get", "/test/controller", "/multi-get",
                "TestSpringController", "1", "0", "0", "void");
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringController:multiAnnotationMethod()",
                ANNOTATION_POST_MAPPING, "0", "/test/controller/multi-post", "/test/controller", "/multi-post",
                "TestSpringController", "1", "0", "0", "void");

        // 方法参数为MultipartFile，可能用于文件上传
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringController:upload(org.springframework.web.multipart.MultipartFile)",
                ANNOTATION_POST_MAPPING, "0", "/test/controller/upload", "/test/controller", "/upload",
                "TestSpringController", "1", "2", "0", "void");

        // 类上没有@RequestMapping类注解时，类路径记录为空字符串
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringControllerNoClassPath:noClassPath()",
                ANNOTATION_GET_MAPPING, "0", "/no-class-path", "", "/no-class-path",
                "TestSpringControllerNoClassPath", "1", "0", "0", "java.lang.String");

        // 类路径为数组时生成多条记录，以/结尾的类路径在URI中规范化
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringControllerMultiClassPath:m()",
                ANNOTATION_GET_MAPPING, "0", "/mc1/m", "/mc1", "/m",
                "TestSpringControllerMultiClassPath", "1", "0", "0", "java.lang.String");
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringControllerMultiClassPath:m()",
                ANNOTATION_GET_MAPPING, "1", "/mc2/m", "mc2/", "/m",
                "TestSpringControllerMultiClassPath", "1", "0", "0", "java.lang.String");

        // 方法路径使用path属性指定
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringControllerPathAttribute:pa()",
                ANNOTATION_GET_MAPPING, "0", "/path/attr/pa1", "/path/attr", "/pa1",
                "TestSpringControllerPathAttribute", "1", "0", "0", "java.lang.String");
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringControllerPathAttribute:pa()",
                ANNOTATION_GET_MAPPING, "1", "/path/attr/pa2", "/path/attr", "/pa2",
                "TestSpringControllerPathAttribute", "1", "0", "0", "java.lang.String");

        // 方法参数为HttpServletResponse，可能用于文件下载
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringControllerDownload:download(javax.servlet.http.HttpServletResponse)",
                ANNOTATION_GET_MAPPING, "0", "/download/dl", "/download", "/dl",
                "TestSpringControllerDownload", "1", "0", "2", "void");

        // 方法返回类型为ResponseEntity，可能用于文件下载
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringControllerResponseEntity:entity()",
                ANNOTATION_GET_MAPPING, "0", "/entity/entity", "/entity", "/entity",
                "TestSpringControllerResponseEntity", "1", "0", "2", "org.springframework.http.ResponseEntity");

        // 类上只有@RequestMapping注解而没有Controller相关注解时，与java-all-call-graph一致，该类仍被视为Spring MVC相关的类，生成记录
        checkControllerRow(rowIndex, "fixtures.spring.controller.TestSpringControllerOnlyRequestMapping:getOnlyRm()",
                ANNOTATION_GET_MAPPING, "0", "/only/request-mapping/get-only-rm", "/only/request-mapping", "/get-only-rm",
                "TestSpringControllerOnlyRequestMapping", "1", "0", "0", "java.lang.String");

        Assert.assertEquals("spring_controller文件的记录数量", 17, rowList.size());

        // 没有任何Spring MVC注解的类及方法不应生成记录
        for (String row : rowList) {
            Assert.assertFalse("没有任何Spring MVC注解的类不应生成记录 " + row, row.contains("TestSpringTask"));
            Assert.assertFalse("没有Spring MVC注解的方法不应生成记录 " + row, row.contains("normalMethod"));
        }
    }

    // 检查spring_controller文件中的一条记录
    private void checkControllerRow(Map<String, String[]> rowIndex, String fullMethod, String annotationName, String seq, String showUri, String classPath,
                                    String methodPath, String simpleClassName, String jarNum, String maybeFileUpload, String maybeFileDownload,
                                    String returnType) {
        String key = genRowKey(fullMethod, annotationName, classPath, methodPath);
        String[] columns = rowIndex.get(key);
        Assert.assertNotNull("spring_controller文件中缺少记录 " + key, columns);
        Assert.assertEquals("方法hash+字节数", genExpectedMethodHash(fullMethod, returnType), columns[1]);
        Assert.assertEquals("记录seq " + key, seq, columns[2]);
        Assert.assertEquals("用于显示的URI " + key, showUri, columns[3]);
        Assert.assertEquals("类上的注解path属性原始值 " + key, classPath, columns[4]);
        Assert.assertEquals("方法上的注解path属性原始值 " + key, methodPath, columns[5]);
        Assert.assertEquals("注解类名 " + key, annotationName, columns[6]);
        Assert.assertEquals("唯一类名 " + key, simpleClassName, columns[7]);
        Assert.assertEquals("方法所在的jar文件序号 " + key, jarNum, columns[8]);
        Assert.assertEquals("方法可能用于文件上传 " + key, maybeFileUpload, columns[9]);
        Assert.assertEquals("方法可能用于文件下载 " + key, maybeFileDownload, columns[10]);
        Assert.assertEquals("完整方法 " + key, fullMethod, columns[11]);
        Assert.assertEquals("方法返回类型 " + key, returnType, columns[12]);
    }

    // 验证spring_task_java文件
    private void checkSpringTask(JavaCG2Entry javaCG2Entry) {
        List<String> rowList = readFile4Output(javaCG2Entry, JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_TASK_JAVA);
        Assert.assertEquals("spring_task_java文件的记录数量", 2, rowList.size());

        Map<String, String[]> rowIndex = genRowIndex(rowList, 9);

        String className = "fixtures.spring.controller.TestSpringTask";
        // 通过cron表达式定义的定时任务
        checkTaskRow(rowIndex, className, "cronTask", "void");
        // 通过fixedRate定义的定时任务，@Scheduled注解的属性不影响记录
        checkTaskRow(rowIndex, className, "fixedRateTask", "void");

        for (String row : rowList) {
            Assert.assertFalse("没有@Scheduled注解的方法不应生成记录 " + row, row.contains("normalMethod"));
        }
    }

    // 检查spring_task_java文件中的一条记录
    private void checkTaskRow(Map<String, String[]> rowIndex, String className, String methodName, String returnType) {
        String fullMethod = className + ":" + methodName + "()";
        // spring_task_java文件中没有注解类名列，使用"完整方法+两个空字符串"作为key
        String[] columns = rowIndex.get(fullMethod + "\t\t");
        Assert.assertNotNull("spring_task_java文件中缺少记录 " + fullMethod, columns);
        Assert.assertEquals("方法hash+字节数 " + fullMethod, genExpectedMethodHash(fullMethod, returnType), columns[1]);
        Assert.assertEquals("通过注解定义的定时任务没有对应的Spring Bean名称 " + fullMethod, "", columns[2]);
        Assert.assertEquals("完整类名 " + fullMethod, className, columns[3]);
        Assert.assertEquals("方法名 " + fullMethod, methodName, columns[4]);
        Assert.assertEquals("在Java代码中定义的类型 " + fullMethod, "j", columns[5]);
        Assert.assertEquals("完整方法 " + fullMethod, fullMethod, columns[6]);
        Assert.assertEquals("方法返回类型 " + fullMethod, returnType, columns[7]);
        Assert.assertEquals("在Java代码中定义时所在的类名 " + fullMethod, className, columns[8]);
    }

    // 读取生成的结果文件内容
    private List<String> readFile4Output(JavaCG2Entry javaCG2Entry, JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        String filePath = javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo().getMainFilePath(fileTypeEnum);
        Assert.assertTrue("结果文件应存在: " + filePath, new File(filePath).exists());
        return JavaCG2FileUtil.readFile2List(filePath);
    }

    // 以"完整方法+注解类名+类路径+方法路径"为key生成记录索引
    private Map<String, String[]> genRowIndex(List<String> rowList, int expectedColumnNum) {
        Map<String, String[]> rowIndex = new HashMap<>(rowList.size());
        for (String row : rowList) {
            String[] columns = row.split("\t", -1);
            Assert.assertEquals("结果文件的列数", expectedColumnNum, columns.length);
            if (columns.length == 13) {
                rowIndex.put(genRowKey(columns[11], columns[6], columns[4], columns[5]), columns);
            } else {
                rowIndex.put(columns[6] + "\t\t", columns);
            }
        }
        return rowIndex;
    }

    private String genRowKey(String fullMethod, String annotationName, String classPath, String methodPath) {
        return fullMethod + "\t" + annotationName + "\t" + classPath + "\t" + methodPath;
    }

    // 独立计算方法hash+字节数，与java-all-call-graph的JACGUtil.genHashWithLen方法使用相同的公式，避免实现变化导致与下游不一致
    private String genExpectedMethodHash(String fullMethod, String returnType) {
        try {
            String fullMethodWithReturnType = fullMethod + ":" + returnType;
            byte[] md5 = MessageDigest.getInstance("MD5").digest(fullMethodWithReturnType.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return String.format("%s#%03x", Base64.getUrlEncoder().encodeToString(md5), fullMethodWithReturnType.length());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // 将编译后的夹具类及伪Spring注解类打成jar文件
    private void genFixtureJar() throws IOException {
        List<String> entryNameList = new ArrayList<>();
        for (String className : new String[]{
                "TestSpringController",
                "TestSpringControllerNoClassPath",
                "TestSpringControllerOnlyController",
                "TestSpringControllerMultiClassPath",
                "TestSpringControllerPathAttribute",
                "TestSpringControllerDownload",
                "TestSpringControllerResponseEntity",
                "TestSpringControllerOnlyRequestMapping",
                "TestSpringTask"}) {
            entryNameList.add(FIXTURE_PACKAGE_PATH + "/" + className + ".class");
        }
        for (String className : new String[]{
                "org/springframework/stereotype/Controller",
                "org/springframework/web/bind/annotation/RestController",
                "org/springframework/web/bind/annotation/RequestMapping",
                "org/springframework/web/bind/annotation/GetMapping",
                "org/springframework/web/bind/annotation/PostMapping",
                "org/springframework/web/bind/annotation/PutMapping",
                "org/springframework/scheduling/annotation/Scheduled",
                "org/springframework/web/multipart/MultipartFile",
                "org/springframework/http/ResponseEntity",
                "javax/servlet/http/HttpServletResponse"}) {
            entryNameList.add(className + ".class");
        }

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(TEST_JAR_PATH)))) {
            for (String entryName : entryNameList) {
                File classFile = new File("target/test-classes/" + entryName);
                Assert.assertTrue("编译后的class文件应存在 " + JavaCG2FileUtil.getCanonicalPath(classFile), classFile.exists());
                zipOutputStream.putNextEntry(new ZipEntry(entryName));
                zipOutputStream.write(Files.readAllBytes(classFile.toPath()));
                zipOutputStream.closeEntry();
            }
        }
    }
}
