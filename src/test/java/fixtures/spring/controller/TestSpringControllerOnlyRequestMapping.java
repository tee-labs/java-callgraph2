package fixtures.spring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Spring Controller派生信息测试使用的夹具类，类上只有@RequestMapping注解而没有Controller相关注解
 * 与java-all-call-graph的处理逻辑一致，类上存在@RequestMapping类注解时即被视为Spring MVC相关的类，方法上的映射注解仍会生成记录
 */
@RequestMapping("/only/request-mapping")
public class TestSpringControllerOnlyRequestMapping {

    @GetMapping("/get-only-rm")
    public String getOnlyRm() {
        return "getOnlyRm";
    }
}
