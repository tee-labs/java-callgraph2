package com.adrninistrator.javacg2.writer;

import java.io.IOException;

/**
 * @author adrninistrator
 * @date 2026/10/29
 * @description: 结果记录Sink接口，一条记录对应结果文件中的一行（各列以tab分隔）
 * 结果数据的统一写出接口：默认实现将记录写入文件，后续可扩展为写数据库等其他实现
 * close()方法是写出结果的提交边界，由调用方在处理结束时统一关闭
 */
public interface JavaCG2RecordSink extends AutoCloseable {

    /**
     * 写入一条记录
     *
     * @param columns 一条记录中的各列
     * @throws IOException 写入失败时抛出异常
     */
    void writeRow(String... columns) throws IOException;

    /**
     * 写入原始内容，内容不做任何加工
     * 内容为按行组织的数据块，每行以换行符结尾，与多次writeRow的效果一致
     *
     * @param data
     * @throws IOException 写入失败时抛出异常
     */
    void writeRaw(String data) throws IOException;

    @Override
    void close() throws IOException;
}
