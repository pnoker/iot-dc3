/*
 * Copyright 2022 Pnoker All Rights Reserved
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

    // 2022-03-13 检查：通过
    @Pointcut("@annotation(io.github.pnoker.common.annotation.Logs)")
    public void logsCut() {
    }

    // 2022-03-13 检查：通过
    @Around("logsCut() && @annotation(logs)")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint, Logs logs) throws Throwable {
        String uuid = IdUtil.fastSimpleUUID();
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