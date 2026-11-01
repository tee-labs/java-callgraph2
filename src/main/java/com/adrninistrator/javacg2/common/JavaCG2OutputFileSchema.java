package com.adrninistrator.javacg2.common;

import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 输出文件的列结构（schema）类，作为结果文件各列名称与列数的单一来源
 * 每个有固定列结构的结果文件类型在JavaCG2OutPutFileTypeEnum枚举常量上定义对应的当前类实例
 * 支持列名转列下标、列下标转列名的双向查询，用于结果文件的写出与读回解析
 * 部分结果文件的行宽存在多种合法值：
 * 1 注解类文件在注解没有属性值时省略“注解属性名称”“注解属性值”两列
 * 2 lambda_method_info文件在未找到下一个被调用方法时省略最后一列
 * 以上文件通过minColumnNum字段记录合法的最小列数，其余文件最小列数与列数相同
 */
public class JavaCG2OutputFileSchema {

    // 各列的名称
    private final String[] columns;

    // 有效数据行的最小列数
    private final int minColumnNum;

    // 是否允许输出表头，含特殊行的文件（如jar_info）不允许输出表头
    private final boolean headerSupported;

    // 列名转列下标的Map
    private final Map<String, Integer> columnIndexMap;

    // 表头行内容，各列名称以tab分隔
    private final String headerLine;

    /**
     * @param columns 各列的名称
     */
    public JavaCG2OutputFileSchema(String... columns) {
        this(columns.length, true, columns);
    }

    /**
     * @param minColumnNum 有效数据行的最小列数
     * @param columns      各列的名称
     */
    public JavaCG2OutputFileSchema(int minColumnNum, String... columns) {
        this(minColumnNum, true, columns);
    }

    /**
     * @param minColumnNum    有效数据行的最小列数
     * @param headerSupported 是否允许输出表头
     * @param columns         各列的名称
     */
    public JavaCG2OutputFileSchema(int minColumnNum, boolean headerSupported, String... columns) {
        if (ArrayUtils.isEmpty(columns)) {
            throw new IllegalArgumentException("列名数组不允许为空");
        }
        if (minColumnNum < 1 || minColumnNum > columns.length) {
            throw new IllegalArgumentException("最小列数非法 " + minColumnNum + " " + columns.length);
        }
        this.columns = columns;
        this.minColumnNum = minColumnNum;
        this.headerSupported = headerSupported;
        columnIndexMap = new HashMap<>(columns.length);
        for (int i = 0; i < columns.length; i++) {
            Integer prevIndex = columnIndexMap.put(columns[i], i);
            if (prevIndex != null) {
                // 列名重复会导致列名转列下标的查询结果不确定，直接失败
                throw new IllegalArgumentException("列名重复 " + columns[i] + " " + ArrayUtils.toString(columns));
            }
        }
        headerLine = String.join(JavaCG2Constants.FLAG_TAB, columns);
    }

    // 获取列数
    public int getColumnNum() {
        return columns.length;
    }

    // 获取有效数据行的最小列数
    public int getMinColumnNum() {
        return minColumnNum;
    }

    // 是否允许输出表头
    public boolean isHeaderSupported() {
        return headerSupported;
    }

    // 获取各列的名称
    public String[] getColumns() {
        return columns;
    }

    /**
     * 判断指定的列数是否合法（在最小列数与列数之间）
     *
     * @param columnNum
     * @return true: 合法
     */
    public boolean checkColumnNumValid(int columnNum) {
        return columnNum >= minColumnNum && columnNum <= columns.length;
    }

    /**
     * 列名转列下标
     *
     * @param columnName 列名
     * @return 列下标，从0开始；列名不存在时返回-1
     */
    public int getColumnIndex(String columnName) {
        Integer index = columnIndexMap.get(columnName);
        return index == null ? -1 : index;
    }

    /**
     * 列下标转列名
     *
     * @param index 列下标，从0开始
     * @return 列名，下标越界时返回null
     */
    public String getColumnName(int index) {
        if (index < 0 || index >= columns.length) {
            return null;
        }
        return columns[index];
    }

    // 获取表头行内容，各列名称以tab分隔
    public String genHeaderLine() {
        return headerLine;
    }

    /**
     * 判断一行内容是否为表头行
     *
     * @param line 一行内容
     * @return true: 表头行
     */
    public boolean isHeaderLine(String line) {
        return headerLine.equals(line);
    }
}
