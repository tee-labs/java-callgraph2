package fixtures.spring.controller;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Spring定时任务派生信息测试使用的夹具类，方法上通过@Scheduled注解定义定时任务
 */
public class TestSpringTask {

    // 通过cron表达式定义的定时任务
    @Scheduled(cron = "0 0 1 * * *")
    public void cronTask() {
    }

    // 通过fixedRate定义的定时任务
    @Scheduled(fixedRate = 5000L, initialDelay = 1000L)
    public void fixedRateTask() {
    }

    // 方法上没有任何定时任务注解，不应生成记录
    public void normalMethod() {
    }
}
