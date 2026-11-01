package com.adrninistrator.javacg2.common;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.markdown.MarkdownConstants;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2026/11/2
 * @description: 输出文件列结构（schema）导出类
 * 遍历JavaCG2OutPutFileTypeEnum枚举，为每个定义了列结构的文件类型生成结构化信息与markdown格式的文档内容，供下游项目及文档生成使用
 * 列结构信息以枚举常量上定义的JavaCG2OutputFileSchema为单一来源，未定义列结构的文件类型会被跳过
 */
@Slf4j
public class JavaCG2OutputFileSchemaExporter {

    /**
     * 生成所有定义了列结构的文件类型的结构化信息，按枚举定义顺序返回
     *
     * @return 结构化信息列表
     */
    public List<JavaCG2OutputFileSchemaInfo> genSchemaInfoList() {
        List<JavaCG2OutputFileSchemaInfo> schemaInfoList = new ArrayList<>();
        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : JavaCG2OutPutFileTypeEnum.values()) {
            JavaCG2OutputFileSchemaInfo schemaInfo = genSchemaInfo(fileTypeEnum);
            if (schemaInfo != null) {
                schemaInfoList.add(schemaInfo);
            }
        }
        return schemaInfoList;
    }

    /**
     * 生成指定文件类型的结构化信息
     *
     * @param fileTypeEnum 文件类型枚举
     * @return 结构化信息，未定义列结构时返回null
     */
    public JavaCG2OutputFileSchemaInfo genSchemaInfo(JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        JavaCG2OutputFileSchema schema = fileTypeEnum.getSchema();
        if (schema == null) {
            return null;
        }
        // 对列名数组进行拷贝，避免外部修改影响schema中定义的列名
        List<String> columns = new ArrayList<>(Arrays.asList(schema.getColumns()));
        return new JavaCG2OutputFileSchemaInfo(fileTypeEnum.getFileName(), fileTypeEnum.getDesc(),
                schema.getColumnNum(), schema.getMinColumnNum(), schema.isHeaderSupported(), columns);
    }

    /**
     * 生成所有定义了列结构的文件类型的markdown文档内容，每个文件类型一节
     *
     * @return markdown文档内容
     */
    public String genAllMarkdown() {
        StringBuilder stringBuilder = new StringBuilder();
        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : JavaCG2OutPutFileTypeEnum.values()) {
            String markdown = genMarkdown(fileTypeEnum);
            if (markdown != null) {
                stringBuilder.append(markdown);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 生成指定文件类型的markdown文档内容，每个文件类型一节
     *
     * @param fileTypeEnum 文件类型枚举
     * @return markdown文档内容，未定义列结构时返回null
     */
    public String genMarkdown(JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        JavaCG2OutputFileSchemaInfo schemaInfo = genSchemaInfo(fileTypeEnum);
        if (schemaInfo == null) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(MarkdownConstants.FLAG_TITLE).append(MarkdownConstants.FLAG_TITLE).append(MarkdownConstants.FLAG_SPACE)
                .append(schemaInfo.getFileName()).append(MarkdownConstants.FLAG_SPACE).append(schemaInfo.getDesc())
                .append("\n\n");

        // 文件说明
        stringBuilder.append(MarkdownConstants.FLAG_LIST).append("文件说明\n\n").append(schemaInfo.getDesc()).append("\n\n");

        // 列结构说明
        stringBuilder.append(MarkdownConstants.FLAG_LIST).append("列结构说明\n\n");
        stringBuilder.append("列数: ").append(schemaInfo.getColumnNum()).append("\n\n");
        if (schemaInfo.getMinColumnNum() < schemaInfo.getColumnNum()) {
            stringBuilder.append("最小列数: ").append(schemaInfo.getMinColumnNum()).append("，部分数据行的列数可能少于以上列数\n\n");
        } else {
            stringBuilder.append("最小列数: ").append(schemaInfo.getMinColumnNum()).append("\n\n");
        }
        if (schemaInfo.isHeaderSupported()) {
            stringBuilder.append("是否允许表头: 是\n\n");
        } else {
            stringBuilder.append("是否允许表头: 否，文件中包含数据行之外的特殊行\n\n");
        }

        // 带下标的列名列表
        stringBuilder.append(MarkdownConstants.FLAG_LIST).append("带下标的列名列表\n\n");
        stringBuilder.append(MarkdownConstants.FLAG_VERTICAL_BAR).append("下标").append(MarkdownConstants.FLAG_VERTICAL_BAR).append("列名")
                .append(MarkdownConstants.FLAG_VERTICAL_BAR).append("\n");
        stringBuilder.append(MarkdownConstants.FLAG_VERTICAL_BAR).append(MarkdownConstants.FLAG_TABLE_LINE)
                .append(MarkdownConstants.FLAG_VERTICAL_BAR).append(MarkdownConstants.FLAG_TABLE_LINE)
                .append(MarkdownConstants.FLAG_VERTICAL_BAR).append("\n");
        List<String> columns = schemaInfo.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            stringBuilder.append(MarkdownConstants.FLAG_VERTICAL_BAR).append(i).append(MarkdownConstants.FLAG_VERTICAL_BAR)
                    .append(columns.get(i)).append(MarkdownConstants.FLAG_VERTICAL_BAR).append("\n");
        }
        stringBuilder.append("\n");

        // 表头行内容，各列名称以tab分隔
        if (schemaInfo.isHeaderSupported()) {
            stringBuilder.append(MarkdownConstants.FLAG_LIST).append("表头行内容\n\n");
            stringBuilder.append(MarkdownConstants.FLAG_CODE).append("\n").append(fileTypeEnum.getSchema().genHeaderLine())
                    .append("\n").append(MarkdownConstants.FLAG_CODE).append("\n\n");
        }
        return stringBuilder.toString();
    }

    /**
     * 将所有定义了列结构的文件类型的markdown文档内容写出到指定的markdown文件
     *
     * @param mdFilePath markdown文件路径
     * @throws IOException 写文件失败时抛出
     */
    public void writeToFile(String mdFilePath) throws IOException {
        try (BufferedWriter writer = JavaCG2FileUtil.genBufferedWriter(mdFilePath)) {
            writer.write(genAllMarkdown());
        }
        log.info("已写出输出文件列结构文档 {}", JavaCG2FileUtil.getCanonicalPath(mdFilePath));
    }
}
