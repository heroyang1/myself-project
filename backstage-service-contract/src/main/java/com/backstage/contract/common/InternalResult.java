package com.backstage.contract.common;

import java.io.Serializable;

/**
 * 内部服务调用统一结果封装
 *
 * <p>结构 {@code {code,msg,data}} 与单体 {@code com.backstage.common.core.domain.R} 完全一致，
 * 保证服务端（单体 /internal 控制器）与客户端（未来 tool-service Feign）可用同一 JSON 形状编解码。
 * 本类不依赖 R，避免契约模块反向耦合单体内部类型。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 * @param <T> 业务数据类型
 */
public class InternalResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功状态码 */
    public static final int SUCCESS = 200;

    /** 失败状态码 */
    public static final int FAIL = 500;

    /**
     * 状态码：200-成功 500-失败
     */
    private int code;

    /**
     * 提示信息
     */
    private String msg;

    /**
     * 业务数据
     */
    private T data;

    public InternalResult() {
    }

    public InternalResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功（带数据）
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> InternalResult<T> ok(T data) {
        return new InternalResult<>(SUCCESS, "操作成功", data);
    }

    /**
     * 成功（带数据与自定义提示）
     *
     * @param data 业务数据
     * @param msg  提示信息
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> InternalResult<T> ok(T data, String msg) {
        return new InternalResult<>(SUCCESS, msg, data);
    }

    /**
     * 失败（自定义提示）
     *
     * @param msg 提示信息
     * @param <T> 数据类型
     * @return 失败结果
     */
    public static <T> InternalResult<T> fail(String msg) {
        return new InternalResult<>(FAIL, msg, null);
    }

    /**
     * 失败（自定义状态码与提示）
     *
     * @param code 状态码
     * @param msg  提示信息
     * @param <T>  数据类型
     * @return 失败结果
     */
    public static <T> InternalResult<T> fail(int code, String msg) {
        return new InternalResult<>(code, msg, null);
    }

    /**
     * 判断结果是否成功
     *
     * @param result 待判断结果
     * @param <T>    数据类型
     * @return true-成功
     */
    public static <T> boolean isSuccess(InternalResult<T> result) {
        return result != null && SUCCESS == result.getCode();
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
