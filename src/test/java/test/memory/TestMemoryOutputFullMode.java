package test.memory;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElAllowedVariableEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.writer.JavaCG2MemoryOutputAdapter;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

/**
 * @author adrninistrator
 * @date 2026/11/2
 * @description: 纯内存输出模式完整性测试
 * 使用JavaCG2MemoryOutputAdapter时，output.root.path参数未设置或无效，JavaCG2Entry.run()均完整成功，
 * 无列结构的自由格式文本文件也经输出适配器写出，可从内存侧读回：
 * 1 所有配置参数与当前使用的配置参数的markdown文件（_javacg2_all_config.md、_javacg2_used_config.md）
 * 2 方法处理耗时日志文件（_log_method_spend_time.md），未写入时读回为null，与文件实现未写文件时不生成文件的行为一致
 * 3 EL表达式忽略数据文件（el_ignore_data.log），在EL表达式命中忽略条件时读回非空
 */
@Slf4j
public class TestMemoryOutputFullMode extends TestBase {

    // 无效的输出根目录路径：以文件（target/test.jar）作为父目录，目录必然无法创建
    private static final String INVALID_OUTPUT_ROOT_PATH = "target/test.jar/not_possible_dir";

    /**
     * 验证纯内存模式：未设置output.root.path参数时完整成功
     */
    @Test
    public void testMemoryModeWithoutOutputRootPath() {
        doTestMemoryMode(null);
    }

    /**
     * 验证纯内存模式：output.root.path参数为无效路径时完整成功
     */
    @Test
    public void testMemoryModeWithInvalidOutputRootPath() {
        doTestMemoryMode(INVALID_OUTPUT_ROOT_PATH);
    }

    /**
     * 验证纯内存模式：方法处理耗时日志未写入时读回为null（skip语义），写入后可读回
     */
    @Test
    public void testMemoryModeSpendTimeFileSkip() {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        // 关闭记录方法处理耗时，方法处理耗时日志文件不应写出
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_LOG_METHOD_SPEND_TIME, Boolean.FALSE.toString());
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_CHECK_JAR_COMPATIBILITY_ENABLE, Boolean.FALSE.toString());

        JavaCG2MemoryOutputAdapter memoryOutputAdapter = new JavaCG2MemoryOutputAdapter();
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        javaCG2Entry.setOutputAdapter(memoryOutputAdapter);
        Assert.assertTrue("纯内存模式下执行失败", javaCG2Entry.run());

        String spendTimeFileName = javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo()
                .getMainFileName(JavaCG2OutPutFileTypeEnum.OPFTE_LOG_METHOD_SPEND_TIME);
        Assert.assertNull("未写入方法处理耗时日志时不应有对应的内存数据，与文件实现未写文件时不生成文件的行为一致",
                memoryOutputAdapter.getOtherRecordContent(spendTimeFileName));
    }

    private void doTestMemoryMode(String outputRootPath) {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        if (outputRootPath != null) {
            javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, outputRootPath);
        }
        // 关闭Jar兼容性检查，当前测试仅验证解析与自由格式文本文件的内存写出
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_CHECK_JAR_COMPATIBILITY_ENABLE, Boolean.FALSE.toString());
        // 设置EL表达式，使解析方法时部分类的命中忽略条件，触发EL表达式忽略数据写出，其余类的方法正常解析
        javaCG2ConfigureWrapper.setElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD,
                "string.startsWith(" + JavaCG2ElAllowedVariableEnum.EAVE_PARSE_CLASS_NAME.getVariableName() + ", 'test.el.')");

        JavaCG2MemoryOutputAdapter memoryOutputAdapter = new JavaCG2MemoryOutputAdapter();
        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        javaCG2Entry.setOutputAdapter(memoryOutputAdapter);
        Assert.assertTrue("纯内存模式下执行失败 output.root.path: " + outputRootPath, javaCG2Entry.run());

        // 主结果文件可从内存侧读回
        Assert.assertTrue("class_info结果文件内容不应为空",
                StringUtils.isNotBlank(memoryOutputAdapter.getMainRecordContent(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO)));

        // 所有配置参数的markdown文件可从内存侧读回，且包含关键内容
        String allConfigContent = memoryOutputAdapter.getOtherRecordContent(JavaCG2Constants.FILE_JAVACG2_ALL_CONFIG_MD);
        Assert.assertTrue("所有配置参数的markdown文件内容不应为空", StringUtils.isNotBlank(allConfigContent));
        Assert.assertTrue("所有配置参数的markdown文件应包含公共说明", allConfigContent.contains("当前文件代表所有支持的配置参数"));

        // 当前使用的配置参数的markdown文件可从内存侧读回，且包含关键内容
        String usedConfigContent = memoryOutputAdapter.getOtherRecordContent(JavaCG2Constants.FILE_JAVACG2_USED_CONFIG_MD);
        Assert.assertTrue("当前使用的配置参数的markdown文件内容不应为空", StringUtils.isNotBlank(usedConfigContent));
        Assert.assertTrue("当前使用的配置参数的markdown文件应包含公共说明", usedConfigContent.contains("当前文件代表本次执行时有使用的配置参数"));

        // 方法处理耗时日志文件可从内存侧读回（log.method.spend.time参数开启）
        String spendTimeFileName = javaCG2Entry.getJavaCG2InputAndOutput().getJavaCG2OutputInfo()
                .getMainFileName(JavaCG2OutPutFileTypeEnum.OPFTE_LOG_METHOD_SPEND_TIME);
        String spendTimeContent = memoryOutputAdapter.getOtherRecordContent(spendTimeFileName);
        Assert.assertTrue("方法处理耗时日志文件内容不应为空", StringUtils.isNotBlank(spendTimeContent));

        // EL表达式忽略数据文件可从内存侧读回，且包含忽略数据
        String elIgnoreDataContent = memoryOutputAdapter.getOtherRecordContent(JavaCG2Constants.EL_IGNORE_DATA_LOG_FILE_NAME);
        Assert.assertTrue("EL表达式忽略数据文件内容不应为空", StringUtils.isNotBlank(elIgnoreDataContent));
        Assert.assertTrue("EL表达式忽略数据文件应包含忽略数据说明", elIgnoreDataContent.contains("通过表达式执行结果判断需要忽略当前数据"));

        // 自由格式文本文件也可经内存RecordSource读回
        JavaCG2RecordSource recordSource = memoryOutputAdapter.genOtherRecordSource(JavaCG2Constants.EL_IGNORE_DATA_LOG_FILE_NAME);
        Assert.assertNotNull("已写出的自由格式文本文件应能获得读回数据来源", recordSource);
    }
}
