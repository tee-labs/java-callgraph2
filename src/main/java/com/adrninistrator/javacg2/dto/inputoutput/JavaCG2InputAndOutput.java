package com.adrninistrator.javacg2.dto.inputoutput;

import com.adrninistrator.javacg2.conf.JavaCG2ConfInfo;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.dto.output.JavaCG2OtherRunResult;
import com.adrninistrator.javacg2.dto.output.JavaCG2OutputInfo;
import com.adrninistrator.javacg2.el.manager.JavaCG2ElManager;
import lombok.Getter;
import lombok.Setter;

/**
 * @author adrninistrator
 * @date 2025/2/2
 * @description: 保存需要传递的输入与输出数据
 */
@Getter
public class JavaCG2InputAndOutput {

    @Setter
    private JavaCG2ConfInfo javaCG2ConfInfo;

    @Setter
    private JavaCG2ConfigureWrapper javaCG2ConfigureWrapper;

    @Setter
    private JavaCG2ElManager javaCG2ElManager;

    @Setter
    private JavaCG2OutputInfo javaCG2OutputInfo;

    @Setter
    private JavaCG2OtherRunResult javaCG2OtherRunResult;
}
