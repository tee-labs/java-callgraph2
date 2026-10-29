package com.adrninistrator.javacg2.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author adrninistrator
 * @date 2025/7/20
 * @description:
 */
@Slf4j
public class JavaCG2ThreadUtil {

    /**
     * 等待直到任务执行完毕
     *
     * @param currentSimpleClassName
     * @param threadPoolExecutor
     * @param runningTaskNum
     */
    public static void wait4TPEDone(String currentSimpleClassName, ThreadPoolExecutor threadPoolExecutor, AtomicInteger runningTaskNum) {
        while (true) {
            int queueSize = threadPoolExecutor.getQueue().size();
            int activeThreadCount = threadPoolExecutor.getActiveCount();
            int runningTaskNumValue = runningTaskNum.get();
            if (queueSize == 0 && activeThreadCount == 0 && runningTaskNumValue == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("{} 线程池中的任务已执行完毕", currentSimpleClassName);
                }
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug("{} 等待线程池任务执行完毕，任务队列数量 {} 活跃线程数 {} 正在执行的任务数量 {}", currentSimpleClassName, queueSize, activeThreadCount, runningTaskNumValue);
            }
            JavaCG2Util.sleep(100L);
        }
    }


    /**
     * 使用线程池执行任务
     *
     * @param currentSimpleClassName
     * @param threadPoolExecutor
     * @param runningTaskNum
     * @param runnable
     */
    public static void executeByTPE(String currentSimpleClassName, ThreadPoolExecutor threadPoolExecutor, AtomicInteger runningTaskNum, Runnable runnable) {
        threadPoolExecutor.execute(() -> {
            int currentRunningTaskNum = runningTaskNum.addAndGet(1);
            log.debug("{} 开始执行任务 正在执行的任务数量 {}", currentSimpleClassName, currentRunningTaskNum);
            try {
                runnable.run();
            } finally {
                int currentRunningTaskNum2 = runningTaskNum.decrementAndGet();
                log.debug("{} 结束执行任务 正在执行的任务数量 {}", currentSimpleClassName, currentRunningTaskNum2);
            }
        });
    }

    /**
     * 等待直到允许任务执行
     *
     * @param currentSimpleClassName
     * @param threadPoolExecutor
     * @param taskQueueMaxSize
     */
    public static void wait4TPEAllowExecute(String currentSimpleClassName, ThreadPoolExecutor threadPoolExecutor, int taskQueueMaxSize) {
        while (true) {
            if (threadPoolExecutor.getQueue().size() < taskQueueMaxSize) {
                return;
            }
            log.debug("{} wait4TPEExecute ...", currentSimpleClassName);
            JavaCG2Util.sleep(100L);
        }
    }

    private JavaCG2ThreadUtil() {
        throw new IllegalStateException("illegal");
    }
}
