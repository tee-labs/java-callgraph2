package com.adrninistrator.javacg2.dto.instruction.parseresult;

import com.adrninistrator.javacg2.dto.element.variable.VariableElement;
import lombok.Getter;

/**
 * @author adrninistrator
 * @date 2024/1/2
 * @description: ATHROW指令解析结果
 */
@Getter
public class AThrowParseResult extends BaseInstructionParseResult {

    // 抛出的异常
    private final VariableElement throwElement;

    public AThrowParseResult(VariableElement throwElement) {
        this.throwElement = throwElement;
    }
}
