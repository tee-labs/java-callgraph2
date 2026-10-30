package fixtures.spring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Spring Controller派生信息测试使用的夹具类，类上的@RequestMapping类注解的路径为数组，且部分路径以/结尾
 */
@RestController
@RequestMapping({"/mc1", "mc2/"})
public class TestSpringControllerMultiClassPath {

    @GetMapping("/m")
    public String m() {
        return "m";
    }
}
