package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 将记录写入内存的输出适配器实现类，结果数据保存在内存中供同进程直接消费，不写文件
 * 适用于将当前项目作为库嵌入使用的场景：解析结果无需落盘，写出的结果数据可通过genRecordSource等方法读回，
 * 也可通过getMainRecordContent等方法按结果文件类型获取全部内容
 * 通过genOtherFileWriter方法写出的自由格式文本文件（如配置参数说明的markdown文件等）同样保存在内存中，不依赖输出目录
 * 表头行的写出规则与JavaCG2FileOutputAdapter一致，遵守output.file.header.enable参数与schema.headerSupported规则
 * genMainRecordSink与genOtherRecordSink每次调用都会生成新的内存Sink并替换此前同名的数据，与文件实现覆盖同名文件的行为一致
 */
public class JavaCG2MemoryOutputAdapter extends JavaCG2AbstractOutputAdapter {

    // 各主结果文件对应的内存Sink；key为结果文件类型
    private final Map<JavaCG2OutPutFileTypeEnum, JavaCG2MemoryRecordSink> mainRecordSinkMap = new HashMap<>();

    // 其他结果文件对应的内存Sink（如扩展机制生成的文件）；key为文件名，不需要指定文件后缀
    private final Map<String, JavaCG2MemoryRecordSink> otherRecordSinkMap = new HashMap<>();

    public JavaCG2MemoryOutputAdapter() {
        this(false);
    }

    /**
     * @param outputFileHeaderEnable 是否在结果文件首行输出表头，与output.file.header.enable参数的含义一致
     */
    public JavaCG2MemoryOutputAdapter(boolean outputFileHeaderEnable) {
        super(outputFileHeaderEnable);
    }

    @Override
    public JavaCG2RecordSink genMainRecordSink(JavaCG2OutPutFileTypeEnum fileTypeEnum) throws IOException {
        JavaCG2MemoryRecordSink recordSink = new JavaCG2MemoryRecordSink();
        mainRecordSinkMap.put(fileTypeEnum, recordSink);
        writeHeaderIfEnable(recordSink, fileTypeEnum.getSchema());
        return recordSink;
    }

    @Override
    public JavaCG2RecordSink genOtherRecordSink(String fileName) throws IOException {
        JavaCG2MemoryRecordSink recordSink = new JavaCG2MemoryRecordSink();
        otherRecordSinkMap.put(fileName, recordSink);
        // 文件名与文件类型枚举匹配时，同样在需要时写入表头，与文件实现的行为一致
        JavaCG2OutPutFileTypeEnum fileTypeEnum = JavaCG2OutPutFileTypeEnum.getFromFileName(fileName);
        if (fileTypeEnum != null) {
            writeHeaderIfEnable(recordSink, fileTypeEnum.getSchema());
        }
        return recordSink;
    }

    @Override
    public Writer genOtherFileWriter(String fileName) {
        // 与文件实现在创建Writer时即生成文件的行为一致，立即登记对应的内存Sink，未写入内容时读回为空串
        JavaCG2MemoryRecordSink recordSink = new JavaCG2MemoryRecordSink();
        otherRecordSinkMap.put(fileName, recordSink);
        // Writer将数据直接写入内存Sink，不经过缓冲，写入后无需flush即可读回
        return new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                recordSink.writeRaw(new String(cbuf, off, len));
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
                // 关闭时封版，之后不允许再写入，已收集的数据仍可通过getContent方法获取
                recordSink.close();
            }
        };
    }

    @Override
    public boolean isMemoryOnly() {
        // 纯内存输出模式不写文件，输出目录无法创建不会导致处理失败
        return true;
    }

    @Override
    public JavaCG2RecordSource genRecordSource(JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        JavaCG2MemoryRecordSink recordSink = mainRecordSinkMap.get(fileTypeEnum);
        if (recordSink == null) {
            // 未生成对应的结果文件
            return null;
        }
        return new JavaCG2MemoryRecordSource(recordSink.getContent(), fileTypeEnum);
    }

    /**
     * 生成其他结果文件对应的记录数据来源（扩展机制生成的文件等）
     *
     * @param fileName 文件名，不需要指定文件后缀
     * @return 记录数据来源，未生成对应的结果文件时返回null
     */
    public JavaCG2RecordSource genOtherRecordSource(String fileName) {
        JavaCG2MemoryRecordSink recordSink = otherRecordSinkMap.get(fileName);
        if (recordSink == null) {
            // 未生成对应的结果文件
            return null;
        }
        return new JavaCG2MemoryRecordSource(recordSink.getContent(), JavaCG2OutPutFileTypeEnum.getFromFileName(fileName));
    }

    /**
     * 获取指定主结果文件收集的全部内容（包含表头行，如有），格式与写入结果文件的内容一致
     *
     * @param fileTypeEnum 结果文件类型
     * @return 收集的全部记录内容，未生成对应的结果文件时返回null
     */
    public String getMainRecordContent(JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        JavaCG2MemoryRecordSink recordSink = mainRecordSinkMap.get(fileTypeEnum);
        return recordSink == null ? null : recordSink.getContent();
    }

    /**
     * 获取指定其他结果文件收集的全部内容（包含表头行，如有），格式与写入结果文件的内容一致
     * 通过genOtherFileWriter方法写出的自由格式文本文件（文件名包含后缀名）的内容也通过当前方法读回
     *
     * @param fileName 文件名，不需要指定文件后缀；自由格式文本文件需要指定包含后缀名的文件名
     * @return 收集的全部记录内容，未生成对应的结果文件时返回null
     */
    public String getOtherRecordContent(String fileName) {
        JavaCG2MemoryRecordSink recordSink = otherRecordSinkMap.get(fileName);
        return recordSink == null ? null : recordSink.getContent();
    }
}
