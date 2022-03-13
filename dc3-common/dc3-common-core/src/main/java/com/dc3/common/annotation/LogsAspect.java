package com.dc3.common.annotation;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 日志切片
 *
 * @author pnoker
 */
@Slf4j
@Aspect
@Component
public class LogsAspect {

    @Pointcut("@annotation(Logs)")
    public void logsCut() {
    }

    @Around("logsCut()&&@annotation(logs)")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint, Logs logs) throws Throwable {
        String uuid = IdUtil.fastSimpleUUID();
        String className = proceedingJoinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = proceedingJoinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();
        log.info("Start => [{}].[{}.{}]: {}", uuid, className, methodName, logs.value());
        try {
            Object proceed = proceedingJoinPoint.proceed();
            log.info("End <= [{}].[{}.{}].[{}ms]: {}", uuid, className, methodName, System.currentTimeMillis() - startTime, logs.value());
            return proceed;
        } catch (Throwable throwable) {
            log.info("End   <= [{}].[{}.{}].[{}ms]: {}", uuid, className, methodName, System.currentTimeMillis() - startTime, logs.value());
            throw throwable;
        }
    }
}