package fixtures.spring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * @author adrninistrator
 * @date 2026/10/30
 * @description: Spring Controller派生信息测试使用的夹具类，方法参数为HttpServletResponse，可能用于文件下载
 */
@RestController
@RequestMapping("/download")
public class TestSpringControllerDownload {

    @GetMapping("/dl")
    public void download(HttpServletResponse response) {
    }
}
