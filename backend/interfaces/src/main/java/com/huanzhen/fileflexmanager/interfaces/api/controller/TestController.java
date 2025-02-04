package com.huanzhen.fileflexmanager.interfaces.api.controller;

import com.huanzhen.fileflexmanager.domain.model.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/test")
    public BaseResponse<String> test() {
        return BaseResponse.success("后端连接成功！");
    }
}