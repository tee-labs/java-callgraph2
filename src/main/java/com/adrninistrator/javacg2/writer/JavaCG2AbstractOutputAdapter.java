package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchema;

import java.io.IOException;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 输出适配器基类，定义表头行的统一写出规则
 * 各输出适配器实现类（文件实现、内存实现等）通过当前类保证表头行的写出行为一致：
 * 仅当开启表头开关（与output.file.header.enable参数含义一致）、文件类型定义了列结构且允许表头时，在结果文件首行写入表头
 */
public abstract class JavaCG2AbstractOutputAdapter implements JavaCG2OutputAdapter {

    // 是否在结果文件首行输出表头，与output.file.header.enable参数的含义一致
    protected final boolean outputFileHeaderEnable;

    /**
     * @param outputFileHeaderEnable 是否在结果文件首行输出表头
     */
    protected JavaCG2AbstractOutputAdapter(boolean outputFileHeaderEnable) {
        this.outputFileHeaderEnable = outputFileHeaderEnable;
    }

    // 需要时在结果文件首行写入表头，仅当开启表头、文件类型定义了列结构且允许表头时写入
    protected void writeHeaderIfEnable(JavaCG2RecordSink recordSink, JavaCG2OutputFileSchema schema) throws IOException {
        if (schema == null || !schema.isHeaderSupported() || !outputFileHeaderEnable) {
            return;
        }
        recordSink.writeRow(schema.getColumns());
    }
}
