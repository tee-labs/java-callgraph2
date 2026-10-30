package test.jardiff;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.dto.jardiff.JavaCG2JarDiffModifiedClassInfo;
import com.adrninistrator.javacg2.dto.jardiff.JavaCG2JarDiffModifiedMethodInfo;
import com.adrninistrator.javacg2.jardiff.JavaCG2JarDiffRunner;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: 对JarDiff比较处理类的单元级测试
 * 通过手工构造两个伪造的解析输出目录（jar_info/class_info/method_info小文件），对jar文件配对、jar文件HASH相同的跳过、
 * 类文件HASH相同的跳过、方法新增/修改、静态代码块与指令HASH为空方法的跳过、新增jar文件中的类与方法等场景进行验证
 * jar_info中记录的jar文件路径指向构造的小文件，仅用于计算文件HASH
 */
public class TestJarDiffRunner {

    private static final String OUT_ROOT = "out/test_jar_diff";

    private static final String OLD_DIR = OUT_ROOT + "/old";

    private static final String NEW_DIR = OUT_ROOT + "/new";

    private static final String DIFF_DIR = OUT_ROOT + "/diff";

    private static final String FILE_EXT = ".md";

    // 构造的jar文件的内容
    private static final String JAR_CONTENT_LIB_OLD = "jar-content-lib-old";

    private static final String JAR_CONTENT_LIB_NEW = "jar-content-lib-new";

    private static final String JAR_CONTENT_SAME = "jar-content-same";

    private static final String JAR_CONTENT_SPRING_CORE_OLD = "jar-content-spring-core-old";

    private static final String JAR_CONTENT_SPRING_CORE_NEW = "jar-content-spring-core-new";

    private static final String JAR_CONTENT_EXTRA = "jar-content-extra";

    @Before
    public void init() throws IOException {
        // 删除上次生成的目录
        FileUtils.deleteDirectory(new File(OUT_ROOT));
        Files.createDirectories(new File(OLD_DIR).toPath());
        Files.createDirectories(new File(NEW_DIR).toPath());
        Files.createDirectories(new File(DIFF_DIR).toPath());
    }

    /**
     * 对JarDiff比较进行全面验证
     * 场景：jar文件名版本号差异配对、jar文件HASH相同跳过、类文件HASH相同跳过、方法新增/修改、删除的方法不记录、
     * 静态代码块与指令HASH为空的方法跳过、新增jar文件中的类与全部方法、文件名前缀含“-”但后接非数字的jar文件配对
     */
    @Test
    public void testCompare() throws IOException {
        // 构造新旧目录中的jar文件（内容任意，仅用于计算文件HASH）
        String libJarPathOld = genFile(OLD_DIR, "lib-1.0.jar", JAR_CONTENT_LIB_OLD);
        String libJarPathNew = genFile(NEW_DIR, "lib-2.0.jar", JAR_CONTENT_LIB_NEW);
        String sameJarPathOld = genFile(OLD_DIR, "same-1.0.jar", JAR_CONTENT_SAME);
        String sameJarPathNew = genFile(NEW_DIR, "same-1.0.jar", JAR_CONTENT_SAME);
        String springCoreJarPathOld = genFile(OLD_DIR, "spring-core-5.0.0.jar", JAR_CONTENT_SPRING_CORE_OLD);
        String springCoreJarPathNew = genFile(NEW_DIR, "spring-core-6.0.0.jar", JAR_CONTENT_SPRING_CORE_NEW);
        String extraJarPathNew = genFile(NEW_DIR, "extra-1.0.jar", JAR_CONTENT_EXTRA);

        // 构造旧目录的结果文件
        writeFile(OLD_DIR, "jar_info",
                line("J", "1", libJarPathOld, ""),
                line("J", "2", sameJarPathOld, ""),
                line("R", "3", OLD_DIR, ""),
                line("J", "4", springCoreJarPathOld, ""));
        writeFile(OLD_DIR, "class_info",
                line("a.b.ClassSame", "1", "hash_class_same", "1", "a/b/ClassSame.class"),
                line("a.b.ClassModified", "1", "hash_class_modified_old", "1", "a/b/ClassModified.class"),
                line("c.s.ClassCore", "1", "hash_class_core_old", "4", "c/s/ClassCore.class"));
        writeFile(OLD_DIR, "method_info",
                line("a.b.ClassSame:m1()I", "1", "int", "0", "J", "0", "hash_same_m1", "1"),
                line("a.b.ClassSame:<clinit>()V", "8", "void", "0", "J", "0", "hash_same_clinit", "1"),
                line("a.b.ClassModified:mUnchanged()V", "1", "void", "0", "J", "0", "hash_m_unchanged", "1"),
                line("a.b.ClassModified:mChanged()V", "1", "void", "0", "J", "0", "hash_m_changed_old", "1"),
                line("a.b.ClassModified:mRemoved()I", "1", "int", "0", "J", "0", "hash_m_removed", "1"),
                line("a.b.ClassModified:<clinit>()V", "8", "void", "0", "J", "0", "hash_m_clinit", "1"),
                line("c.s.ClassCore:core()J", "9", "long", "0", "J", "0", "hash_core_old", "4"));

        // 构造新目录的结果文件
        writeFile(NEW_DIR, "jar_info",
                line("J", "1", libJarPathNew, ""),
                line("J", "2", sameJarPathNew, ""),
                line("R", "3", NEW_DIR, ""),
                line("J", "4", springCoreJarPathNew, ""),
                line("J", "5", extraJarPathNew, ""));
        writeFile(NEW_DIR, "class_info",
                line("a.b.ClassSame", "1", "hash_class_same", "1", "a/b/ClassSame.class"),
                line("a.b.ClassModified", "1", "hash_class_modified_new", "1", "a/b/ClassModified.class"),
                line("c.s.ClassCore", "1", "hash_class_core_new", "4", "c/s/ClassCore.class"),
                line("e.x.ClassExtra", "1", "hash_class_extra", "5", "e/x/ClassExtra.class"));
        writeFile(NEW_DIR, "method_info",
                line("a.b.ClassSame:m1()I", "1", "int", "0", "J", "0", "hash_same_m1", "1"),
                line("a.b.ClassSame:<clinit>()V", "8", "void", "0", "J", "0", "hash_same_clinit", "1"),
                line("a.b.ClassModified:mUnchanged()V", "1", "void", "0", "J", "0", "hash_m_unchanged", "1"),
                line("a.b.ClassModified:mChanged()V", "1", "void", "0", "J", "0", "hash_m_changed_new", "1"),
                line("a.b.ClassModified:mAdded()Ljava/lang/String;", "1", "java.lang.String", "0", "J", "0", "hash_m_added", "1"),
                line("a.b.ClassModified:mAbstract()V", "1025", "void", "0", "J", "0", "", "1"),
                line("a.b.ClassModified:<clinit>()V", "8", "void", "0", "J", "0", "hash_m_clinit", "1"),
                line("c.s.ClassCore:core()J", "9", "long", "0", "J", "0", "hash_core_new", "4"),
                line("e.x.ClassExtra:extra1()V", "1", "void", "0", "J", "0", "hash_extra1", "5"),
                line("e.x.ClassExtra:extra2()V", "1", "void", "0", "J", "0", "hash_extra2", "5"),
                line("e.x.ClassExtra:<clinit>()V", "8", "void", "0", "J", "0", "hash_extra_clinit", "5"));

        // 执行比较
        JavaCG2JarDiffRunner jarDiffRunner = new JavaCG2JarDiffRunner(OLD_DIR, NEW_DIR, DIFF_DIR);
        jarDiffRunner.setOutputFileExt(FILE_EXT);
        Assert.assertTrue(jarDiffRunner.run());

        // 验证发生变化的类：lib-1.0.jar与lib-2.0.jar配对，ClassSame类文件HASH相同被跳过；extra-1.0.jar为新增的jar文件
        List<String> modifiedClassList = readFile(DIFF_DIR, JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_CLASSES);
        Assert.assertEquals("发生变化的类应为3行", 3, modifiedClassList.size());
        for (String row : modifiedClassList) {
            Assert.assertEquals("发生变化的类的报告应为9列", 9, row.split("\t", -1).length);
        }
        // 第1行为a.b.ClassModified，第2行为c.s.ClassCore（按jar文件名称、类名排序），第3行为e.x.ClassExtra
        String[] classModifiedColumns = modifiedClassList.get(0).split("\t", -1);
        Assert.assertEquals("jar文件名称前缀", "lib", classModifiedColumns[0]);
        Assert.assertEquals("新jar文件名称", "lib-2.0.jar", classModifiedColumns[1]);
        Assert.assertEquals("旧jar文件名称", "lib-1.0.jar", classModifiedColumns[2]);
        Assert.assertEquals("发生变化的类名", "a.b.ClassModified", classModifiedColumns[3]);
        Assert.assertEquals("class文件HASH发生变化的类的变更类型", "M", classModifiedColumns[4]);
        Assert.assertEquals("新class文件在jar文件中的路径", "a/b/ClassModified.class", classModifiedColumns[5]);
        Assert.assertEquals("新类所在的jar文件序号", "1", classModifiedColumns[6]);
        Assert.assertEquals("旧class文件在jar文件中的路径", "a/b/ClassModified.class", classModifiedColumns[7]);
        Assert.assertEquals("旧类所在的jar文件序号", "1", classModifiedColumns[8]);

        String[] classCoreColumns = modifiedClassList.get(1).split("\t", -1);
        Assert.assertEquals("jar文件名称前缀（文件名中的“-”后接非数字时不作为版本号）", "spring-core", classCoreColumns[0]);
        Assert.assertEquals("新jar文件名称", "spring-core-6.0.0.jar", classCoreColumns[1]);
        Assert.assertEquals("旧jar文件名称", "spring-core-5.0.0.jar", classCoreColumns[2]);
        Assert.assertEquals("发生变化的类名", "c.s.ClassCore", classCoreColumns[3]);
        Assert.assertEquals("class文件HASH发生变化的类的变更类型", "M", classCoreColumns[4]);

        String[] classExtraColumns = modifiedClassList.get(2).split("\t", -1);
        Assert.assertEquals("新增jar文件的jar文件名称前缀", "extra", classExtraColumns[0]);
        Assert.assertEquals("新增jar文件的新jar文件名称", "extra-1.0.jar", classExtraColumns[1]);
        Assert.assertEquals("新增jar文件的旧jar文件名称为空", "", classExtraColumns[2]);
        Assert.assertEquals("新增的类名", "e.x.ClassExtra", classExtraColumns[3]);
        Assert.assertEquals("新增的类的变更类型", "N", classExtraColumns[4]);
        Assert.assertEquals("新增的类的旧class文件在jar文件中的路径为空", "", classExtraColumns[7]);
        Assert.assertEquals("新增的类的旧类所在的jar文件序号为空", "", classExtraColumns[8]);

        // 验证发生变化的方法
        List<String> modifiedMethodList = readFile(DIFF_DIR, JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_METHODS);
        Assert.assertEquals("发生变化的方法应为6行", 6, modifiedMethodList.size());
        Map<String, Boolean> modifiedMethodMap = new LinkedHashMap<>();
        for (String row : modifiedMethodList) {
            Assert.assertEquals("发生变化的方法的报告应为6列", 6, row.split("\t", -1).length);
            String[] columns = row.split("\t", -1);
            modifiedMethodMap.put(columns[3], Boolean.parseBoolean(columns[5]));
        }
        // 修改的方法、新增的方法均应记录；未变化、删除的方法、静态代码块、指令HASH为空的方法不记录
        Assert.assertTrue("方法指令HASH变化的方法应记录为修改",
                modifiedMethodMap.containsKey("a.b.ClassModified:mChanged()V") && modifiedMethodMap.get("a.b.ClassModified:mChanged()V"));
        Assert.assertTrue("旧目录中不存在的方法应记录为新增",
                modifiedMethodMap.containsKey("a.b.ClassModified:mAdded()Ljava/lang/String;") && !modifiedMethodMap.get("a.b.ClassModified:mAdded()Ljava/lang/String;"));
        Assert.assertTrue("方法指令HASH变化的方法应记录为修改", modifiedMethodMap.containsKey("c.s.ClassCore:core()J") && modifiedMethodMap.get("c.s.ClassCore:core()J"));
        Assert.assertTrue("新增jar文件中的方法应记录为新增", modifiedMethodMap.containsKey("e.x.ClassExtra:extra1()V") && !modifiedMethodMap.get("e.x.ClassExtra:extra1()V"));
        Assert.assertTrue("新增jar文件中的方法应记录为新增", modifiedMethodMap.containsKey("e.x.ClassExtra:extra2()V") && !modifiedMethodMap.get("e.x.ClassExtra:extra2()V"));
        Assert.assertTrue("新增jar文件中的静态代码块也应记录", modifiedMethodMap.containsKey("e.x.ClassExtra:<clinit>()V") && !modifiedMethodMap.get("e.x.ClassExtra:<clinit>()V"));
        Assert.assertFalse("方法指令HASH未变化的方法不应记录", modifiedMethodMap.containsKey("a.b.ClassModified:mUnchanged()V"));
        Assert.assertFalse("旧目录中存在但新目录中不存在的方法（删除的方法）不应记录", modifiedMethodMap.containsKey("a.b.ClassModified:mRemoved()I"));
        Assert.assertFalse("静态代码块在旧类存在时不应记录", modifiedMethodMap.containsKey("a.b.ClassModified:<clinit>()V"));
        Assert.assertFalse("方法指令HASH为空的方法在旧类存在时不应记录", modifiedMethodMap.containsKey("a.b.ClassModified:mAbstract()V"));
        Assert.assertFalse("类文件HASH相同的类的方法不应记录", modifiedMethodMap.containsKey("a.b.ClassSame:m1()I"));

        // 验证内存中的结果
        Map<String, List<JavaCG2JarDiffModifiedMethodInfo>> jarModifiedMethodInfoMap = jarDiffRunner.getJarModifiedMethodInfoMap();
        Assert.assertEquals("发生变化的jar文件数量", 3, jarModifiedMethodInfoMap.size());
        List<JavaCG2JarDiffModifiedMethodInfo> libModifiedMethodList = jarModifiedMethodInfoMap.get("lib-2.0.jar");
        Assert.assertNotNull(libModifiedMethodList);
        Assert.assertEquals("lib-2.0.jar中发生变化的方法数量", 2, libModifiedMethodList.size());
        Map<String, List<JavaCG2JarDiffModifiedClassInfo>> jarModifiedClassInfoMap = jarDiffRunner.getJarModifiedClassInfoMap();
        Assert.assertEquals("发生变化的jar文件数量", 3, jarModifiedClassInfoMap.size());
        List<JavaCG2JarDiffModifiedClassInfo> extraModifiedClassList = jarModifiedClassInfoMap.get("extra-1.0.jar");
        Assert.assertNotNull(extraModifiedClassList);
        Assert.assertFalse("新增jar文件中的类在旧jar文件中不存在", extraModifiedClassList.get(0).isOldClassExists());
    }

    /**
     * 验证旧目录中jar_info记录的jar文件不存在时，将其视为内容发生变化的jar文件，继续通过class文件HASH、方法指令HASH比较
     */
    @Test
    public void testJarFileMissingTreatedAsModified() throws IOException {
        String goneJarPathNew = genFile(NEW_DIR, "gone-2.0.jar", "jar-content-gone-new");

        // 旧目录中的jar文件gone-1.0.jar未构造，文件不存在
        writeFile(OLD_DIR, "jar_info",
                line("J", "1", OLD_DIR + "/gone-1.0.jar", ""));
        writeFile(OLD_DIR, "class_info");
        writeFile(OLD_DIR, "method_info");
        writeFile(NEW_DIR, "jar_info",
                line("J", "1", goneJarPathNew, ""));
        writeFile(NEW_DIR, "class_info",
                line("g.c.ClassGone", "1", "hash_class_gone", "1", "g/c/ClassGone.class"));
        writeFile(NEW_DIR, "method_info",
                line("g.c.ClassGone:run()V", "1", "void", "0", "J", "0", "hash_gone_run", "1"));

        JavaCG2JarDiffRunner jarDiffRunner = new JavaCG2JarDiffRunner(OLD_DIR, NEW_DIR, DIFF_DIR);
        jarDiffRunner.setOutputFileExt(FILE_EXT);
        Assert.assertTrue(jarDiffRunner.run());

        // jar文件HASH无法计算时视为内容发生变化，其中的类作为新增的类记录
        List<String> modifiedClassList = readFile(DIFF_DIR, JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_CLASSES);
        Assert.assertEquals("发生变化的类应为1行", 1, modifiedClassList.size());
        String[] classGoneColumns = modifiedClassList.get(0).split("\t", -1);
        Assert.assertEquals("jar文件名称前缀", "gone", classGoneColumns[0]);
        Assert.assertEquals("新增的类的变更类型", "N", classGoneColumns[4]);

        List<String> modifiedMethodList = readFile(DIFF_DIR, JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_METHODS);
        Assert.assertEquals("发生变化的方法应为1行", 1, modifiedMethodList.size());
        Assert.assertTrue(modifiedMethodList.get(0).contains("g.c.ClassGone:run()V"));
    }

    /**
     * 验证新旧目录中jar文件内容相同时不发生变化的jar文件，比较结果文件仍生成但内容为空
     */
    @Test
    public void testNoModifiedJar() throws IOException {
        String sameJarPathOld = genFile(OLD_DIR, "same-1.0.jar", JAR_CONTENT_SAME);
        String sameJarPathNew = genFile(NEW_DIR, "same-1.0.jar", JAR_CONTENT_SAME);

        writeFile(OLD_DIR, "jar_info", line("J", "1", sameJarPathOld, ""));
        writeFile(OLD_DIR, "class_info", line("a.b.ClassSame", "1", "hash_class_same", "1", "a/b/ClassSame.class"));
        writeFile(OLD_DIR, "method_info", line("a.b.ClassSame:m1()I", "1", "int", "0", "J", "0", "hash_same_m1", "1"));
        writeFile(NEW_DIR, "jar_info", line("J", "1", sameJarPathNew, ""));
        writeFile(NEW_DIR, "class_info", line("a.b.ClassSame", "1", "hash_class_same", "1", "a/b/ClassSame.class"));
        writeFile(NEW_DIR, "method_info", line("a.b.ClassSame:m1()I", "1", "int", "0", "J", "0", "hash_same_m1", "1"));

        JavaCG2JarDiffRunner jarDiffRunner = new JavaCG2JarDiffRunner(OLD_DIR, NEW_DIR, DIFF_DIR);
        jarDiffRunner.setOutputFileExt(FILE_EXT);
        Assert.assertTrue(jarDiffRunner.run());

        // 未发现变化的类、方法时也生成内容为空的结果文件
        Assert.assertTrue(readFile(DIFF_DIR, JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_CLASSES).isEmpty());
        Assert.assertTrue(readFile(DIFF_DIR, JavaCG2JarDiffRunner.FILE_NAME_JAR_DIFF_MODIFIED_METHODS).isEmpty());
        Assert.assertTrue(jarDiffRunner.getJarModifiedMethodInfoMap().isEmpty());
        Assert.assertTrue(jarDiffRunner.getJarModifiedClassInfoMap().isEmpty());
    }

    /**
     * 验证旧目录不存在时执行失败；当前类的实例只能执行一次run方法
     */
    @Test
    public void testInvalidArgsAndRunOnce() throws IOException {
        String sameJarPathNew = genFile(NEW_DIR, "same-1.0.jar", JAR_CONTENT_SAME);
        writeFile(NEW_DIR, "jar_info", line("J", "1", sameJarPathNew, ""));
        writeFile(NEW_DIR, "class_info");
        writeFile(NEW_DIR, "method_info");

        JavaCG2JarDiffRunner jarDiffRunner = new JavaCG2JarDiffRunner(OUT_ROOT + "/not_exists", NEW_DIR, DIFF_DIR);
        jarDiffRunner.setOutputFileExt(FILE_EXT);
        Assert.assertFalse("旧输出目录不存在时应执行失败", jarDiffRunner.run());

        // 同一实例不允许再次执行
        Assert.assertFalse("当前类的实例只能执行一次run方法", jarDiffRunner.run());
    }

    // 构造一个内容为指定文本的文件，返回其规范路径
    private String genFile(String dirPath, String fileName, String content) throws IOException {
        File file = new File(dirPath, fileName);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return JavaCG2FileUtil.getCanonicalPath(file);
    }

    // 构造结果文件，每个参数为一行，不写各列的值时生成空文件
    private void writeFile(String dirPath, String fileName, String... lines) throws IOException {
        String filePath = JavaCG2FileUtil.addSeparator4FilePath(dirPath) + fileName + FILE_EXT;
        FileUtils.writeLines(new File(filePath), StandardCharsets.UTF_8.name(), Arrays.asList(lines), false);
    }

    // 构造一行记录
    private String line(String... columns) {
        return StringUtils.join(columns, JavaCG2Constants.FLAG_TAB);
    }

    // 读取生成的结果文件内容
    private List<String> readFile(String dirPath, String fileName) {
        String filePath = JavaCG2FileUtil.addSeparator4FilePath(dirPath) + fileName + FILE_EXT;
        Assert.assertTrue("结果文件应存在: " + filePath, new File(filePath).exists());
        return JavaCG2FileUtil.readFile2List(filePath);
    }
}
