package com.moying.lvyou.utils;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * 通一返回对象
 *
 * @param <T> 实体
 */
@Setter
@Getter
@Schema(description = "通一返回对象")
@SuppressWarnings("unused")
public class JsonResult<T> {
    /**
     * 信息
     */
    @Schema(description = "信息", required = true)
    private String msg;
    /**
     * 响应码
     */
    @Schema(description = "响应码", required = true)
    private int code;
    /**
     * 数据
     */
    @Schema(description = "数据", required = true)
    private T data;


    /**
     * 直接返回
     *
     * @return T
     */
    public static <T> JsonResult<T> success() {
        JsonResult<T> jsonResult = new JsonResult<>();
        jsonResult.setData(null);
        jsonResult.setCode(200);
        jsonResult.setMsg("ok");
        return jsonResult;
    }

    /**
     * 返回信息
     *
     * @param msg 信息
     * @return T
     */
    public static <T> JsonResult<T> success(String msg) {
        JsonResult<T> jsonResult = new JsonResult<>();
        jsonResult.setData(null);
        jsonResult.setCode(200);
        jsonResult.setMsg(msg);
        return jsonResult;
    }

    /**
     * 返回成功数据
     *
     * @param data 数据
     * @param <T>  实体
     * @return T
     */
    public static <T> JsonResult<T> success(T data) {
        JsonResult<T> jsonResult = new JsonResult<>();
        jsonResult.setData(data);
        jsonResult.setCode(200);
        jsonResult.setMsg("ok");
        return jsonResult;
    }

    /**
     * 自定义返回数据和内容
     *
     * @param msg  返回码
     * @param data 数据
     * @return T
     */
    public static <T> JsonResult<T> success(String msg, T data) {
        JsonResult<T> jsonResult = new JsonResult<>();
        jsonResult.setData(data);
        jsonResult.setCode(200);
        jsonResult.setMsg(msg);
        return jsonResult;
    }

    /**
     * 直接返回
     *
     * @return T
     */
    public static <T> JsonResult<T> fail() {
        JsonResult<T> jsonResult = new JsonResult<>();
        jsonResult.setData(null);
        jsonResult.setCode(400);
        jsonResult.setMsg("操作失败");
        return jsonResult;
    }

    /**
     * 自定义返回信息
     *
     * @param msg 错误信息
     * @return T
     */
    public static <T> JsonResult<T> fail(String msg) {
        JsonResult<T> jsonResult = new JsonResult<>();
        jsonResult.setData(null);
        jsonResult.setCode(400);
        jsonResult.setMsg(msg);
        return jsonResult;
    }

    /**
     * 自定义返回编码和信息
     *
     * @param code 返回码
     * @param msg  错误信息
     * @return T
     */
    public static <T> JsonResult<T> fail(int code, String msg) {
        JsonResult<T> jsonResult = new JsonResult<>();
        jsonResult.setData(null);
        jsonResult.setCode(code);
        jsonResult.setMsg(msg);
        return jsonResult;
    }
}

