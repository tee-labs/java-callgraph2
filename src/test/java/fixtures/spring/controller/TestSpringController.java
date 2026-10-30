package fixtures.spring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Spring Controller派生信息测试使用的夹具类，类上有@RequestMapping且指定了类路径
 */
@RestController
@RequestMapping("/test/controller")
public class TestSpringController {

    @GetMapping("/get1")
    public String get1() {
        return "get1";
    }

    // 方法路径为数组，应生成两条记录，seq分别为0、1
    @PostMapping({"/post1", "/post2"})
    public void post1() {
    }

    // 方法路径使用value属性指定
    @RequestMapping(value = "/req1")
    public String req1() {
        return "req1";
    }

    // value属性与非路径属性同时存在，非路径属性不应参与URI拼接
    @RequestMapping(value = "/req2", method = "GET")
    public String req2() {
        return "req2";
    }

    // 方法上未指定路径属性，方法路径应记录为空字符串
    @PutMapping
    public void put1() {
    }

    // 同一方法上存在多个RequestMapping相关注解，每个注解应各自生成记录
    @GetMapping("/multi-get")
    @PostMapping("/multi-post")
    public void multiAnnotationMethod() {
    }

    // 方法参数为MultipartFile，可能用于文件上传
    @PostMapping("/upload")
    public void upload(MultipartFile multipartFile) {
    }

    // 方法上没有任何Spring MVC注解，不应生成记录
    public void normalMethod() {
    }
}
