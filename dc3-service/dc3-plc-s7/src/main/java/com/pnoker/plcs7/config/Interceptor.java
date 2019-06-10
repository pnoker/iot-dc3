package com.pnoker.plcs7.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 拦截器
 */
@Slf4j
@Component
public class Interceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler.getClass().isAssignableFrom(HandlerMethod.class)) {
            long beginNaoTime = System.nanoTime();
            request.setAttribute("begin_nao_time", beginNaoTime);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (handler.getClass().isAssignableFrom(HandlerMethod.class)) {
            long beginNaoTime = (long) request.getAttribute("begin_nao_time");
            List<String> parameters = new ArrayList<>();
            long interval = (System.nanoTime() - beginNaoTime) / 1000000;
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                MethodParameter[] methodParameters = handlerMethod.getMethodParameters();
                for (MethodParameter methodParameter : methodParameters) {
                    parameters.add(methodParameter.getParameterName());
                }
            }
            Logs eidps = ((HandlerMethod) handler).getMethodAnnotation(Logs.class);
            if (eidps != null) {
                log.info("{} -> [url:{}({})] , takes {} ms", eidps.value(), request.getRequestURI(), String.join(",", parameters), interval);
            } else {
                log.info("Request -> [url:{}({})] , takes {} ms", request.getRequestURI(), String.join(",", parameters), interval);
            }
        }
    }
}