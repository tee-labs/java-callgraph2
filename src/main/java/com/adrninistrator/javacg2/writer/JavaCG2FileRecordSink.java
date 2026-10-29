package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.util.JavaCG2FileUtil;

import java.io.IOException;
import java.io.Writer;

/**
 * @author adrninistrator
 * @date 2026/10/29
 * @description: 将记录写入文件的Sink实现类，一条记录中的各列以tab分隔写入一行，与原有文件写入行为一致
 */
public class JavaCG2FileRecordSink implements JavaCG2RecordSink {

    private final Writer writer;

    /**
     * @param writer 用于写文件的Writer类
     */
    public JavaCG2FileRecordSink(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void writeRow(String... columns) throws IOException {
        JavaCG2FileUtil.write2FileWithTab(writer, columns);
    }

    @Override
    public void writeRaw(String data) throws IOException {
        writer.write(data);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
