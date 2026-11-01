package com.adrninistrator.javacg2.common;

import lombok.Getter;

import java.util.List;

/**
 * @author adrninistrator
 * @date 2026/11/2
 * @description: 输出文件列结构（schema）的结构化信息
 * 由JavaCG2OutputFileSchemaExporter从JavaCG2OutPutFileTypeEnum枚举常量上定义的JavaCG2OutputFileSchema生成
 * 供下游项目以结构化方式使用结果文件的列结构信息
 */
@Getter
public class JavaCG2OutputFileSchemaInfo {

    // 文件名，不包含文件后缀
    private final String fileName;

    // 文件的中文描述
    private final String desc;

    // 完整列数
    private final int columnNum;

    // 有效数据行的最小列数
    private final int minColumnNum;

    // 是否允许输出表头
    private final boolean headerSupported;

    // 各列的名称，列表下标即列下标（从0开始）
    private final List<String> columns;

    public JavaCG2OutputFileSchemaInfo(String fileName, String desc, int columnNum, int minColumnNum, boolean headerSupported, List<String> columns) {
        this.fileName = fileName;
        this.desc = desc;
        this.columnNum = columnNum;
        this.minColumnNum = minColumnNum;
        this.headerSupported = headerSupported;
        this.columns = columns;
    }
}
