package fixtures.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Spring Controller派生信息测试使用的夹具类，类上只有@Controller注解且方法上没有RequestMapping相关注解，不应生成记录
 */
@Controller
@RequestMapping("/only/controller")
public class TestSpringControllerOnlyController {

    public void normalMethod() {
    }
}
