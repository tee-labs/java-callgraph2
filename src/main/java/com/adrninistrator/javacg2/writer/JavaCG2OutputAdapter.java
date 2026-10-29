package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;

import java.io.IOException;

/**
 * @author adrninistrator
 * @date 2026/10/29
 * @description: 输出适配器接口，为每种结果文件提供对应的记录Sink
 * 每种结果文件可使用不同的实现；通过JavaCG2Entry的setOutputAdapter方法在run()之前注册自定义实现
 * 默认使用JavaCG2FileOutputAdapter将记录写入文件，后续可实现将记录写数据库等实现
 */
public interface JavaCG2OutputAdapter {

    /**
     * 生成主结果文件对应的记录Sink
     *
     * @param fileTypeEnum 主结果文件类型
     * @return
     * @throws IOException 创建失败时抛出异常
     */
    JavaCG2RecordSink genMainRecordSink(JavaCG2OutPutFileTypeEnum fileTypeEnum) throws IOException;

    /**
     * 生成其他结果文件对应的记录Sink（扩展机制生成的文件）
     *
     * @param fileName 文件名，不需要指定文件后缀
     * @return
     * @throws IOException 创建失败时抛出异常
     */
    JavaCG2RecordSink genOtherRecordSink(String fileName) throws IOException;
}
