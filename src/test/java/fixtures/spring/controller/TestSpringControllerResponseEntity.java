package fixtures.spring.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Spring Controller派生信息测试使用的夹具类，方法返回类型为ResponseEntity，可能用于文件下载
 */
@RestController
@RequestMapping("/entity")
public class TestSpringControllerResponseEntity {

    @GetMapping("/entity")
    public ResponseEntity<String> entity() {
        return null;
    }
}
