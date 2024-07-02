/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.annotation;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 日志切片
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Aspect
@Component
public class LogsAspect {

    @Pointcut("@annotation(io.github.pnoker.common.annotation.Logs)")
    public void logsCut() {
        // nothing to do
    }

    @Around("logsCut() && @annotation(logs)")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint, Logs logs) throws Throwable {
        String uuid = UUID.randomUUID().toString();
        String className = proceedingJoinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = proceedingJoinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();
        log.info("Start => [{}].[{}.{}]: {}", uuid, className, methodName, logs.value());
        try {
            Object proceed = proceedingJoinPoint.proceed();
            log.info("End   <= [{}].[{}.{}].[{}ms]: {}", uuid, className, methodName, System.currentTimeMillis() - startTime, logs.value());
            return proceed;
        } catch (Throwable throwable) {
            log.info("End   <= [{}].[{}.{}].[{}ms]: {}", uuid, className, methodName, System.currentTimeMillis() - startTime, logs.value());
            throw throwable;
        }
    }
}