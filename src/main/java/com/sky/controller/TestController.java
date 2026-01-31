package com.sky.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口控制器，验证项目启动成功
 */
@RestController // 核心注解：标识这是接口控制器，返回JSON/字符串（不用加@ResponseBody）
@RequestMapping("/test") // 接口统一前缀：所有该类下的接口都以/test开头
public class TestController {

    // 测试接口：http://localhost:8080/test/hello
    @GetMapping("/hello") // 限定GET请求，接口路径/hello，拼接后就是/test/hello
    public String hello() {
        // 接口返回值，浏览器/Postman访问会直接显示这个字符串
        return "苍穹外卖项目启动成功！接口访问正常";
    }
}