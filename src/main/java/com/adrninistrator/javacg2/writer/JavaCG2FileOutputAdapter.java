package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.dto.output.JavaCG2OutputInfo;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;

import java.io.IOException;

/**
 * @author adrninistrator
 * @date 2026/10/29
 * @description: 将记录写入文件的输出适配器实现类，生成的文件保存在输出目录中
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
        return new JavaCG2FileRecordSink(JavaCG2FileUtil.genBufferedWriter(javaCG2OutputInfo.getMainFilePath(fileTypeEnum)));
    }

    @Override
    public JavaCG2RecordSink genOtherRecordSink(String fileName) throws IOException {
        String filePath = JavaCG2FileUtil.genFilePath(javaCG2OutputInfo.getOutputDirPath(), fileName, javaCG2OutputInfo.getOutputFileExt());
        return new JavaCG2FileRecordSink(JavaCG2FileUtil.genBufferedWriter(filePath));
    }
}
