package com.adrninistrator.javacg2.builder;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.el.enums.interfaces.ElConfigInterface;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: 表达式开关类Builder的公共逻辑，供JavaCG2MergeFileSwitchBuilder、JavaCG2ParseSwitchBuilder与JavaCG2XmlSwitchBuilder共用
 * 记录待应用的表达式配置，仅支持通过原始Aviator表达式文本设置
 * 同一个表达式配置枚举多次设置时，以后设置的为准（表达式文本整体替换）
 */
@Slf4j
class ElSwitchConfigHolder {

    // 待应用的表达式文本，key为表达式配置枚举，同一个枚举以后设置的为准
    private final Map<ElConfigInterface, String> pendingElTextMap = new LinkedHashMap<>();

    // 记录原始表达式文本
    void putRaw(ElConfigInterface elConfig, String elText) {
        if (elText == null) {
            throw new JavaCG2ConfigException("表达式文本不允许传入null " + elConfig.getKey());
        }
        log.info("设置表达式开关配置 {} {}", elConfig.getKey(), elText);
        pendingElTextMap.put(elConfig, elText);
    }

    // 将当前收集的表达式配置应用到配置参数包装对象
    void applyTo(JavaCG2ConfigureWrapper javaCG2ConfigureWrapper) {
        for (Map.Entry<ElConfigInterface, String> entry : pendingElTextMap.entrySet()) {
            javaCG2ConfigureWrapper.setElConfigText(entry.getKey(), entry.getValue());
        }
    }

    // 获取已设置表达式配置的枚举集合
    Set<ElConfigInterface> getSetElConfigSet() {
        return pendingElTextMap.keySet();
    }
}
