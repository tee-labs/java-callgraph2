package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchema;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 从内存中读回记录数据的数据来源实现类，数据来源为JavaCG2MemoryOutputAdapter收集的记录内容
 * 记录内容与写入结果文件的内容格式一致（各列以tab分隔，每行一条记录）
 */
public class JavaCG2MemoryRecordSource extends JavaCG2AbstractRecordSource {

    // 收集的记录内容，格式与写入结果文件的内容一致
    private final String content;

    /**
     * @param content      收集的记录内容
     * @param fileTypeEnum 结果文件类型，可为null（未定义列结构的文件类型等）
     */
    public JavaCG2MemoryRecordSource(String content, JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        this(content, fileTypeEnum == null ? null : fileTypeEnum.getSchema());
    }

    /**
     * @param content 收集的记录内容
     * @param schema  结果文件对应的列结构，可为null
     */
    public JavaCG2MemoryRecordSource(String content, JavaCG2OutputFileSchema schema) {
        super(schema);
        this.content = content;
    }

    @Override
    public BufferedReader genBufferedReader() throws IOException {
        return skipHeaderLine(new BufferedReader(new StringReader(content)));
    }
}
