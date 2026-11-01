package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchema;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 结果记录数据来源基类，提供读回时跳过表头行的处理
 * 表头行的跳过逻辑统一在当前类中处理，读回方（如Jar兼容性检查、JarDiff比较）无需各自处理表头行
 */
public abstract class JavaCG2AbstractRecordSource implements JavaCG2RecordSource {

    // 结果文件对应的列结构，未定义列结构的结果文件类型为null
    protected final JavaCG2OutputFileSchema schema;

    /**
     * @param schema 结果文件对应的列结构，未定义列结构时为null
     */
    protected JavaCG2AbstractRecordSource(JavaCG2OutputFileSchema schema) {
        this.schema = schema;
    }

    // 对生成的BufferedReader跳过首部的表头行，首行不是表头行时通过reset回退，保证数据行不丢失
    protected BufferedReader skipHeaderLine(BufferedReader bufferedReader) throws IOException {
        if (schema == null) {
            return bufferedReader;
        }
        // mark的读取限制需大于表头行的长度，表头行为各列名称以tab分隔，长度远小于当前值
        bufferedReader.mark(8192);
        String firstLine = bufferedReader.readLine();
        if (firstLine == null || !schema.isHeaderLine(firstLine)) {
            // 没有数据行，或首行不是表头行，回退到起始位置
            bufferedReader.reset();
        }
        return bufferedReader;
    }
}
