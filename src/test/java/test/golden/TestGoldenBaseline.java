package test.golden;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.dto.output.JavaCG2OutputInfo;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import com.adrninistrator.javacg2.writer.JavaCG2MemoryOutputAdapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author adrninistrator
 * @date 2026/11/2
 * @description: 黄金基线回归测试
 * 解析固定的输入文件（src/test/resources/golden/golden-fixture.jar，二进制提交入库，与src/test/java彻底解耦），
 * 断言全部结果文件的内容与提交在库中的黄金指纹清单（src/test/resources/golden/golden_manifest.properties，文件名=归一化后内容的md5）一致，
 * 任何写出行为的意外变化都会导致当前测试失败
 * 测试流程：将golden-fixture.jar复制到target/golden-work/后再解析（避免合并jar等运行时产物写入src/test/resources），
 * 使用JavaCG2MemoryOutputAdapter纯内存输出（不落盘，无Windows下文件锁风险；内存与文件写出内容逐字节一致的对称性由test.memory包中的测试保证），
 * 从内存读回全部主结果文件与扩展/自由格式文件，内容归一化后逐文件计算md5，与黄金清单比对：文件集合双向一致，每个md5一致
 * 归一化规则（消除环境差异）：仓库绝对路径与target/golden-work路径替换为占位符（\与/两种分隔符都处理），剩余的Windows路径分隔符统一为/，换行符统一为\n；
 * 类文件HASH值、方法指令HASH值来自提交的固定jar文件字节，天然稳定；方法处理耗时日志（_log_method_spend_time）内容天然波动，不纳入清单
 * 基线更新方式：有意变更写出行为后，通过系统属性golden.update重新生成黄金清单后通过：
 * mvn test -Dtest=TestGoldenBaseline -Dgolden.update=true
 * 变更基线时需要同步检查golden-fixture.jar与golden_manifest.properties的改动是否符合预期
 */
@Slf4j
public class TestGoldenBaseline extends TestBase {

    // 黄金基线资源目录
    private static final String GOLDEN_RESOURCE_DIR = "src/test/resources/golden";

    // 固定夹具jar文件路径（提交入库的解析输入，运行时只读）
    private static final String GOLDEN_JAR_SOURCE_PATH = GOLDEN_RESOURCE_DIR + "/golden-fixture.jar";

    // 黄金指纹清单文件路径（提交入库，golden.update模式下覆写）
    private static final String GOLDEN_MANIFEST_PATH = GOLDEN_RESOURCE_DIR + "/golden_manifest.properties";

    // 工作目录：夹具jar文件复制到当前目录后再解析，运行时产物（合并jar、输出目录等）均生成在当前目录下
    private static final String GOLDEN_WORK_DIR = "target/golden-work";

    // 工作目录中的夹具jar文件路径
    private static final String GOLDEN_JAR_WORK_PATH = GOLDEN_WORK_DIR + "/golden-fixture.jar";

    // 基线更新开关的系统属性名，设为true时重算并覆写黄金清单后通过
    public static final String SYS_PROP_GOLDEN_UPDATE = "golden.update";

    // 本次执行归一化后实际内容的转储目录，比对失败或更新基线时写出，便于查看差异明细
    private static final String ACTUAL_DUMP_DIR = GOLDEN_WORK_DIR + "/actual";

    // 归一化占位符：仓库根目录
    private static final String PLACEHOLDER_REPO_DIR = "{REPO_DIR}";

    // 归一化占位符：工作目录
    private static final String PLACEHOLDER_GOLDEN_WORK_DIR = "{GOLDEN_WORK_DIR}";

    // 黄金清单文件中的说明信息
    private static final String MANIFEST_HEADER = "# java-callgraph2黄金基线指纹清单，文件名=归一化后内容（UTF-8）的md5\n" +
            "# 重新生成方式：mvn test -Dtest=TestGoldenBaseline -Dgolden.update=true\n";

    /**
     * 验证全部结果文件内容与黄金清单一致
     */
    @Test
    public void testGoldenBaseline() throws IOException {
        // 1 将夹具jar文件复制到工作目录
        prepareWorkJar();

        // 2 解析工作目录中的夹具jar文件，结果写入内存输出适配器
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(GOLDEN_JAR_WORK_PATH);
        // 显式关闭Jar兼容性检查，保持基线文件集合固定
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_CHECK_JAR_COMPATIBILITY_ENABLE, Boolean.FALSE.toString());

        JavaCG2MemoryOutputAdapter memoryOutputAdapter = new JavaCG2MemoryOutputAdapter();
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        javaCG2Entry.setOutputAdapter(memoryOutputAdapter);
        Assert.assertTrue("解析夹具jar文件失败 " + GOLDEN_JAR_WORK_PATH, javaCG2Entry.run());

        // 3 从内存读回全部结果文件内容
        Map<String, String> actualContentMap = collectActualContents(memoryOutputAdapter,
                javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo());

        // 4 保证基线有意义：夹具jar文件中的关键内容应出现在对应的结果文件中
        assertFixtureParsed(actualContentMap);

        // 5 归一化后逐文件计算md5
        Map<String, String> normalizedContentMap = new TreeMap<>();
        Map<String, String> actualMd5Map = new TreeMap<>();
        for (Map.Entry<String, String> contentEntry : actualContentMap.entrySet()) {
            String normalizedContent = normalizeContent(contentEntry.getValue());
            normalizedContentMap.put(contentEntry.getKey(), normalizedContent);
            actualMd5Map.put(contentEntry.getKey(), DigestUtils.md5Hex(normalizedContent.getBytes(StandardCharsets.UTF_8)));
        }

        // 6 基线更新模式：重算并覆写黄金清单后通过
        if (Boolean.parseBoolean(System.getProperty(SYS_PROP_GOLDEN_UPDATE, "false"))) {
            updateGoldenManifest(actualMd5Map, normalizedContentMap);
            return;
        }

        // 7 严格比对模式：与黄金清单双向比对
        checkGoldenManifest(actualMd5Map, normalizedContentMap);
    }

    // 将夹具jar文件复制到工作目录，避免合并jar等运行时产物写入src/test/resources
    private void prepareWorkJar() throws IOException {
        File goldenJarSourceFile = new File(GOLDEN_JAR_SOURCE_PATH);
        Assert.assertTrue("夹具jar文件不存在，应随代码提交入库 " + JavaCG2FileUtil.getCanonicalPath(goldenJarSourceFile),
                goldenJarSourceFile.isFile());

        // 删除上次的运行时产物后重建工作目录
        FileUtils.deleteDirectory(new File(GOLDEN_WORK_DIR));
        FileUtils.forceMkdir(new File(GOLDEN_WORK_DIR));
        FileUtils.copyFile(goldenJarSourceFile, new File(GOLDEN_JAR_WORK_PATH));
    }

    // 从内存适配器读回全部结果文件内容，key为结果文件名（与写入结果目录时的文件名一致），value为文件内容
    private Map<String, String> collectActualContents(JavaCG2MemoryOutputAdapter memoryOutputAdapter, JavaCG2OutputInfo outputInfo) {
        Map<String, String> contentMap = new TreeMap<>();

        // 主结果文件：经genMainRecordSink写出的文件，未生成的文件类型（如Jar兼容性检查报告文件）读回为null，跳过
        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : JavaCG2OutPutFileTypeEnum.values()) {
            if (fileTypeEnum == JavaCG2OutPutFileTypeEnum.OPFTE_ILLEGAL) {
                // 非法值不代表实际的文件
                continue;
            }
            String content = memoryOutputAdapter.getMainRecordContent(fileTypeEnum);
            if (content == null) {
                // 本次执行未生成的文件类型
                continue;
            }
            putContent(contentMap, outputInfo.getMainFileName(fileTypeEnum), content);
        }

        // 自由格式文本文件：所有配置参数与当前使用的配置参数的markdown文件，固定生成
        putContent(contentMap, JavaCG2Constants.FILE_JAVACG2_ALL_CONFIG_MD,
                memoryOutputAdapter.getOtherRecordContent(JavaCG2Constants.FILE_JAVACG2_ALL_CONFIG_MD));
        putContent(contentMap, JavaCG2Constants.FILE_JAVACG2_USED_CONFIG_MD,
                memoryOutputAdapter.getOtherRecordContent(JavaCG2Constants.FILE_JAVACG2_USED_CONFIG_MD));

        // 扩展解析器生成的其他类型结果文件（内置Spring XML解析类写出的文件等），登记的文件名不包含后缀名
        String outputFileExt = outputInfo.getOutputFileExt();
        for (String otherFileName : outputInfo.getOtherFileNameSet()) {
            String content = memoryOutputAdapter.getOtherRecordContent(otherFileName);
            if (content == null) {
                // 登记了文件信息但本次执行未写出的文件
                continue;
            }
            putContent(contentMap, otherFileName + outputFileExt, content);
        }

        /*
            EL表达式忽略数据文件：表达式管理类创建时即经输出适配器登记，当前测试未配置EL表达式，内容为空
            若内容非空（如EL表达式配置默认值变化导致命中忽略条件）也纳入文件集合参与比对
         */
        String elIgnoreDataContent = memoryOutputAdapter.getOtherRecordContent(JavaCG2Constants.EL_IGNORE_DATA_LOG_FILE_NAME);
        if (elIgnoreDataContent != null) {
            putContent(contentMap, JavaCG2Constants.EL_IGNORE_DATA_LOG_FILE_NAME, elIgnoreDataContent);
        }

        /*
            方法处理耗时日志文件（_log_method_spend_time.md）的内容为各方法每次执行的耗时，天然波动，不纳入清单
            当前配置下该文件在内存适配器中有对应数据（getOtherRecordContent可读回），此处不读回即不参与比对
         */
        return contentMap;
    }

    // 向内容Map中放入文件内容，value不允许为null，同一文件名不允许重复放入
    private void putContent(Map<String, String> contentMap, String fileName, String content) {
        Assert.assertNotNull("结果文件内容不允许为null " + fileName, content);
        Assert.assertNull("同一文件名对应了多个结果文件，文件集合存在冲突 " + fileName, contentMap.get(fileName));
        contentMap.put(fileName, content);
    }

    // 保证基线有意义：夹具jar文件中的关键内容应出现在对应的结果文件中，防止夹具损坏或解析开关变化导致基线静默退化为空内容
    private void assertFixtureParsed(Map<String, String> contentMap) {
        // 类的信息：夹具类应全部出现
        Assert.assertTrue("class_info中应包含夹具类", contentMap.get("class_info.md").contains("golden.core.GoldenChildService"));
        // 继承与实现
        Assert.assertTrue("extends_impl中应包含继承与实现信息",
                contentMap.get("extends_impl.md").contains("golden.core.GoldenChildService") &&
                        contentMap.get("extends_impl.md").contains("golden.core.GoldenSpi"));
        // 方法调用
        Assert.assertTrue("method_call中应包含夹具类的方法调用", contentMap.get("method_call.md").contains("golden."));
        // Lambda表达式方法
        Assert.assertTrue("lambda_method_info中应包含Lambda表达式方法信息",
                StringUtils.isNotBlank(contentMap.get("lambda_method_info.md")));
        // 内部类（含匿名内部类）
        Assert.assertTrue("inner_class中应包含匿名内部类", contentMap.get("inner_class.md").contains("golden.inner.GoldenOuter$1"));
        // 枚举
        Assert.assertTrue("enum_init_assign_info中应包含枚举常量赋值信息",
                StringUtils.isNotBlank(contentMap.get("enum_init_assign_info.md")));
        // try-catch-finally
        Assert.assertTrue("method_catch中应包含catch信息", StringUtils.isNotBlank(contentMap.get("method_catch.md")));
        Assert.assertTrue("method_finally中应包含finally信息", StringUtils.isNotBlank(contentMap.get("method_finally.md")));
        // 类与方法注解
        Assert.assertTrue("class_annotation中应包含类的注解", contentMap.get("class_annotation.md").contains("GoldenMarker"));
        Assert.assertTrue("method_annotation中应包含方法的注解", contentMap.get("method_annotation.md").contains("GoldenTrace"));
        // get/set方法与字段关联关系
        Assert.assertTrue("get_method中应包含dto的get方法", contentMap.get("get_method.md").contains("golden.dto.GoldenDto"));
        Assert.assertTrue("field_relationship中应包含通过get/set方法关联的字段关系",
                StringUtils.isNotBlank(contentMap.get("field_relationship.md")));
        // 静态、final字段初始化方法信息
        Assert.assertTrue("sf_field_method_call中应包含static final字段初始化方法信息",
                StringUtils.isNotBlank(contentMap.get("sf_field_method_call.md")));
        // 内置Spring XML解析类写出的文件
        Assert.assertTrue("spring_bean中应包含XML中定义的Bean", contentMap.get("spring_bean.md").contains("goldenChildService"));
        Assert.assertTrue("spring_task_xml中应包含定时任务信息", StringUtils.isNotBlank(contentMap.get("spring_task_xml.md")));
        Assert.assertTrue("spring_aop_advice_xml中应包含advice信息", StringUtils.isNotBlank(contentMap.get("spring_aop_advice_xml.md")));
        Assert.assertTrue("spring_scan_package_xml中应包含包扫描路径", StringUtils.isNotBlank(contentMap.get("spring_scan_package_xml.md")));
    }

    // 归一化：替换环境相关路径、统一分隔符与换行符，消除环境差异
    private String normalizeContent(String content) {
        // 仓库根目录的规范路径（Windows分隔符）
        String repoDirPath = JavaCG2FileUtil.getCanonicalPath(new File(""));
        String result = content;
        // 仓库绝对路径替换为占位符，\与/两种分隔符都处理
        result = StringUtils.replace(result, repoDirPath + "\\", PLACEHOLDER_REPO_DIR + "/");
        result = StringUtils.replace(result, repoDirPath + "/", PLACEHOLDER_REPO_DIR + "/");
        result = StringUtils.replace(result, repoDirPath, PLACEHOLDER_REPO_DIR);
        // 工作目录路径替换为占位符（绝对路径的前缀已在上方替换，此处处理剩余的相对路径形式），\与/两种分隔符都处理
        result = StringUtils.replace(result, "target\\golden-work", PLACEHOLDER_GOLDEN_WORK_DIR);
        result = StringUtils.replace(result, "target/golden-work", PLACEHOLDER_GOLDEN_WORK_DIR);
        // 统一剩余的Windows路径分隔符，保证同一内容在Windows与Linux下归一化结果一致
        result = StringUtils.replace(result, "\\", "/");
        // 统一换行符，主代码统一输出\n，此处兜底处理自由格式文本中可能出现的\r\n
        result = StringUtils.replace(result, "\r\n", "\n");
        result = StringUtils.replace(result, "\r", "\n");
        return result;
    }

    // 基线更新模式：重算黄金清单并覆写文件后通过，用于有意变更写出行为后更新基线
    private void updateGoldenManifest(Map<String, String> actualMd5Map, Map<String, String> normalizedContentMap) throws IOException {
        // 与旧清单比对，打印变化明细
        Properties oldManifest = loadGoldenManifestOrNull();
        if (oldManifest != null) {
            for (Map.Entry<String, String> actualEntry : actualMd5Map.entrySet()) {
                String oldMd5 = oldManifest.getProperty(actualEntry.getKey());
                if (oldMd5 == null) {
                    log.warn("[黄金基线更新] 新增文件 {} {}", actualEntry.getKey(), actualEntry.getValue());
                } else if (!oldMd5.equals(actualEntry.getValue())) {
                    log.warn("[黄金基线更新] md5变化 {} {} -> {}", actualEntry.getKey(), oldMd5, actualEntry.getValue());
                }
            }
            for (String oldFileName : oldManifest.stringPropertyNames()) {
                if (!actualMd5Map.containsKey(oldFileName)) {
                    log.warn("[黄金基线更新] 删除文件 {} {}", oldFileName, oldManifest.getProperty(oldFileName));
                }
            }
        }

        writeGoldenManifest(actualMd5Map);
        log.warn("[黄金基线更新] 已重新生成黄金清单，共{}个文件 {}", actualMd5Map.size(), JavaCG2FileUtil.getCanonicalPath(new File(GOLDEN_MANIFEST_PATH)));
        dumpActualContents(normalizedContentMap);
    }

    // 严格比对模式：文件集合双向一致，每个md5一致，失败时打印全部差异明细与再生成指引
    private void checkGoldenManifest(Map<String, String> actualMd5Map, Map<String, String> normalizedContentMap) throws IOException {
        Properties goldenManifest = loadGoldenManifestOrNull();
        Assert.assertTrue("黄金清单文件不存在，应随代码提交入库，或通过 -D" + SYS_PROP_GOLDEN_UPDATE + "=true 重新生成 " +
                JavaCG2FileUtil.getCanonicalPath(new File(GOLDEN_MANIFEST_PATH)), goldenManifest != null);

        Set<String> goldenFileNames = goldenManifest.stringPropertyNames();
        Set<String> actualFileNames = actualMd5Map.keySet();

        // 黄金清单中存在而实际未生成的文件
        List<String> onlyInGoldenList = new ArrayList<>();
        for (String goldenFileName : goldenFileNames) {
            if (!actualFileNames.contains(goldenFileName)) {
                onlyInGoldenList.add(goldenFileName);
            }
        }

        // 实际生成而黄金清单中缺少的文件
        List<String> onlyInActualList = new ArrayList<>();
        for (String actualFileName : actualFileNames) {
            if (!goldenFileNames.contains(actualFileName)) {
                onlyInActualList.add(actualFileName);
            }
        }

        // md5不一致的文件
        List<String> md5DiffList = new ArrayList<>();
        for (Map.Entry<String, String> actualEntry : actualMd5Map.entrySet()) {
            String goldenMd5 = goldenManifest.getProperty(actualEntry.getKey());
            if (goldenMd5 != null && !goldenMd5.equals(actualEntry.getValue())) {
                md5DiffList.add(actualEntry.getKey() + ": 预期=" + goldenMd5 + " 实际=" + actualEntry.getValue());
            }
        }

        if (onlyInGoldenList.isEmpty() && onlyInActualList.isEmpty() && md5DiffList.isEmpty()) {
            log.info("黄金基线比对通过，共{}个结果文件与清单一致", actualFileNames.size());
            return;
        }

        // 失败时转储本次归一化后的实际内容，便于查看差异明细
        dumpActualContents(normalizedContentMap);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("结果文件内容与黄金基线不一致（共").append(actualFileNames.size()).append("个结果文件参与比对）");
        if (!onlyInGoldenList.isEmpty()) {
            messageBuilder.append("\n黄金清单中存在而实际未生成的文件: ").append(onlyInGoldenList);
        }
        if (!onlyInActualList.isEmpty()) {
            messageBuilder.append("\n实际生成而黄金清单中缺少的文件: ").append(onlyInActualList);
        }
        if (!md5DiffList.isEmpty()) {
            messageBuilder.append("\nmd5不一致的文件（预期=黄金清单, 实际=本次执行，共").append(md5DiffList.size()).append("个）:");
            for (String md5Diff : md5DiffList) {
                messageBuilder.append("\n  ").append(md5Diff);
            }
        }
        messageBuilder.append("\n本次归一化后的实际内容已转储到 ").append(JavaCG2FileUtil.getCanonicalPath(new File(ACTUAL_DUMP_DIR)))
                .append(" 目录，可与git中的golden-fixture.jar解析结果对照排查");
        messageBuilder.append("\n若为有意的行为变更，请重新生成黄金清单后提交：mvn test -Dtest=TestGoldenBaseline -D")
                .append(SYS_PROP_GOLDEN_UPDATE).append("=true");
        Assert.fail(messageBuilder.toString());
    }

    // 读取黄金清单文件，不存在时返回null
    private Properties loadGoldenManifestOrNull() throws IOException {
        File manifestFile = new File(GOLDEN_MANIFEST_PATH);
        if (!manifestFile.isFile()) {
            return null;
        }
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(manifestFile), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    // 覆写黄金清单文件，文件名排序后输出，保证文件内容稳定
    private void writeGoldenManifest(Map<String, String> actualMd5Map) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(GOLDEN_MANIFEST_PATH), StandardCharsets.UTF_8))) {
            writer.write(MANIFEST_HEADER);
            for (Map.Entry<String, String> entry : actualMd5Map.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.write("\n");
            }
        }
    }

    // 将本次归一化后的实际内容转储到工作目录，便于比对失败时查看差异明细
    private void dumpActualContents(Map<String, String> normalizedContentMap) throws IOException {
        File actualDumpDirFile = new File(ACTUAL_DUMP_DIR);
        FileUtils.deleteDirectory(actualDumpDirFile);
        FileUtils.forceMkdir(actualDumpDirFile);
        for (Map.Entry<String, String> entry : normalizedContentMap.entrySet()) {
            // 文件名中不包含路径分隔符，仅包含结果文件名
            File dumpFile = new File(actualDumpDirFile, entry.getKey());
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dumpFile), StandardCharsets.UTF_8))) {
                writer.write(entry.getValue());
            }
        }
        log.info("已转储本次归一化后的实际内容，共{}个文件 {}", normalizedContentMap.size(), JavaCG2FileUtil.getCanonicalPath(actualDumpDirFile));
    }
}
