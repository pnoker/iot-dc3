/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.annotation;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.AutoConfiguration;

import java.util.UUID;

/**
 * Aspect for logging method execution details using the @Logs annotation. This aspect
 * provides automatic logging of method entry and exit points, including execution time
 * tracking and unique request identification.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Aspect
@AutoConfiguration
public class LogsAspect {

    /**
     * Pointcut definition targeting methods annotated with @Logs annotation. This
     * pointcut will be used to intercept and log method executions.
     */
    @Pointcut("@annotation(io.github.pnoker.common.annotation.Logs)")
    public void logsCut() {
        // nothing to do
    }

    /**
     * Around advice that handles the logging of method execution. Generates a unique UUID
     * for each execution, logs the start and end of method calls, tracks execution time,
     * and handles any exceptions that occur during execution.
     *
     * @param proceedingJoinPoint The joinpoint representing the intercepted method
     * @param logs                The Logs annotation instance containing the log message
     * @return The result of the method execution
     * @throws Throwable If any error occurs during method execution
     */
    @Around("logsCut() && @annotation(logs)")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint, Logs logs) throws Throwable {
        String uuid = UUID.randomUUID().toString();
        String className = proceedingJoinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = proceedingJoinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();
        log.info("Annotated operation started, operationId={}, class={}, method={}, action={}", uuid, className,
                methodName, logs.value());
        try {
            Object proceed = proceedingJoinPoint.proceed();
            log.info("Annotated operation completed, operationId={}, class={}, method={}, durationMs={}, action={}",
                    uuid, className, methodName, System.currentTimeMillis() - startTime, logs.value());
            return proceed;
        } catch (Throwable throwable) {
            log.warn("Annotated operation failed, operationId={}, class={}, method={}, durationMs={}, action={}",
                    uuid, className, methodName, System.currentTimeMillis() - startTime, logs.value(), throwable);
            throw throwable;
        }
    }

}
