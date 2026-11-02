package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;

import java.io.IOException;
import java.io.Writer;

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

    /**
     * 生成自由格式文本文件对应的Writer，用于写出无列结构的自由格式内容（如配置参数说明的markdown文件、EL表达式忽略数据文件、方法处理耗时日志文件等）
     * 与记录Sink不同，通过当前方法获得的Writer直接写出原始文本内容，不按列写出，也不写表头
     * 文件实现生成真实文件对应的Writer，内存实现将内容写入内存缓冲，可通过getOtherRecordContent、genOtherRecordSource等方法读回
     *
     * @param fileName 文件名，包含文件后缀名（如"_javacg2_all_config.md"、"el_ignore_data.log"）
     * @return 自由格式文本文件对应的Writer
     * @throws IOException 创建失败时抛出异常
     */
    Writer genOtherFileWriter(String fileName) throws IOException;

    /**
     * 是否为纯内存输出模式（不写文件）
     * 默认false；纯内存输出实现（如JavaCG2MemoryOutputAdapter）覆写当前方法返回true
     * 纯内存输出时，输出目录无法创建不会导致处理失败
     *
     * @return true: 纯内存输出模式 false: 非纯内存输出模式
     */
    default boolean isMemoryOnly() {
        return false;
    }

    /**
     * 生成主结果文件对应的记录数据来源，用于在写出后读回结果数据（如Jar兼容性检查、JarDiff比较）
     * 默认不支持读回，返回null；支持读回的实现类（如JavaCG2FileOutputAdapter、JavaCG2MemoryOutputAdapter）覆写当前方法
     * 不支持读回时，Jar兼容性检查会跳过对应结果文件的检查
     *
     * @param fileTypeEnum 主结果文件类型
     * @return 记录数据来源，不支持读回时返回null
     */
    default JavaCG2RecordSource genRecordSource(JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        return null;
    }
}
