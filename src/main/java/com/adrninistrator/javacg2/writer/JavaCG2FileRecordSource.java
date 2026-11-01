package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.dto.output.JavaCG2OutputInfo;
import com.adrninistrator.javacg2.util.JavaCG2FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 从结果文件读回记录数据的数据来源实现类，由输出根目录与文件后缀名定位结果文件
 */
public class JavaCG2FileRecordSource extends JavaCG2AbstractRecordSource {

    // 结果文件路径
    private final String filePath;

    /**
     * @param javaCG2OutputInfo 处理结果信息
     * @param fileTypeEnum      结果文件类型
     */
    public JavaCG2FileRecordSource(JavaCG2OutputInfo javaCG2OutputInfo, JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        this(javaCG2OutputInfo.getMainFilePath(fileTypeEnum), fileTypeEnum);
    }

    /**
     * @param outputDirPath 输出根目录
     * @param outputFileExt 输出文件后缀名
     * @param fileTypeEnum  结果文件类型
     */
    public JavaCG2FileRecordSource(String outputDirPath, String outputFileExt, JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        this(JavaCG2FileUtil.genFilePath(JavaCG2FileUtil.addSeparator4FilePath(outputDirPath), fileTypeEnum.getFileName(), outputFileExt), fileTypeEnum);
    }

    /**
     * @param filePath     结果文件路径
     * @param fileTypeEnum 结果文件类型
     */
    public JavaCG2FileRecordSource(String filePath, JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        super(fileTypeEnum.getSchema());
        this.filePath = filePath;
    }

    // 获取结果文件路径
    public String getFilePath() {
        return filePath;
    }

    @Override
    public BufferedReader genBufferedReader() throws IOException {
        if (!new File(filePath).exists()) {
            // 结果文件不存在
            return null;
        }
        return skipHeaderLine(JavaCG2FileUtil.genBufferedReader(filePath));
    }
}
