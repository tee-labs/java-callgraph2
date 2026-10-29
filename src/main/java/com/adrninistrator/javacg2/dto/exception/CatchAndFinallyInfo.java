package com.adrninistrator.javacg2.dto.exception;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2023/12/28
 * @description: Exception table中某个try对应的catch、finally信息
 */
@Getter
public class CatchAndFinallyInfo {

    private final List<CatchInfo> catchInfoList = new ArrayList<>();

    @Setter
    private FinallyInfo finallyInfo;
}
