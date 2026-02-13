package com.sky.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果封装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseResult<T> {
    /**
     * 状态码：200成功，400失败，500服务器错误
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应（带数据）
     */
    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(200, "success", data);
    }

    /**
     * 成功响应（无数据）
     */
    public static ResponseResult<Void> success() {
        return new ResponseResult<>(200, "success", null);
    }

    /**
     * 失败响应
     */
    public static <T> ResponseResult<T> error(Integer code, String message) {
        return new ResponseResult<>(code, message, null);
    }

    /**
     * 失败响应（默认状态码500）
     */
    public static <T> ResponseResult<T> error(String message) {
        return new ResponseResult<>(500, message, null);
    }
}