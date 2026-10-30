package fixtures.spring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Spring Controller派生信息测试使用的夹具类，类上没有@RequestMapping类注解，类路径应记录为空字符串
 */
@RestController
public class TestSpringControllerNoClassPath {

    @GetMapping("/no-class-path")
    public String noClassPath() {
        return "noClassPath";
    }
}
