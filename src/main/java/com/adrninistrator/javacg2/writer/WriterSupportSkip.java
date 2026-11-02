package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.util.JavaCG2FileUtil;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

/**
 * @author adrninistrator
 * @date 2023/4/26
 * @description: 包装后的Writer，若未写文件则不创建文件
 */
public class WriterSupportSkip implements Closeable {

    private final String filePath;

    // 输出适配器与自由格式文本文件的文件名，首次写入时通过输出适配器创建对应的Writer，与指定文件路径的方式二选一
    private final JavaCG2OutputAdapter outputAdapter;
    private final String fileName;

    private final boolean buffered;

    private Writer writer;

    public WriterSupportSkip(String filePath) {
        this(filePath, true);
    }

    public WriterSupportSkip(String filePath, boolean buffered) {
        this.filePath = filePath;
        this.outputAdapter = null;
        this.fileName = null;
        this.buffered = buffered;
    }

    /**
     * @param outputAdapter 输出适配器，首次写入时通过genOtherFileWriter方法创建自由格式文本文件对应的Writer
     * @param fileName      文件名，包含文件后缀名
     * @param buffered      是否缓存写入的内容，false时每次写入后flush
     */
    public WriterSupportSkip(JavaCG2OutputAdapter outputAdapter, String fileName, boolean buffered) {
        this.filePath = null;
        this.outputAdapter = outputAdapter;
        this.fileName = fileName;
        this.buffered = buffered;
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    public void write(String data) throws IOException {
        if (writer == null) {
            writer = genWriter();
        }
        writer.write(data);
        if (!buffered) {
            // 不缓存时，每次写文件后flush
            writer.flush();
        }
    }

    // 首次写入时生成用于写出的Writer，未写入时不创建文件（或内存缓冲）
    private Writer genWriter() throws IOException {
        if (outputAdapter != null) {
            return outputAdapter.genOtherFileWriter(fileName);
        }
        return JavaCG2FileUtil.genBufferedWriter(filePath);
    }
}
