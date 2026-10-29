package com.adrninistrator.javacg2.builder;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.el.enums.ElStringAnyFunctionEnum;
import com.adrninistrator.javacg2.el.enums.interfaces.ElAllowedVariableInterface;
import com.adrninistrator.javacg2.el.enums.interfaces.ElConfigInterface;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: 表达式开关类Builder的公共逻辑，供JavaCG2MergeFileSwitchBuilder与JavaCG2ParseSwitchBuilder共用
 * 记录待应用的表达式配置，生成合法的Aviator表达式文本（字符串参数自动以单引号包裹，不以分号结尾）
 * 同一个表达式配置枚举多次设置时，以后设置的为准（表达式文本整体替换）
 */
@Slf4j
class ElSwitchConfigHolder {

    // 待应用的表达式配置，key为表达式配置枚举，同一个枚举以后设置的为准
    private final Map<ElConfigInterface, PendingElText> pendingElTextMap = new LinkedHashMap<>();

    // 记录使用字符串比较函数生成的表达式配置，字符串参数自动以单引号包裹
    void putStringAny(ElConfigInterface elConfig, ElStringAnyFunctionEnum stringAnyFunctionEnum, ElAllowedVariableInterface elVariable, String... args) {
        // 检查当前表达式是否允许使用该变量
        checkElVariableAllowed(elConfig, elVariable);
        if (ArrayUtils.isEmpty(args)) {
            throw new JavaCG2ConfigException("不允许传入null或空数组 " + elConfig.getKey());
        }
        String[] quotedArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.isBlank(arg)) {
                throw new JavaCG2ConfigException("参数不允许为空 " + elConfig.getKey());
            }
            if (arg.contains("'")) {
                throw new JavaCG2ConfigException("参数不允许包含单引号 " + elConfig.getKey() + " " + arg);
            }
            // 语义化API自动为字符串参数添加单引号，保证生成的表达式是合法的Aviator表达式
            quotedArgs[i] = "'" + arg + "'";
        }
        putPendingElText(elConfig, PendingElText.genStringAny(elConfig, stringAnyFunctionEnum, elVariable, quotedArgs));
    }

    // 记录原始表达式文本
    void putRaw(ElConfigInterface elConfig, String elText) {
        if (elText == null) {
            throw new JavaCG2ConfigException("表达式文本不允许传入null " + elConfig.getKey());
        }
        putPendingElText(elConfig, PendingElText.genRaw(elConfig, elText));
    }

    // 将当前收集的表达式配置应用到配置参数包装对象
    void applyTo(JavaCG2ConfigureWrapper javaCG2ConfigureWrapper) {
        for (Map.Entry<ElConfigInterface, PendingElText> entry : pendingElTextMap.entrySet()) {
            entry.getValue().applyTo(javaCG2ConfigureWrapper);
        }
    }

    // 获取已设置表达式配置的枚举集合
    Set<ElConfigInterface> getSetElConfigSet() {
        return pendingElTextMap.keySet();
    }

    // 记录待应用的表达式配置，同一个表达式配置枚举以后设置的为准
    private void putPendingElText(ElConfigInterface elConfig, PendingElText pendingElText) {
        log.info("设置表达式开关配置 {} {}", elConfig.getKey(), pendingElText.genPrintInfo());
        pendingElTextMap.put(elConfig, pendingElText);
    }

    // 检查当前表达式配置是否允许使用该变量
    private void checkElVariableAllowed(ElConfigInterface elConfig, ElAllowedVariableInterface elVariable) {
        for (ElAllowedVariableInterface allowedVariable : elConfig.getElAllowedVariableEnums()) {
            if (allowedVariable.getVariableName().equals(elVariable.getVariableName())) {
                return;
            }
        }
        throw new JavaCG2ConfigException("当前表达式不允许使用该变量 " + elConfig.getKey() + " " + elVariable.getVariableName());
    }

    // 待应用的表达式文本
    private static class PendingElText {

        // 对应的表达式配置枚举
        private final ElConfigInterface elConfig;

        // 字符串比较方式枚举，为null时使用原始表达式文本
        private final ElStringAnyFunctionEnum stringAnyFunctionEnum;

        private final ElAllowedVariableInterface elVariable;

        // 以单引号包裹的字符串参数
        private final String[] stringAnyArgs;

        // 原始表达式文本
        private final String rawElText;

        private PendingElText(ElConfigInterface elConfig, ElStringAnyFunctionEnum stringAnyFunctionEnum, ElAllowedVariableInterface elVariable,
                              String[] stringAnyArgs, String rawElText) {
            this.elConfig = elConfig;
            this.stringAnyFunctionEnum = stringAnyFunctionEnum;
            this.elVariable = elVariable;
            this.stringAnyArgs = stringAnyArgs;
            this.rawElText = rawElText;
        }

        private static PendingElText genStringAny(ElConfigInterface elConfig, ElStringAnyFunctionEnum stringAnyFunctionEnum, ElAllowedVariableInterface elVariable,
                                                  String... stringAnyArgs) {
            return new PendingElText(elConfig, stringAnyFunctionEnum, elVariable, stringAnyArgs, null);
        }

        private static PendingElText genRaw(ElConfigInterface elConfig, String rawElText) {
            return new PendingElText(elConfig, null, null, null, rawElText);
        }

        private void applyTo(JavaCG2ConfigureWrapper javaCG2ConfigureWrapper) {
            if (stringAnyFunctionEnum != null) {
                /*
                    Builder不通过BaseConfigureWrapper.setElConfigStringAny方法生成表达式文本：
                    语义化方法需要在设置参数时完成参数预校验（空白、单引号检查）并输出统一的日志，
                    因此在putStringAny阶段即完成表达式文本的拼接，应用时直接通过setElConfigText方法设置
                 */
                javaCG2ConfigureWrapper.setElConfigText(elConfig, genElText());
            } else {
                javaCG2ConfigureWrapper.setElConfigText(elConfig, rawElText);
            }
        }

        // 生成使用字符串比较函数的表达式文本，字符串参数已带单引号，不以分号结尾
        private String genElText() {
            return stringAnyFunctionEnum.getName() + "(" + elVariable.getVariableName() + "," + StringUtils.join(stringAnyArgs, ",") + ")";
        }

        private String genPrintInfo() {
            if (stringAnyFunctionEnum != null) {
                return stringAnyFunctionEnum.getName() + " " + elVariable.getVariableName() + " " + StringUtils.join(stringAnyArgs, ",");
            }
            return rawElText;
        }
    }
}
