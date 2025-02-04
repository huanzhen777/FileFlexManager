package com.huanzhen.fileflexmanager.interfaces.exception;

import com.huanzhen.fileflexmanager.domain.model.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalRestExceptionHandler {

    @ExceptionHandler(Exception.class)
    public BaseResponse handleException(Exception ex) {
        log.error("", ex);
        return BaseResponse.error(ex.getMessage());
    }


}