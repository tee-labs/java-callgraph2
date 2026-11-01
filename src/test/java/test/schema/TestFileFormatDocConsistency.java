package test.schema;

import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchema;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author adrninistrator
 * @date 2026/11/2
 * @description: docs/file_format.md与JavaCG2OutPutFileTypeEnum中定义的列结构（schema）的一致性测试
 * 遍历所有定义了列结构的文件类型，解析文档中对应的节，校验文档中的列名序列、列数与schema完全一致
 * 任一文件类型的文档漂移（缺少对应的节、列数不一致、列名不一致、缺少最小列数说明）都会导致当前测试失败，并打印差异明细
 * 文档节的格式约定：以“## ”标题行分节，节中“- 文件名”之后的第一个非空行为文件名，“- 文件各列内容说明”之后为“|序号|说明|”格式的表格，
 * 表格“说明”列以schema中定义的列名开头（其后可跟“，”或“（”等补充说明）
 */
@Slf4j
public class TestFileFormatDocConsistency {

    private static final String FILE_FORMAT_MD_PATH = "docs" + File.separator + "file_format.md";

    // 文件名标志行
    private static final String FLAG_FILE_NAME = "- 文件名";

    // 文件各列内容说明标志行
    private static final String FLAG_COLUMN_DESC = "- 文件各列内容说明";

    // markdown节标题前缀
    private static final String FLAG_TITLE_PREFIX = "## ";

    // 最小列数说明前缀
    private static final String FLAG_MIN_COLUMN_NUM = "最小列数为";

    @Test
    public void testFileFormatDocMatchSchema() throws IOException {
        List<String> docLines = readDocLines(FILE_FORMAT_MD_PATH);
        List<String> structErrorList = new ArrayList<>();
        Map<String, DocSection> docSectionMap = parseDocSections(docLines, structErrorList);
        Assert.assertTrue("解析" + FILE_FORMAT_MD_PATH + "未找到任何文件名，文档格式可能已变化", !docSectionMap.isEmpty());

        List<String> diffList = new ArrayList<>(structErrorList);
        int checkedNum = 0;
        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : JavaCG2OutPutFileTypeEnum.values()) {
            JavaCG2OutputFileSchema schema = fileTypeEnum.getSchema();
            if (schema == null) {
                // 未定义列结构的文件类型不校验
                continue;
            }
            checkedNum++;

            String fileName = fileTypeEnum.getFileName();
            DocSection docSection = docSectionMap.get(fileName);
            if (docSection == null) {
                diffList.add("[" + fileName + "] 文档中缺少对应的节");
                continue;
            }
            checkDocSection(fileName, schema, docSection, diffList);
        }

        // 打印全部差异明细
        for (String diff : diffList) {
            log.error("文档与列结构定义不一致 {}", diff);
        }
        Assert.assertTrue("定义了列结构的文件类型数量异常 " + checkedNum, checkedNum > 0);
        Assert.assertTrue(FILE_FORMAT_MD_PATH + " 与JavaCG2OutPutFileTypeEnum中定义的列结构存在" + diffList.size() + "处不一致，明细见上方日志",
                diffList.isEmpty());
        log.info("检查了定义列结构的文件类型数量: {} 文档节数量: {} 未发现不一致", checkedNum, docSectionMap.size());
    }

    // 校验文档中一个文件类型对应节的内容与列结构定义一致
    private void checkDocSection(String fileName, JavaCG2OutputFileSchema schema, DocSection docSection, List<String> diffList) {
        // 校验完整列数
        if (docSection.columnDescList.size() != schema.getColumnNum()) {
            diffList.add("[" + fileName + "] 列数不一致，文档: " + docSection.columnDescList.size() + " 列，schema: " + schema.getColumnNum()
                    + " 列，文档中各列: " + docSection.columnDescList);
            return;
        }

        // 逐列校验列名序列：文档“说明”列需要以schema中定义的列名开头
        String[] columns = schema.getColumns();
        for (int i = 0; i < columns.length; i++) {
            String columnDesc = docSection.columnDescList.get(i);
            if (!columnDesc.startsWith(columns[i])) {
                diffList.add("[" + fileName + "] 第" + (i + 1) + "列列名不一致，文档: " + columnDesc + "，schema: " + columns[i]);
            }
        }

        // 校验最小列数说明：存在合法的更少列数的数据行时，文档中需要说明最小列数
        if (schema.getMinColumnNum() < schema.getColumnNum()) {
            if (!docSection.sectionText.toString().contains(FLAG_MIN_COLUMN_NUM + schema.getMinColumnNum())) {
                diffList.add("[" + fileName + "] 文档中缺少最小列数说明 " + FLAG_MIN_COLUMN_NUM + schema.getMinColumnNum());
            }
        }
    }

    // 读取文档的全部原始行（包含空行与标题行）
    private List<String> readDocLines(String filePath) throws IOException {
        List<String> docLines = new ArrayList<>();
        try (BufferedReader br = JavaCG2FileUtil.genBufferedReader(filePath)) {
            String line;
            while ((line = br.readLine()) != null) {
                docLines.add(line);
            }
        }
        return docLines;
    }

    // 解析文档，以“## ”标题行分节，返回文件名与节的对应关系；文档结构异常时记录到structErrorList
    private Map<String, DocSection> parseDocSections(List<String> docLines, List<String> structErrorList) {
        Map<String, DocSection> docSectionMap = new HashMap<>();

        // 当前节的各行，“## ”标题行之前的内容不属于任何节
        List<String> curSectionLines = null;
        for (String line : docLines) {
            if (line.startsWith(FLAG_TITLE_PREFIX)) {
                if (curSectionLines != null) {
                    parseDocSection(curSectionLines, docSectionMap, structErrorList);
                }
                curSectionLines = new ArrayList<>();
            }
            if (curSectionLines == null) {
                continue;
            }
            curSectionLines.add(line);
        }
        if (curSectionLines != null) {
            parseDocSection(curSectionLines, docSectionMap, structErrorList);
        }
        return docSectionMap;
    }

    private void parseDocSection(List<String> sectionLines, Map<String, DocSection> docSectionMap, List<String> structErrorList) {
        DocSection docSection = new DocSection();
        // 解析阶段，0: 未找到文件名标志行 1: 等待文件名 2: 等待列内容说明标志行 3: 等待表头行 4: 等待分隔行 5: 表格数据行 6: 表格结束
        int stage = 0;
        for (String line : sectionLines) {
            String trimmedLine = line.trim();
            switch (stage) {
                case 0:
                    if (FLAG_FILE_NAME.equals(trimmedLine)) {
                        stage = 1;
                    }
                    break;
                case 1:
                    if (StringUtils.isEmpty(trimmedLine)) {
                        // 文件名之前的空行
                        break;
                    }
                    docSection.fileName = trimmedLine;
                    stage = 2;
                    break;
                case 2:
                    docSection.sectionText.append(line).append("\n");
                    if (FLAG_COLUMN_DESC.equals(trimmedLine)) {
                        stage = 3;
                    }
                    break;
                case 3:
                    if (StringUtils.isEmpty(trimmedLine)) {
                        // 表头行之前的空行
                        break;
                    }
                    if (!checkTableHeaderLine(trimmedLine, docSection.fileName, structErrorList)) {
                        return;
                    }
                    stage = 4;
                    break;
                case 4:
                    stage = 5;
                    break;
                case 5:
                    if (!trimmedLine.startsWith("|")) {
                        // 表格结束
                        stage = 6;
                        break;
                    }
                    docSection.columnDescList.add(parseColumnDesc(trimmedLine, docSection.fileName, structErrorList));
                    break;
                default:
                    break;
            }
        }

        if (docSection.fileName == null) {
            // 表头说明等不含文件名的节
            return;
        }
        DocSection prevSection = docSectionMap.put(docSection.fileName, docSection);
        if (prevSection != null) {
            structErrorList.add("[" + docSection.fileName + "] 文档中存在多个对应的节");
        }
    }

    // 校验表格头两行（|序号|说明|与分隔行）的格式
    private boolean checkTableHeaderLine(String tableHeaderLine, String fileName, List<String> structErrorList) {
        String[] cells = StringUtils.splitPreserveAllTokens(tableHeaderLine, "|");
        if (cells.length < 3 || !"序号".equals(cells[1]) || !"说明".equals(cells[2])) {
            structErrorList.add("[" + fileName + "] 列内容说明表格头格式异常 " + tableHeaderLine);
            return false;
        }
        return true;
    }

    // 从表格数据行中解析“说明”列
    private String parseColumnDesc(String tableRowLine, String fileName, List<String> structErrorList) {
        String[] cells = StringUtils.splitPreserveAllTokens(tableRowLine, "|");
        if (cells.length < 3) {
            structErrorList.add("[" + fileName + "] 列内容说明表格行格式异常 " + tableRowLine);
            return "";
        }
        return cells[2];
    }

    // 文档节
    private static class DocSection {
        // 文件名
        private String fileName;

        // 节的文本内容
        private final StringBuilder sectionText = new StringBuilder();

        // 表格中各数据行“说明”列的内容
        private final List<String> columnDescList = new ArrayList<>();
    }
}
