package com.pnoker.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author: Pnoker
 * @email : pnokers@gmail.com
 * <p>
 * 统一异常处理
 */
@Slf4j
@ControllerAdvice
public class AnyExceptionHandler {

    /**
     * 处理全局异常
     */
    @ExceptionHandler(Exception.class)
    public String handlerGlobalException(Exception exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error";
    }

}
