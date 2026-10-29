package com.adrninistrator.javacg2.dto.jar;

import lombok.Getter;
import lombok.Setter;

import java.io.File;

/**
 * @author adrninistrator
 * @date 2025/7/6
 * @description: 合并jar文件的结果
 */
@Getter
public class MergeJarResult {

    // 合并后的jar文件
    @Setter
    private File mergeJarFile;

    // 合并生成的fat jar文件
    @Setter
    private File fatJarFile;

    public static MergeJarResult genOne(File mergeJarFile) {
        MergeJarResult mergeJarResult = new MergeJarResult();
        mergeJarResult.setMergeJarFile(mergeJarFile);
        return mergeJarResult;
    }

    public static MergeJarResult genTwo(File mergeJarFile, File fatJarFile) {
        MergeJarResult mergeJarResult = new MergeJarResult();
        mergeJarResult.setMergeJarFile(mergeJarFile);
        mergeJarResult.setFatJarFile(fatJarFile);
        return mergeJarResult;
    }
}
