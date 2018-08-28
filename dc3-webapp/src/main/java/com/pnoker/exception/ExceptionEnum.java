package com.pnoker.exception;

public enum ExceptionEnum {

    // 系统级别异常
    NOT_AUTHTICATION(401, "没有权限！"),
    HTTP_REQUEST_ERROR(402, "HTTP 请求失败！"),

    // 业务级别异常 10000 ~ 20000
    HUB_ERROR(10001, "仓库不存在！"),
    VIDEO_SEARCH_ERROR(10002, "视频搜索请求失败！"),
    VID_CANNOT_MATCH(10003, "无法匹配视频ID！"),
    NO_VIDEO(10004, "视频找不到！"),
    NO_PARSER(10005, "找不到合适的解析器！")
    ;

    private int code;

    private String message;

    ExceptionEnum(int code, String message){
        this.code = code;
        this.message = message;
    }

    public int getCode(){
        return code;
    }

    public String getMessage(){
        return message;
    }
}
