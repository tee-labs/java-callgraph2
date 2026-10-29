package com.adrninistrator.javacg2.thread;

import lombok.extern.slf4j.Slf4j;

/**
 * @author adrninistrator
 * @date 2024/3/23
 * @description:
 */
@Slf4j
public class JavaCG2UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private int exceptionCount;

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e != null) {
            exceptionCount++;
            log.error("线程池执行出现未捕获的异常 {} ", t.getName(), e);
        }
    }

    public int getExceptionCount() {
        return exceptionCount;
    }
}
