package com.platform.common.api;

import lombok.Data;
import java.io.Serializable;

@Data
public class R<T> implements Serializable {
    private Integer code;
    private String msg;
    private T data;

    // 成功
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    // 失败 - 默认500
    public static <T> R<T> fail(String msg) {
        return fail(500, msg);
    }

    // 失败 - 自定义Code
    public static <T> R<T> fail(Integer code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}