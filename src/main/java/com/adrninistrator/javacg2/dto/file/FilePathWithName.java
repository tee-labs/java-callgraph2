package com.adrninistrator.javacg2.dto.file;

import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2025/7/27
 * @description: 文件路径及文件名称
 */
@Getter
public class FilePathWithName {

    @Setter
    private String filePath;

    @Setter
    private String fileName;
}
