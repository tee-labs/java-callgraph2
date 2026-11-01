package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.util.JavaCG2FileUtil;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 将记录写入内存的Sink实现类，收集的记录内容格式与写入结果文件的内容一致（各列以tab分隔，每行一条记录）
 * 供JavaCG2MemoryOutputAdapter使用，结果数据保存在内存中供同进程直接消费，不写文件
 * close()方法是封版边界，关闭后不允许再写入，但已收集的数据仍可通过getContent方法获取
 */
public class JavaCG2MemoryRecordSink implements JavaCG2RecordSink {

    // 收集记录内容使用的Writer
    private final StringWriter stringWriter = new StringWriter();

    // 是否已关闭，关闭后不允许再写入
    private boolean closed = false;

    @Override
    public void writeRow(String... columns) throws IOException {
        checkNotClosed();
        // 与文件实现使用相同的写出行格式，各列以tab分隔，行尾换行
        JavaCG2FileUtil.write2FileWithTab(stringWriter, columns);
    }

    @Override
    public void writeRaw(String data) throws IOException {
        checkNotClosed();
        stringWriter.write(data);
    }

    @Override
    public void close() {
        // 内存Sink的close即封版，之后不允许再写入
        closed = true;
    }

    // 获取收集的全部记录内容
    public String getContent() {
        return stringWriter.toString();
    }

    // 关闭后不允许再写入
    private void checkNotClosed() throws IOException {
        if (closed) {
            throw new IOException("内存记录Sink已关闭，不允许再写入");
        }
    }
}
