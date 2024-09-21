package org.adelaide.dto;

public class CommonResult {
    int code = 200;
    String message;
    Object data;
    int clock;

    public CommonResult() {}

    public CommonResult(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    public CommonResult(Object data) {
        this.data = data;
    }
    public CommonResult(int code, String message) {
        this.code = code;
        this.message = message;
    }
    public CommonResult(int code, int clock) {
        this.code = code;
        this.clock = clock;
    }
    public CommonResult(int code, String message, Object data, int clock) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.clock = clock;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getClock() {
        return clock;
    }

    public void setClock(int clock) {
        this.clock = clock;
    }
}
