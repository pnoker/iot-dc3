package com.pnoker.exception;

/**
 * @author: Pnoker
 * @email : pnokers@gmail.com
 * <p>
 * 统一异常
 */
public class AnyException extends RuntimeException {

    public AnyException(ExceptionEnum exceptionEnum) {
        super(exceptionEnum.getMessage());
    }

    public AnyException(String message) {
        super(message);
    }

}
