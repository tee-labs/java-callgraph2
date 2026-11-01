package test.schema;

import com.adrninistrator.javacg2.common.JavaCG2Constants;
import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchema;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import test.base.TestBase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 结果文件表头输出测试
 * 在同一个输出根目录分别以表头开关关闭、开启的状态执行真实解析（第一次解析的结果先移走），验证：
 * 1 表头开启时，定义了列结构的输出文件首行为正确的列名（与列结构定义一致）
 * 2 表头开启时，输出文件中表头之外的数据行与表头关闭时完全一致
 * 3 表头关闭时，输出文件首行即数据行
 * 4 jar_info文件包含数据行之外的特殊行，表头开启时也不输出表头，文件内容与表头关闭时完全一致
 * 两次解析使用同一个输出根目录，保证jar_info、javacg2_config等记录路径的文件中的数据在两次解析中一致
 * 输出目录使用target目录，避免影响out/test目录中的文件
 */
@Slf4j
public class TestOutputFileHeader extends TestBase {

    // 两次解析共用的输出根目录
    private static final String OUTPUT_ROOT_PATH = "target/header_test/run";

    // 表头关闭时的解析结果保存目录
    private static final String HEADER_OFF_KEEP_DIR = "target/header_test/off";

    @Test
    public void testHeader() throws IOException {
        // 表头关闭时执行解析，将结果移至保存目录
        runParse(Boolean.FALSE, OUTPUT_ROOT_PATH);
        String outputDirName = new File(JAR_TEST_JAR).getName() + "-output_javacg2";
        File outputDir = new File(OUTPUT_ROOT_PATH, outputDirName);
        Assert.assertTrue("未找到输出目录 " + JavaCG2FileUtil.getCanonicalPath(outputDir), outputDir.isDirectory());

        // 执行前清理表头关闭时解析结果的保存目录
        deleteDir(new File(HEADER_OFF_KEEP_DIR));
        File keepDir = new File(HEADER_OFF_KEEP_DIR);
        Assert.assertTrue("创建目录失败 " + JavaCG2FileUtil.getCanonicalPath(keepDir), keepDir.mkdirs());
        Files.move(outputDir.toPath(), new File(HEADER_OFF_KEEP_DIR, outputDirName).toPath());

        // 表头开启时执行解析
        runParse(Boolean.TRUE, OUTPUT_ROOT_PATH);

        File outputDirHeaderOff = new File(HEADER_OFF_KEEP_DIR, outputDirName);
        File outputDirHeaderOn = new File(OUTPUT_ROOT_PATH, outputDirName);
        Assert.assertTrue("未找到表头关闭时的输出目录 " + JavaCG2FileUtil.getCanonicalPath(outputDirHeaderOff), outputDirHeaderOff.isDirectory());
        Assert.assertTrue("未找到表头开启时的输出目录 " + JavaCG2FileUtil.getCanonicalPath(outputDirHeaderOn), outputDirHeaderOn.isDirectory());

        int checkedFileNum = 0;
        int checkedDataLineNum = 0;
        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : JavaCG2OutPutFileTypeEnum.values()) {
            JavaCG2OutputFileSchema schema = fileTypeEnum.getSchema();
            if (schema == null) {
                // 未定义列结构的文件类型不校验
                continue;
            }
            String fileName = fileTypeEnum.getFileName() + JavaCG2Constants.EXT_MD;
            File fileHeaderOff = new File(outputDirHeaderOff, fileName);
            File fileHeaderOn = new File(outputDirHeaderOn, fileName);
            if (!fileHeaderOn.exists() || !fileHeaderOff.exists()) {
                // 当前解析未生成对应文件时跳过
                continue;
            }
            checkedFileNum++;

            List<String> lineListHeaderOff = JavaCG2FileUtil.readFile2List(fileHeaderOff.getPath());
            List<String> lineListHeaderOn = JavaCG2FileUtil.readFile2List(fileHeaderOn.getPath());
            if (lineListHeaderOn.isEmpty()) {
                // 文件内容为空时跳过
                continue;
            }

            if (schema.isHeaderSupported()) {
                // 表头开启时首行为表头，内容与列结构定义一致
                Assert.assertEquals("表头开启时输出文件首行与列结构定义不一致 " + fileHeaderOn.getPath(), schema.genHeaderLine(), lineListHeaderOn.get(0));
                // 表头开启时表头之外的数据行与表头关闭时完全一致
                List<String> dataLineListHeaderOn = lineListHeaderOn.subList(1, lineListHeaderOn.size());

                List<String> dataLineListHeaderOff = lineListHeaderOff;
                if (fileTypeEnum == JavaCG2OutPutFileTypeEnum.OPFTE_JAVACG2_CONFIG) {
                    /*
                        javacg2_config文件记录了配置参数的值，其中output.file.header.enable参数的值在两次解析中分别为false与true，
                        比较数据行时跳过该参数对应的行
                     */
                    String headerEnableConfigKey = JavaCG2ConfigKeyEnum.CKE_OUTPUT_FILE_HEADER_ENABLE.getKey();
                    dataLineListHeaderOff = filterHeaderEnableConfigLine(dataLineListHeaderOff, headerEnableConfigKey);
                    dataLineListHeaderOn = filterHeaderEnableConfigLine(dataLineListHeaderOn, headerEnableConfigKey);
                }

                Assert.assertEquals("表头开启时输出文件的数据行与表头关闭时不一致 " + fileHeaderOn.getPath(), dataLineListHeaderOff, dataLineListHeaderOn);
                checkedDataLineNum += dataLineListHeaderOn.size();

                // 校验表头开启时数据行的列数与列结构一致
                for (String line : dataLineListHeaderOn) {
                    if (StringUtils.isBlank(line)) {
                        continue;
                    }
                    String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
                    Assert.assertTrue("输出文件列数与列结构定义不一致，文件: " + fileHeaderOn.getPath() + "，实际列数: " + columns.length
                            + "，允许的列数范围: " + schema.getMinColumnNum() + "-" + schema.getColumnNum(), schema.checkColumnNumValid(columns.length));
                }
            } else {
                // jar_info等包含特殊行的文件不允许输出表头，表头开启时首行仍为数据行
                Assert.assertFalse("不允许输出表头的输出文件中出现了表头行 " + fileHeaderOn.getPath(), schema.isHeaderLine(lineListHeaderOn.get(0)));
                // 两次解析使用同一个输出根目录，文件内容与表头关闭时完全一致
                Assert.assertEquals("不允许输出表头的输出文件内容与表头关闭时不一致 " + fileHeaderOn.getPath(), lineListHeaderOff, lineListHeaderOn);
            }
        }
        log.info("校验了定义列结构的输出文件数量: {} 表头之外的数据行数量: {}", checkedFileNum, checkedDataLineNum);
        Assert.assertTrue("未找到定义了列结构的输出文件，解析可能未正常执行", checkedFileNum > 0);
        // 表头开启时至少存在一个数据行被校验
        Assert.assertTrue("定义了列结构的输出文件中没有数据行，解析可能未正常执行", checkedDataLineNum > 0);
    }

    // 过滤javacg2_config文件中output.file.header.enable参数对应的行
    private List<String> filterHeaderEnableConfigLine(List<String> lineList, String headerEnableConfigKey) {
        List<String> resultList = new ArrayList<>(lineList.size());
        for (String line : lineList) {
            String[] columns = StringUtils.splitPreserveAllTokens(line, JavaCG2Constants.FLAG_TAB);
            if (columns.length >= 2 && headerEnableConfigKey.equals(columns[1])) {
                continue;
            }
            resultList.add(line);
        }
        return resultList;
    }

    // 执行一次真实解析
    private void runParse(Boolean headerEnable, String outputRootPath) {
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JavaCg2(JAR_TEST_JAR);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, outputRootPath);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_FILE_HEADER_ENABLE, headerEnable.toString());

        // 执行前清理输出目录，避免残留文件影响校验
        deleteDir(new File(outputRootPath));

        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
        Assert.assertTrue("解析失败 输出根目录: " + outputRootPath, javaCG2Entry.run());
    }

    // 删除文件或目录
    private void deleteDir(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles != null) {
                for (File childFile : listFiles) {
                    deleteDir(childFile);
                }
            }
        }
        Assert.assertTrue("删除文件失败 " + JavaCG2FileUtil.getCanonicalPath(file), JavaCG2FileUtil.deleteFile(file));
    }
}
