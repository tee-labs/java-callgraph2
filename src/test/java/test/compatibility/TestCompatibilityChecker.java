package test.compatibility;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.compatibility.JavaCG2CompatibilityChecker;
import com.adrninistrator.javacg2.dto.output.JavaCG2OutputInfo;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import com.adrninistrator.javacg2.writer.JavaCG2FileOutputAdapter;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 对Jar兼容性检查处理类的单元级测试
 * 通过构造的结果文件对各类兼容性问题进行验证：被引用类不存在、被调用类/方法不存在、方法可见性与静态性、被使用字段不存在、字段可见性与静态性、重复同名类冲突
 * 可见性、静态性等兼容性问题无法通过正常编译产生，因此通过构造结果文件的方式验证
 */
public class TestCompatibilityChecker {

    private static final String OUT_DIR = "out/test_compatibility_checker";

    // 输出目录中的结果文件使用的后缀名
    private static final String FILE_EXT = ".md";

    @Before
    public void init() throws IOException {
        // 删除上次生成的目录
        FileUtils.deleteDirectory(new File(OUT_DIR));
        Files.createDirectories(new File(OUT_DIR).toPath());
    }

    /**
     * 对Jar兼容性检查处理类进行全面验证
     * 构造包含各类兼容性问题的结果文件，检查生成的报告文件内容
     */
    @Test
    public void testChecker() throws IOException {
        // 构造结果文件
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO,
                // 公开的类，含两个字段、多个方法
                line("a.b.Class1", "33", "hash_class1_primary", "1", "a/b/Class1.class"),
                // 子类，继承a.b.Class1
                line("a.b.Child", "33", "hash_child", "1", "a/b/Child.class"),
                // 被继承的父类与被引用的类需要存在
                line("a.b.Caller", "33", "hash_caller", "1", "a/b/Caller.class"));
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_DUP_CLASS_INFO,
                // 重复同名类，class文件HASH与原始类不同
                line("a.b.Class1", "33", "hash_class1_dup", "2", "a/b/Class1.class"));
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_INFO,
                // public非静态方法
                line("a.b.Class1:publicMethod()I", "1", "int", "0", "J", "0", "", "1"),
                // private方法
                line("a.b.Class1:privateMethod()V", "2", "void", "0", "J", "0", "", "1"),
                // public静态方法
                line("a.b.Class1:staticMethod()V", "9", "void", "0", "J", "0", "", "1"),
                // 仅在原始类中存在的public方法，与重复同名类冲突
                line("a.b.Class1:removedInDup()V", "1", "void", "0", "J", "0", "", "1"),
                // 子类的方法
                line("a.b.Child:childMethod()V", "1", "void", "0", "J", "0", "", "1"));
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_DUP_METHOD_INFO,
                // 与原始类中相同的方法，方法名及参数+返回类型+静态标志一致，不冲突
                line("a.b.Class1:publicMethod()I", "1", "int", "0", "J", "0", "", "2"),
                // 与原始类中相同的静态方法，不冲突
                line("a.b.Class1:staticMethod()V", "9", "void", "0", "J", "0", "", "2"),
                // 仅在重复同名类中存在的public方法，与原始类冲突
                line("a.b.Class1:onlyInDup()V", "1", "void", "0", "J", "0", "", "2"));
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_INFO,
                // public非静态字段
                line("a.b.Class1", "publicField", "int", "0", "J", "public", "0", "0", "0", "0", "0", "0", "1"),
                // public静态字段
                line("a.b.Class1", "publicStaticField", "int", "0", "J", "public", "0", "1", "0", "0", "0", "0", "1"),
                // private字段
                line("a.b.Class1", "privateField", "int", "0", "J", "private", "0", "0", "0", "0", "0", "0", "1"),
                // 仅在原始类中存在的public字段，与重复同名类冲突
                line("a.b.Class1", "removedInDupField", "int", "0", "J", "public", "0", "0", "0", "0", "0", "0", "1"));
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_DUP_FIELD_INFO,
                // 与原始类中相同的字段，不冲突
                line("a.b.Class1", "publicField", "int", "0", "J", "public", "0", "0", "0", "0", "0", "0", "2"),
                // 与原始类中相同的静态字段，不冲突
                line("a.b.Class1", "publicStaticField", "int", "0", "J", "public", "0", "1", "0", "0", "0", "0", "2"),
                // 仅在重复同名类中存在的public字段，与原始类冲突
                line("a.b.Class1", "onlyInDupField", "int", "0", "J", "public", "0", "0", "0", "0", "0", "0", "2"));
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_EXTENDS_IMPL,
                // Child继承Class1
                line("a.b.Child", "33", "e", "a.b.Class1"));
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO,
                line("jar", "1", "D:/tmp/test1.jar", ""),
                line("jar", "2", "D:/tmp/test2.jar", ""));
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_REFERENCE,
                // 引用不存在的类，应记录
                line("a.b.Class1", "a.b.MissingClass", "1"),
                // 引用JDK中的类，不记录
                line("a.b.Class1", "java.lang.String", "1"),
                // 引用存在的类，不记录
                line("a.b.Child", "a.b.Class1", "1"));
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_DUP_CLASS_REFERENCE,
                // 重复同名类引用不存在类，与class_reference中的记录不同类，应记录
                line("a.b.Class1", "a.b.MissingClass2", "2"));
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL,
                // 被调用类不存在，应记录
                line("1", "1", "a.b.Caller:callMissingClass()V", "(SPE)a.b.Missing2:run()V", "10", "void", "0", "t", "void", "", "1", "-", ""),
                // 被调用方法不存在，应记录
                line("2", "1", "a.b.Caller:callMissingMethod()V", "(VIR)a.b.Class1:notExists()V", "11", "void", "0", "t", "void", "", "1", "1", ""),
                // 调用private方法，应记录
                line("3", "1", "a.b.Caller:callPrivateMethod()V", "(SPE)a.b.Class1:privateMethod()V", "12", "void", "0", "t", "void", "", "1", "1", ""),
                // 通过静态调用指令调用非静态方法，应记录
                line("4", "1", "a.b.Caller:callNonStaticByStatic()V", "(STA)a.b.Class1:publicMethod()I", "13", "int", "0", "t", "int", "", "1", "1", ""),
                // 通过非静态调用指令调用静态方法，应记录
                line("5", "1", "a.b.Caller:callStaticByNonStatic()V", "(VIR)a.b.Class1:staticMethod()V", "14", "void", "0", "t", "void", "", "1", "1", ""),
                // 调用子类中未定义、父类中存在的方法，沿父类向上查找，不记录
                line("6", "1", "a.b.Caller:callInheritedMethod()V", "(VIR)a.b.Child:publicMethod()I", "15", "int", "0", "t", "int", "", "1", "1", ""),
                // 调用java.lang.Object中定义的方法，不记录
                line("7", "1", "a.b.Caller:callObjectMethod()V", "(VIR)a.b.Class1:toString()", "16", "java.lang.String", "0", "t", "java.lang.String", "", "1", "1", ""),
                // 调用JDK类的方法，不记录
                line("8", "1", "a.b.Caller:callJdkMethod()V", "(VIR)java.lang.String:length()I", "17", "int", "0", "t", "int", "", "1", "-", ""),
                // 未启用的方法调用，不记录
                line("9", "0", "a.b.Caller:callDisabled()V", "(VIR)a.b.Class1:notExists2()V", "18", "void", "0", "t", "void", "", "1", "1", ""),
                // 不存在指令的方法调用类型，不记录
                line("10", "1", "a.b.Caller:callLambda()V", "(_LM)a.b.Class1:lambda$callLambda$0()V", "19", "void", "0", "t", "void", "", "1", "1", ""));
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_USAGE_OTHER,
                // 被使用字段所在的类不存在，应记录
                line("a.b.Caller:useFieldMissingClass()V", "void", "1", "1", "a.b.Missing3", "FIELD1", "int", "20", "1", "-"),
                // 被使用的字段不存在，应记录
                line("a.b.Caller:useFieldMissing()V", "void", "1", "1", "a.b.Class1", "noSuchField", "int", "21", "1", "1"),
                // 使用private字段，应记录
                line("a.b.Caller:usePrivateField()V", "void", "0", "1", "a.b.Class1", "privateField", "int", "22", "1", "1"),
                // 使用字段时为静态，字段实际为非静态，应记录
                line("a.b.Caller:useNonStaticFieldByStatic()V", "void", "1", "1", "a.b.Class1", "publicField", "int", "23", "1", "1"),
                // 使用子类中未定义、父类中存在的字段，沿父类向上查找，不记录
                line("a.b.Caller:useInheritedField()V", "void", "0", "1", "a.b.Child", "publicField", "int", "24", "1", "1"),
                // 使用父类中存在的静态字段，静态标志一致，不记录
                line("a.b.Caller:useStaticField()V", "void", "1", "1", "a.b.Class1", "publicStaticField", "int", "25", "1", "1"));

        // 执行检查
        JavaCG2OutputInfo javaCG2OutputInfo = new JavaCG2OutputInfo(OUT_DIR, FILE_EXT);
        JavaCG2CompatibilityChecker checker = new JavaCG2CompatibilityChecker(javaCG2OutputInfo,
                new JavaCG2FileOutputAdapter(javaCG2OutputInfo));
        Assert.assertTrue(checker.check());

        // 验证被引用类不存在的报告
        List<String> classReferenceMissingList = readFile4Report(JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_CLASS_REFERENCE_MISSING);
        Assert.assertEquals("被引用类不存在的报告应为2行", 2, classReferenceMissingList.size());
        String[] missingClassColumns = classReferenceMissingList.get(0).split("\t", -1);
        Assert.assertEquals("被引用类不存在的报告应为5列", 5, missingClassColumns.length);
        Assert.assertEquals("引用方类名", "a.b.Class1", missingClassColumns[0]);
        Assert.assertEquals("不存在的被引用类名", "a.b.MissingClass", missingClassColumns[1]);
        Assert.assertEquals("引用方所在jar文件序号", "1", missingClassColumns[2]);
        Assert.assertEquals("引用方所在jar文件路径", "D:/tmp/test1.jar", missingClassColumns[3]);
        Assert.assertTrue("class_reference与dup_class_reference中的记录都应存在", classReferenceMissingList.get(1).contains("a.b.MissingClass2"));

        // 验证方法调用问题的报告
        List<String> methodCallMissingList = readFile4Report(JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_METHOD_CALL_MISSING);
        Assert.assertEquals("方法调用问题的报告应为5行", 5, methodCallMissingList.size());
        List<String> problemDescList = new ArrayList<>();
        for (String line : methodCallMissingList) {
            String[] columns = line.split("\t", -1);
            Assert.assertEquals("方法调用问题的报告应为10列", 10, columns.length);
            problemDescList.add(columns[0] + " " + columns[4]);
        }
        Assert.assertTrue("应记录被调用类未找到", problemDescList.contains("方法调用中的被调用类未找到 a.b.Missing2:run()V"));
        Assert.assertTrue("应记录未找到被调用方法", problemDescList.contains("未找到被调用方法 a.b.Class1:notExists()V"));
        Assert.assertTrue("应记录调用private方法", problemDescList.contains("被调用方法为private方法，无法被其他类调用 a.b.Class1:privateMethod()V"));
        Assert.assertTrue("应记录通过静态调用指令调用非静态方法",
                problemDescList.contains("被调用方法编译时属于静态方法，但目前是非静态方法 a.b.Class1:publicMethod()I"));
        Assert.assertTrue("应记录通过非静态调用指令调用静态方法",
                problemDescList.contains("被调用方法编译时属于非静态方法，但目前是静态方法 a.b.Class1:staticMethod()V"));
        // 验证调用private方法的记录中包含检查明细
        for (String line : methodCallMissingList) {
            if (line.contains("a.b.Class1:privateMethod()V")) {
                Assert.assertTrue("调用private方法的记录中应包含找到的方法定义明细", line.contains("找到方法定义: a.b.Class1:privateMethod()V"));
            }
        }

        // 验证字段使用问题的报告
        List<String> fieldUsageMissingList = readFile4Report(JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_FIELD_USAGE_MISSING);
        Assert.assertEquals("字段使用问题的报告应为4行", 4, fieldUsageMissingList.size());
        List<String> fieldProblemDescList = new ArrayList<>();
        for (String line : fieldUsageMissingList) {
            String[] columns = line.split("\t", -1);
            Assert.assertEquals("字段使用问题的报告应为10列", 10, columns.length);
            fieldProblemDescList.add(columns[0] + " " + columns[4]);
        }
        Assert.assertTrue("应记录被使用字段所在的类未找到", fieldProblemDescList.contains("被使用字段所在的类未找到 FIELD1"));
        Assert.assertTrue("应记录未找到被使用的字段", fieldProblemDescList.contains("未找到被使用的字段 noSuchField"));
        Assert.assertTrue("应记录使用private字段", fieldProblemDescList.contains("被使用的字段为private字段，无法被其他类使用 privateField"));
        Assert.assertTrue("应记录静态标志不一致", fieldProblemDescList.contains("被使用的字段编译时属于静态字段，但目前是非静态字段 publicField"));

        // 验证重复同名类冲突的报告：1条HASH不同，2条public方法冲突，2条public字段冲突
        List<String> dupClassConflictList = readFile4Report(JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_DUP_CLASS_CONFLICT);
        Assert.assertEquals("重复同名类冲突的报告应为5行", 5, dupClassConflictList.size());
        int hashConflictCount = 0;
        int methodConflictCount = 0;
        int fieldConflictCount = 0;
        boolean existsRemovedInDupMethodConflict = false;
        boolean existsOnlyInDupMethodConflict = false;
        boolean existsRemovedInDupFieldConflict = false;
        boolean existsOnlyInDupFieldConflict = false;
        for (String line : dupClassConflictList) {
            String[] columns = line.split("\t", -1);
            Assert.assertEquals("重复同名类冲突的报告应为11列", 11, columns.length);
            Assert.assertEquals("冲突的完整类名", "a.b.Class1", columns[1]);
            if ("H".equals(columns[0])) {
                hashConflictCount++;
                Assert.assertEquals("HASH冲突的类版本1路径", "a/b/Class1.class", columns[2]);
                Assert.assertEquals("HASH冲突的类版本2所在jar文件序号", "2", columns[6]);
            } else if ("M".equals(columns[0])) {
                methodConflictCount++;
                if (line.contains("removedInDup")) {
                    existsRemovedInDupMethodConflict = true;
                    Assert.assertTrue("类版本1存在public方法的冲突说明", columns[10].contains("类版本1存在"));
                } else if (line.contains("onlyInDup")) {
                    existsOnlyInDupMethodConflict = true;
                    Assert.assertTrue("类版本2存在public方法的冲突说明", columns[10].contains("类版本2存在"));
                }
            } else if ("F".equals(columns[0])) {
                fieldConflictCount++;
                if (line.contains("removedInDupField")) {
                    existsRemovedInDupFieldConflict = true;
                } else if (line.contains("onlyInDupField")) {
                    existsOnlyInDupFieldConflict = true;
                }
            }
        }
        Assert.assertEquals("存在多种class文件HASH的记录应为1条", 1, hashConflictCount);
        Assert.assertEquals("public方法冲突的记录应为2条", 2, methodConflictCount);
        Assert.assertEquals("public字段冲突的记录应为2条", 2, fieldConflictCount);
        Assert.assertTrue("应记录仅原始类存在的public方法冲突", existsRemovedInDupMethodConflict);
        Assert.assertTrue("应记录仅重复同名类存在的public方法冲突", existsOnlyInDupMethodConflict);
        Assert.assertTrue("应记录仅原始类存在的public字段冲突", existsRemovedInDupFieldConflict);
        Assert.assertTrue("应记录仅重复同名类存在的public字段冲突", existsOnlyInDupFieldConflict);
    }

    /**
     * 构造无问题的结果文件时，生成的报告文件应为空文件
     */
    @Test
    public void testNoProblem() throws IOException {
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO,
                line("a.b.Class1", "33", "hash1", "1", "a/b/Class1.class"));
        writeFile(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_REFERENCE,
                line("a.b.Class1", "a.b.Class1", "1"),
                line("a.b.Class1", "java.lang.String", "1"));

        JavaCG2OutputInfo javaCG2OutputInfo = new JavaCG2OutputInfo(OUT_DIR, FILE_EXT);
        JavaCG2CompatibilityChecker checker = new JavaCG2CompatibilityChecker(javaCG2OutputInfo,
                new JavaCG2FileOutputAdapter(javaCG2OutputInfo));
        Assert.assertTrue(checker.check());

        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : new JavaCG2OutPutFileTypeEnum[]{
                JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_CLASS_REFERENCE_MISSING,
                JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_METHOD_CALL_MISSING,
                JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_FIELD_USAGE_MISSING,
                JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_DUP_CLASS_CONFLICT}) {
            File reportFile = new File(javaCG2OutputInfo.getMainFilePath(fileTypeEnum));
            Assert.assertTrue("无问题时也应生成报告文件 " + fileTypeEnum.getFileName(), reportFile.exists());
            Assert.assertEquals("无问题时报告文件应为空 " + fileTypeEnum.getFileName(), 0, JavaCG2FileUtil.readFile2List(reportFile.getAbsolutePath()).size());
        }
    }

    // 拼接结果文件中的一行
    private String line(String... columns) {
        return String.join("\t", columns);
    }

    // 写入构造的结果文件
    private void writeFile(JavaCG2OutPutFileTypeEnum fileTypeEnum, String... lines) throws IOException {
        List<String> lineList = new ArrayList<>();
        for (String line : lines) {
            lineList.add(line);
            lineList.add("\n");
        }
        String content = String.join("", lineList);
        Files.write(new File(javaCG2OutputInfoPath(fileTypeEnum)).toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private String javaCG2OutputInfoPath(JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        return OUT_DIR + File.separator + fileTypeEnum.getFileName() + FILE_EXT;
    }

    // 读取生成的报告文件内容
    private List<String> readFile4Report(JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        String filePath = OUT_DIR + File.separator + fileTypeEnum.getFileName() + FILE_EXT;
        Assert.assertTrue("报告文件应存在: " + filePath, new File(filePath).exists());
        return JavaCG2FileUtil.readFile2List(filePath);
    }
}
