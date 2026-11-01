package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.common.JavaCG2OutputFileSchema;
import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.dto.output.JavaCG2OutputInfo;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;

import java.io.IOException;

/**
 * @author adrninistrator
 * @date 2026/10/29
 * @description: 将记录写入文件的输出适配器实现类，生成的文件保存在输出目录中
 * 开启output.file.header.enable参数时，为定义了列结构且允许表头的主结果文件在首行写入表头
 */
public class JavaCG2FileOutputAdapter implements JavaCG2OutputAdapter {

    private final JavaCG2OutputInfo javaCG2OutputInfo;

    /**
     * @param javaCG2OutputInfo 处理结果信息
     */
    public JavaCG2FileOutputAdapter(JavaCG2OutputInfo javaCG2OutputInfo) {
        this.javaCG2OutputInfo = javaCG2OutputInfo;
    }

    @Override
    public JavaCG2RecordSink genMainRecordSink(JavaCG2OutPutFileTypeEnum fileTypeEnum) throws IOException {
        JavaCG2RecordSink recordSink = new JavaCG2FileRecordSink(JavaCG2FileUtil.genBufferedWriter(javaCG2OutputInfo.getMainFilePath(fileTypeEnum)));
        writeHeaderIfEnable(recordSink, fileTypeEnum.getSchema());
        return recordSink;
    }

    @Override
    public JavaCG2RecordSink genOtherRecordSink(String fileName) throws IOException {
        String filePath = JavaCG2FileUtil.genFilePath(javaCG2OutputInfo.getOutputDirPath(), fileName, javaCG2OutputInfo.getOutputFileExt());
        JavaCG2RecordSink recordSink = new JavaCG2FileRecordSink(JavaCG2FileUtil.genBufferedWriter(filePath));
        // 文件名与文件类型枚举匹配时，同样在需要时写入表头（如内置Spring XML解析类生成的其他类型结果文件）
        JavaCG2OutPutFileTypeEnum fileTypeEnum = JavaCG2OutPutFileTypeEnum.getFromFileName(fileName);
        if (fileTypeEnum != null) {
            writeHeaderIfEnable(recordSink, fileTypeEnum.getSchema());
        }
        return recordSink;
    }

    // 需要时在结果文件首行写入表头，仅当开启表头参数、文件类型定义了列结构且允许表头时写入
    private void writeHeaderIfEnable(JavaCG2RecordSink recordSink, JavaCG2OutputFileSchema schema) throws IOException {
        if (schema == null || !schema.isHeaderSupported() || !javaCG2OutputInfo.isOutputFileHeaderEnable()) {
            return;
        }
        recordSink.writeRow(schema.getColumns());
    }
}
