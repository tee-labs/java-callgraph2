package com.adrninistrator.javacg2.writer;

import com.adrninistrator.javacg2.common.enums.JavaCG2OutPutFileTypeEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author adrninistrator
 * @date 2026/11/1
 * @description: 主结果文件记录Sink统一持有器
 * 以“文件类型枚举到记录Sink”的映射在JavaCG2Entry与JarEntryHandleParser、ClassHandler、各方法处理类之间传递记录Sink
 * 替代在各类之间逐个设置记录Sink的方式，新增输出文件时仅需在文件类型枚举与MAIN_OUTPUT_FILE_TYPES中登记，由实际写入的处理类按需取用
 * 持有器不拥有记录Sink的生命周期：记录Sink由JavaCG2OutputAdapter创建，在JavaCG2Entry的try-with-resources中统一关闭，持有器仅作为传递载体
 */
public class JavaCG2RecordSinkHolder implements AutoCloseable {

    // JavaCG2Entry写出的全部主结果文件对应的文件类型枚举，顺序与记录Sink的创建顺序一致
    private static final JavaCG2OutPutFileTypeEnum[] MAIN_OUTPUT_FILE_TYPES = {
            JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION,
            JavaCG2OutPutFileTypeEnum.OPFTE_JAVACG2_CONFIG,
            JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO,
            JavaCG2OutPutFileTypeEnum.OPFTE_DUP_CLASS_INFO,
            JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_REFERENCE,
            JavaCG2OutPutFileTypeEnum.OPFTE_DUP_CLASS_REFERENCE,
            JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_SIGNATURE_GENERICS_TYPE,
            JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_EXT_IMPL_GENERICS_TYPE,
            JavaCG2OutPutFileTypeEnum.OPFTE_EXTENDS_IMPL,
            JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_ANNOTATION,
            JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_INFO,
            JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_GENERICS_TYPE,
            JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_RELATIONSHIP,
            JavaCG2OutPutFileTypeEnum.OPFTE_FIELD_USAGE_OTHER,
            JavaCG2OutPutFileTypeEnum.OPFTE_GET_METHOD,
            JavaCG2OutPutFileTypeEnum.OPFTE_INNER_CLASS,
            JavaCG2OutPutFileTypeEnum.OPFTE_JAR_INFO,
            JavaCG2OutPutFileTypeEnum.OPFTE_LAMBDA_METHOD_INFO,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_ANNOTATION,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_ARGUMENT,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_ARG_ANNOTATION,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_ARG_GENERICS_TYPE,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_INFO,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_METHOD_CALL_RETURN,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_STATIC_FIELD,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_NON_STATIC_FIELD,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_FIELD_ACTUAL_TYPE,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_STATIC_FIELD_MCR,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_RETURN_ARG_SEQ,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_RETURN_CALL_ID,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_RETURN_CONST_VALUE,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_RETURN_FIELD_INFO,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL_RAW_CALLEE,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_INFO,
            JavaCG2OutPutFileTypeEnum.OPFTE_DUP_METHOD_INFO,
            JavaCG2OutPutFileTypeEnum.OPFTE_DUP_FIELD_INFO,
            JavaCG2OutPutFileTypeEnum.OPFTE_ENUM_INIT_ARG_FIELD,
            JavaCG2OutPutFileTypeEnum.OPFTE_ENUM_INIT_ASSIGN_INFO,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_LINE_NUMBER,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_RETURN_GENERICS_TYPE,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CATCH,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_FINALLY,
            JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_THROW,
            JavaCG2OutPutFileTypeEnum.OPFTE_PACKAGE_INFO,
            JavaCG2OutPutFileTypeEnum.OPFTE_SET_METHOD,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_BEAN,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_SCAN_PACKAGE_JAVA,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ASPECT_JAVA,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_POINTCUT_JAVA,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_AOP_ADVICE_JAVA,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_DI_FIELD,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_CONTROLLER,
            JavaCG2OutPutFileTypeEnum.OPFTE_SPRING_TASK_JAVA,
            JavaCG2OutPutFileTypeEnum.OPFTE_SF_FIELD_METHOD_CALL,
    };

    // 文件类型枚举到记录Sink的映射，按登记顺序保存
    private final Map<JavaCG2OutPutFileTypeEnum, JavaCG2RecordSink> recordSinkMap = new LinkedHashMap<>();

    /**
     * 经输出适配器批量创建全部主结果文件的记录Sink并登记到新的持有器中
     * 创建与登记的顺序均与MAIN_OUTPUT_FILE_TYPES的顺序一致
     * 任一记录Sink创建失败时，关闭已创建的记录Sink后抛出异常，与在try-with-resources中逐个声明时的行为一致
     *
     * @param outputAdapter 输出适配器
     * @return 记录Sink持有器
     * @throws IOException 记录Sink创建失败时抛出
     */
    public static JavaCG2RecordSinkHolder genMainRecordSinkHolder(JavaCG2OutputAdapter outputAdapter) throws IOException {
        JavaCG2RecordSinkHolder recordSinkHolder = new JavaCG2RecordSinkHolder();
        try {
            for (JavaCG2OutPutFileTypeEnum fileTypeEnum : MAIN_OUTPUT_FILE_TYPES) {
                recordSinkHolder.put(fileTypeEnum, outputAdapter.genMainRecordSink(fileTypeEnum));
            }
        } catch (Exception e) {
            try {
                // 关闭已创建的记录Sink，关闭时出现的异常作为被抑制的异常加入
                recordSinkHolder.close();
            } catch (Exception e2) {
                e.addSuppressed(e2);
            }
            throw e;
        }
        return recordSinkHolder;
    }

    /**
     * 登记文件类型枚举对应的记录Sink
     *
     * @param fileTypeEnum 文件类型枚举
     * @param recordSink   记录Sink
     */
    public void put(JavaCG2OutPutFileTypeEnum fileTypeEnum, JavaCG2RecordSink recordSink) {
        if (recordSinkMap.containsKey(fileTypeEnum)) {
            throw new IllegalStateException("不允许重复登记文件类型对应的记录Sink " + fileTypeEnum);
        }
        recordSinkMap.put(fileTypeEnum, recordSink);
    }

    /**
     * 获取文件类型枚举对应的记录Sink
     *
     * @param fileTypeEnum 文件类型枚举
     * @return 记录Sink，未登记时返回null
     */
    public JavaCG2RecordSink getRecordSink(JavaCG2OutPutFileTypeEnum fileTypeEnum) {
        return recordSinkMap.get(fileTypeEnum);
    }

    /**
     * 按登记的逆序关闭全部已登记的记录Sink，与在try-with-resources中按声明顺序逐个关闭的行为一致
     * 某一记录Sink关闭失败不影响其余记录Sink的关闭
     *
     * @throws IOException 记录Sink关闭失败时抛出
     */
    @Override
    public void close() throws IOException {
        List<JavaCG2RecordSink> recordSinkList = new ArrayList<>(recordSinkMap.values());
        recordSinkMap.clear();
        IOException firstException = null;
        for (int i = recordSinkList.size() - 1; i >= 0; i--) {
            try {
                recordSinkList.get(i).close();
            } catch (IOException e) {
                if (firstException == null) {
                    firstException = e;
                } else {
                    firstException.addSuppressed(e);
                }
            }
        }
        if (firstException != null) {
            throw firstException;
        }
    }
}
