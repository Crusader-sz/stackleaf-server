package com.crusader.stackleafserver.result;

import com.crusader.stackleafserver.constant.MessageConstant;
import com.crusader.stackleafserver.constant.ResultCodeConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;// 业务状态码
    private String message;// 提示信息
    private T data;// 响应数据

    // 响应成功，不返回数据
    public static <T> Result<T> success() {
        return new Result<>(ResultCodeConstant.SUCCESS, MessageConstant.SUCCESS, null);
    }
    // 响应成功，返回数据
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCodeConstant.SUCCESS,MessageConstant.SUCCESS, data);
    }
    // 响应成功，返回自定义信息
    public static <T> Result<T> success(String message) {
        return new Result<>(ResultCodeConstant.SUCCESS,message, null);
    }
    // 响应成功，返回自定义信息与数据
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCodeConstant.SUCCESS, message, data);
    }
    // 响应失败，返回提示信息
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCodeConstant.ERROR, message, null);
    }
    // 响应失败，返回状态码和提示信息
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
