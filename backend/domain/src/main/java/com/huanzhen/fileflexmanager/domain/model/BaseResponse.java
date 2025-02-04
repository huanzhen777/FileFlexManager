package com.huanzhen.fileflexmanager.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseResponse<T> {
    // 常用状态码定义
    public static final int SUCCESS_CODE = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_ERROR = 500;

    // 业务状态码
    public static final int TOKEN_EXPIRED = 40003;
    public static final int PARAM_ERROR = 40004;

    // JWT相关状态码
    public static final int TOKEN_EXPIRED_CODE = 401001;
    public static final int TOKEN_INVALID_CODE = 401002;
    public static final int REGISTER_ERROR = 401003;

    private int code = SUCCESS_CODE;
    private String message = "success";
    private T data;
    private long timestamp = System.currentTimeMillis();

    public BaseResponse() {
    }

    public BaseResponse(T data) {
        this.data = data;
    }

    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>();
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(data);
    }

    public static <T> BaseResponse<T> success(T data, String message) {
        BaseResponse<T> response = new BaseResponse<>(data);
        response.setMessage(message);
        return response;
    }

    public static <T> BaseResponse<T> error(int code, String message) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    public static <T> BaseResponse<T> error(String message) {
        return error(INTERNAL_ERROR, message);
    }

    public static <T> BaseResponse<T> badRequest(String message) {
        return error(BAD_REQUEST, message);
    }

    public static <T> BaseResponse<T> unauthorized(String message) {
        return error(UNAUTHORIZED, message);
    }

    public static <T> BaseResponse<T> forbidden(String message) {
        return error(FORBIDDEN, message);
    }

    public static <T> BaseResponse<T> notFound(String message) {
        return error(NOT_FOUND, message);
    }

    public boolean isSuccess() {
        return this.code == SUCCESS_CODE;
    }
}