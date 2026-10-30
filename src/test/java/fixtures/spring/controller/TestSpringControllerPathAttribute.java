package fixtures.spring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Spring Controller派生信息测试使用的夹具类，方法路径使用path属性指定
 */
@RestController
@RequestMapping("/path/attr")
public class TestSpringControllerPathAttribute {

    @GetMapping(path = {"/pa1", "/pa2"})
    public String pa() {
        return "pa";
    }
}
