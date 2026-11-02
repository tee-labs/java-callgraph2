package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchema;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 结果记录数据来源接口，按结果文件类型定位一个结果文件的数据行，用于在写出后读回结果数据（如Jar兼容性检查、JarDiff比较）
 * 与写出侧的JavaCG2RecordSink接口对应；默认实现从输出目录中的结果文件读回（JavaCG2FileRecordSource），
 * 内存实现从内存输出适配器收集的数据中读回（JavaCG2MemoryRecordSource），使读回逻辑与文件路径解耦
 */
public interface JavaCG2RecordSource {

    /**
     * 生成用于逐行读取数据行的BufferedReader对象
     * 表头行（开启output.file.header.enable参数时写出的首行）由实现在内部跳过，调用方无需处理
     *
     * @return 读取数据行的BufferedReader对象，结果数据不可用时返回null（如结果文件不存在、未生成对应的结果文件）
     * @throws IOException 读取失败时抛出异常
     */
    BufferedReader genBufferedReader() throws IOException;

    /**
     * 获取结果文件对应的列结构（schema），供调用方按列名读取各列
     * 典型用法：将genBufferedReader读到的数据行以tab分隔后，通过getSchema().getColumnIndex("列名")获得列下标并取值，
     * 通过getSchema().checkColumnNumValid(列数)校验行宽（注解类文件等存在可变行宽，见列结构的minColumnNum说明）
     * 自行编写裸读器解析结果文件时，可通过getSchema().isHeaderLine(首行内容)判断并跳过表头行
     *
     * @return 结果文件对应的列结构；实现持有文件名而非文件类型枚举（如经genOtherRecordSource读回的扩展文件，无固定列结构），
     * 或文件类型未定义列结构时返回null，此时只能按列下标读取
     */
    JavaCG2OutputFileSchema getSchema();
}
