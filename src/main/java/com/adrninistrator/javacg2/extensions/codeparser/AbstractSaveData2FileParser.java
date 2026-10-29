package com.adrninistrator.javacg2.extensions.codeparser;

import com.adrninistrator.javacg2.writer.JavaCG2OutputAdapter;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSink;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author adrninistrator
 * @date 2023/3/13
 * @description: 解析并将结果通过输出适配器写出的类（对jar文件中的其他文件解析）
 */
public abstract class AbstractSaveData2FileParser implements JarEntryOtherFileParser {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSaveData2FileParser.class);

    // 保存记录Sink，一个
    private JavaCG2RecordSink recordSink;

    /**
     * 保存记录Sink，多个
     * key  文件名
     * value    对应的记录Sink
     */
    private Map<String, JavaCG2RecordSink> recordSinkMap;

    /**
     * 返回当前生成的文件名
     * 需要在子类中实现该方法
     *
     * @return
     */
    public String chooseFileName() {
        return null;
    }

    /**
     * 返回当前生成的文件名，多个
     * 需要在子类中实现该方法
     *
     * @return
     */
    public String[] chooseFileNames() {
        return null;
    }

    /**
     * 初始化
     *
     * @param outputAdapter 输出适配器
     */
    public boolean init(JavaCG2OutputAdapter outputAdapter) {
        try {
            String[] fileNames = chooseFileNames();
            if (fileNames != null) {
                // 输出多个文件
                recordSinkMap = new HashMap<>();
                for (String fileName : fileNames) {
                    if (recordSinkMap.containsKey(fileName)) {
                        logger.error("当前类定义了重复的文件名 {} {}", this.getClass().getName(), fileName);
                        return false;
                    }
                    recordSinkMap.put(fileName, outputAdapter.genOtherRecordSink(fileName));
                }
                return true;
            }
            String fileName = chooseFileName();
            if (StringUtils.isNotBlank(fileName)) {
                // 输出一个文件
                recordSink = outputAdapter.genOtherRecordSink(fileName);
                return true;
            }
            logger.error("当前方法未指定需要处理的文件名 {}", this.getClass().getName());
            return false;
        } catch (Exception e) {
            logger.error("error ", e);
            return false;
        }
    }

    /**
     * 将数据写入记录Sink，仅写一个文件时使用
     *
     * @param data
     * @throws IOException
     */
    public void writeData2File(String... data) throws IOException {
        recordSink.writeRow(data);
    }

    /**
     * 将数据写入记录Sink，写入多个文件时使用
     *
     * @param fileName
     * @param data
     * @throws IOException
     */
    public void writeData2Files(String fileName, String... data) throws IOException {
        JavaCG2RecordSink tmpRecordSink = recordSinkMap.get(fileName);
        tmpRecordSink.writeRow(data);
    }

    /**
     * 关闭
     */
    public void close() {
        try {
            if (recordSinkMap != null) {
                for (JavaCG2RecordSink tmpRecordSink : recordSinkMap.values()) {
                    tmpRecordSink.close();
                }
            }
            if (recordSink != null) {
                recordSink.close();
            }
        } catch (Exception e) {
            logger.error("error ", e);
        }
    }
}
