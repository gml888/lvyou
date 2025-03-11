package com.moying.lvyou.utils;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * 分页列表通一返回对象
 *
 * @param <T> 实体
 */
@Setter
@Getter
@Schema(description = "分页列表通一返回对象")
public class PageResult<T> {
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
     * 页尺寸
     */
    @Schema(description = "页尺寸", required = true)
    private int pageSize;
    /**
     * 页码
     */
    @Schema(description = "页码", required = true)
    private int pageIndex;
    /**
     * 总页数
     */
    @Schema(description = "总页数", required = true)
    private int pages;
    /**
     * 总记录数
     */
    @Schema(description = "总记录数", required = true)
    private int total;
    /**
     * 列表数据
     */
    @Schema(description = "列表数据", required = true)
    private T data;

    /**
     * 自定义返回数据
     *
     * @param data      数据
     * @param pageSize  页尺寸
     * @param pageIndex 页码
     * @param total     总记录数
     * @param <T>       实体
     * @return Boolean
     */
    public static <T> PageResult<T> success(T data,int pageIndex, int pageSize,  int total) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setData(data);
        pageResult.setPageSize(pageSize);
        pageResult.setPageIndex(pageIndex);
        pageResult.setTotal(total);
        pageResult.setPages(pageResult.getTotalPages(total,pageSize));
        pageResult.setCode(200);
        pageResult.setMsg("ok");
        return pageResult;
    }
    public int getTotalPages(int total,int size) {
        return Double.valueOf(Math.ceil((double) total / (double) size)).intValue();
    }
    /**
     * 直接返回
     *
     * @return T
     */
    public static <T> PageResult<T> fail() {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setData(null);
        pageResult.setCode(500);
        pageResult.setMsg("fail");
        return pageResult;
    }

    /**
     * 自定义返回信息
     *
     * @param msg 错误信息
     * @return T
     */
    public static <T> PageResult<T> fail(String msg) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setData(null);
        pageResult.setCode(500);
        pageResult.setMsg(msg);
        return pageResult;
    }

    /**
     * 自定义返回编码和信息
     *
     * @param code 返回码
     * @param msg  错误信息
     * @return T
     */
    public static <T> PageResult<T> fail(int code, String msg) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setData(null);
        pageResult.setCode(code);
        pageResult.setMsg(msg);
        return pageResult;
    }



}

