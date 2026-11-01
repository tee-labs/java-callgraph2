package test.writer;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;
import com.adrninistrator.javacg2.writer.JavaCG2MemoryOutputAdapter;
import com.adrninistrator.javacg2.writer.JavaCG2MemoryRecordSink;
import com.adrninistrator.javacg2.writer.JavaCG2OutputAdapter;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSink;
import com.adrninistrator.javacg2.writer.JavaCG2RecordSinkHolder;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 主结果文件记录Sink统一持有器的单元级测试
 * 验证put/get基本行为、未登记的文件类型返回null、经输出适配器批量创建并登记后各文件类型可取回且内容写出正常、关闭与创建失败时的行为
 */
public class TestJavaCG2RecordSinkHolder {

    // 不由JavaCG2Entry经持有器批量创建的文件类型：非法值、方法处理耗时日志、通过XML定义的Spring文件、Jar兼容性检查结果文件
    private static final Set<JavaCG2OutPutFileTypeEnum> NOT_IN_HOLDER_FILE_TYPE_SET = new HashSet<>(Arrays.asList(
            JavaCG2OutPutFileTypeEnum.OPFTE_ILLEGAL,
            JavaCG2OutPutFileTypeEnum.OPFTE_LOG_METHOD_SPEND_TIME,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_TASK_XML,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ASPECT_XML,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_POINTCUT_XML,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ADVICE_XML,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_SCAN_PACKAGE_XML,
            JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_CLASS_REFERENCE_MISSING,
            JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_METHOD_CALL_MISSING,
            JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_FIELD_USAGE_MISSING,
            JavaCG2OutPutFileTypeEnum.OPFTE_COMPATIBILITY_DUP_CLASS_CONFLICT));

    /**
     * 验证put/get基本行为，put与getRecordSink返回同一实例，重复登记报错
     */
    @Test
    public void testPutAndGet() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter();
        JavaCG2RecordSinkHolder recordSinkHolder = new JavaCG2RecordSinkHolder();
        JavaCG2RecordSink classInfoSink = outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO);
        recordSinkHolder.put(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO, classInfoSink);

        Assert.assertSame("登记后应能取回同一记录Sink实例", classInfoSink, recordSinkHolder.getRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO));

        // 未登记的文件类型返回null
        Assert.assertNull("未登记的文件类型应返回null", recordSinkHolder.getRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL));

        // 重复登记同一文件类型应报错
        try {
            recordSinkHolder.put(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO, outputAdapter.genMainRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO));
            Assert.fail("重复登记同一文件类型应报错");
        } catch (IllegalStateException e) {
            Assert.assertTrue("重复登记的报错应说明不允许重复登记 " + e.getMessage(), e.getMessage().contains("不允许重复登记"));
        }

        recordSinkHolder.close();
    }

    /**
     * 验证经内存输出适配器批量创建并登记后，各主结果文件类型均可取回，不由持有器创建的文件类型返回null
     */
    @Test
    public void testGenMainRecordSinkHolder() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter();
        JavaCG2RecordSinkHolder recordSinkHolder = JavaCG2RecordSinkHolder.genMainRecordSinkHolder(outputAdapter);

        for (JavaCG2OutPutFileTypeEnum fileTypeEnum : JavaCG2OutPutFileTypeEnum.values()) {
            if (NOT_IN_HOLDER_FILE_TYPE_SET.contains(fileTypeEnum)) {
                Assert.assertNull("不应登记非主结果文件的文件类型 " + fileTypeEnum, recordSinkHolder.getRecordSink(fileTypeEnum));
            } else {
                Assert.assertNotNull("应登记全部主结果文件的文件类型 " + fileTypeEnum, recordSinkHolder.getRecordSink(fileTypeEnum));
            }
        }

        recordSinkHolder.close();
    }

    /**
     * 验证经持有器取用的记录Sink可正常写出，关闭持有器后内容可经内存适配器读回
     */
    @Test
    public void testWriteThroughHolder() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter();
        try (JavaCG2RecordSinkHolder recordSinkHolder = JavaCG2RecordSinkHolder.genMainRecordSinkHolder(outputAdapter)) {
            recordSinkHolder.getRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO).writeRow("a.b.Class1", "33", "hash1", "1", "a/b/Class1.class");
            recordSinkHolder.getRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO).writeRow("jar", "1", "D:/tmp/test.jar", "");
            recordSinkHolder.getRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL).writeRaw("1\tY\tA.m()\tB.m():1\n");
        }

        Assert.assertEquals("经持有器写出的内容应可通过内存适配器读回",
                "a.b.Class1\t33\thash1\t1\ta/b/Class1.class\n", outputAdapter.getMainRecordContent(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO));
        Assert.assertEquals("jar_info文件类型的内容应可通过内存适配器读回",
                "jar\t1\tD:/tmp/test.jar\t\n", outputAdapter.getMainRecordContent(JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO));
        Assert.assertEquals("writeRaw写入的内容应可通过内存适配器读回",
                "1\tY\tA.m()\tB.m():1\n", outputAdapter.getMainRecordContent(JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL));
    }

    /**
     * 验证关闭持有器时会关闭全部已登记的记录Sink，关闭后不可再写入
     */
    @Test
    public void testCloseClosesSinks() throws IOException {
        JavaCG2MemoryOutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter();
        JavaCG2RecordSinkHolder recordSinkHolder = JavaCG2RecordSinkHolder.genMainRecordSinkHolder(outputAdapter);
        JavaCG2RecordSink classInfoSink = recordSinkHolder.getRecordSink(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO);
        recordSinkHolder.close();

        // 关闭持有器后，已登记的记录Sink不可再写入
        try {
            classInfoSink.writeRow("a.b.Class1", "33", "hash1", "1", "a/b/Class1.class");
            Assert.fail("关闭持有器后写入应抛出异常");
        } catch (IOException e) {
            Assert.assertTrue("关闭持有器后写入的报错应说明不允许再写入 " + e.getMessage(), e.getMessage().contains("不允许再写入"));
        }

        // 再次关闭持有器不应报错
        recordSinkHolder.close();
    }

    /**
     * 验证批量创建过程中任一记录Sink创建失败时，已创建的记录Sink会被关闭，创建异常向外抛出
     */
    @Test
    public void testGenFailCloseCreatedSinks() throws IOException {
        final List<JavaCG2RecordSink> createdSinkList = new ArrayList<>();
        JavaCG2OutputAdapter outputAdapter = new JavaCG2MemoryOutputAdapter() {
            @Override
            public JavaCG2RecordSink genMainRecordSink(JavaCG2OutPutFileTypeEnum fileTypeEnum) throws IOException {
                if (fileTypeEnum == JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO) {
                    // 模拟创建第3个主结果文件的记录Sink时失败
                    throw new IOException("模拟创建记录Sink失败");
                }
                JavaCG2RecordSink recordSink = super.genMainRecordSink(fileTypeEnum);
                createdSinkList.add(recordSink);
                return recordSink;
            }
        };

        try {
            JavaCG2RecordSinkHolder.genMainRecordSinkHolder(outputAdapter);
            Assert.fail("记录Sink创建失败时应抛出异常");
        } catch (IOException e) {
            Assert.assertTrue("创建失败的报错应为模拟的异常信息 " + e.getMessage(), e.getMessage().contains("模拟创建记录Sink失败"));
        }

        // 创建失败前已生成的记录Sink应被关闭，不可再写入
        Assert.assertFalse("创建失败前应已生成部分记录Sink", createdSinkList.isEmpty());
        for (JavaCG2RecordSink recordSink : createdSinkList) {
            try {
                recordSink.writeRow("a.b.Class1", "33", "hash1", "1", "a/b/Class1.class");
                Assert.fail("创建失败后已生成的记录Sink应已关闭，写入应抛出异常");
            } catch (IOException e) {
                Assert.assertTrue("已关闭的记录Sink写入的报错应说明不允许再写入 " + e.getMessage(), e.getMessage().contains("不允许再写入"));
            }
        }
        Assert.assertTrue("创建失败前生成的记录Sink应为内存记录Sink", createdSinkList.get(0) instanceof JavaCG2MemoryRecordSink);
    }
}
