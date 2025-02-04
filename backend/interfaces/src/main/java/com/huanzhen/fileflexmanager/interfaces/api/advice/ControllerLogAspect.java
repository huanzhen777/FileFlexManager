package com.huanzhen.fileflexmanager.interfaces.api.advice;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class ControllerLogAspect {

    @Pointcut("execution(* com.huanzhen.fileflexmanager.interfaces.api.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        
        // 记录请求信息
        String requestLog = String.format("请求URL: %s, HTTP方法: %s, IP: %s, 类方法: %s.%s, 参数: %s",
                request.getRequestURL().toString(),
                request.getMethod(),
                request.getRemoteAddr(),
                point.getSignature().getDeclaringTypeName(),
                point.getSignature().getName(),
                getRequestParams(point.getArgs())
        );
        log.info("controller >>> {}", requestLog);

        // 执行方法
        Object result = point.proceed();

        // 记录响应信息
        String responseLog = String.format("处理时间: %dms, 响应结果: %s",
                System.currentTimeMillis() - startTime,
                getResponseParams(result)
        );
        log.info("controller <<< {}", responseLog);

        return result;
    }

    private String getRequestParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "无参数";
        }
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg instanceof MultipartFile) {
                        MultipartFile file = (MultipartFile) arg;
                        return String.format("文件[%s]", file.getOriginalFilename());
                    }
                    try {
                        return JSON.toJSONString(arg);
                    } catch (Exception e) {
                        return arg.toString();
                    }
                })
                .collect(Collectors.joining(", "));
    }

    private String getResponseParams(Object result) {
        if (result == null) {
            return "无返回值";
        }
        try {
            return JSON.toJSONString(result);
        } catch (Exception e) {
            return result.toString();
        }
    }
} 