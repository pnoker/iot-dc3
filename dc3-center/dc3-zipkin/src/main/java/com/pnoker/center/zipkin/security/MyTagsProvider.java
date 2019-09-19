package com.pnoker.center.zipkin.security;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTags;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @ClassName: MyTagsProvider
 * @Description: TODO
 * @author: Created by xxx <a href="xxx@163.com">Contact author</a>
 * @date: 2019/2/27 13:49
 * @Version: V1.0
 */
public class MyTagsProvider implements WebMvcTagsProvider {

    /**
     * 去掉WebMvcTags.exception(exception)
     *
     * @param request   请求
     * @param response  响应
     * @param handler   处理
     * @param exception 异常
     * @return
     */
    @Override
    public Iterable<Tag> getTags(HttpServletRequest request, HttpServletResponse response, Object handler, Throwable exception) {
        return Tags.of(WebMvcTags.method(request), WebMvcTags.uri(request, response), WebMvcTags.status(response));
    }

    @Override
    public Iterable<Tag> getLongRequestTags(HttpServletRequest request, Object handler) {
        return Tags.of(WebMvcTags.method(request), WebMvcTags.uri(request, null));
    }
}