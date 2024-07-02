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

package io.github.pnoker.common;

import io.github.pnoker.common.annotation.Transform;
import io.github.pnoker.common.util.TransformUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * 转换器AOP
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Aspect
@Slf4j
public class TransformAspect {

    @Resource
    private GenericConversionService genericConversionService;

    @AfterReturning(pointcut = "@annotation(transformAnnotation)", returning = "returnValue")
    public void doAfter(Object returnValue, Transform transformAnnotation) throws IllegalAccessException {
        long l = System.currentTimeMillis();
        // 获取容器中的转换器进行返回值解包, 注意此处返回结果可能是Bean也可能是集合
        Object result = genericConversionService.convert(returnValue, Object.class);
        TransformUtil.transform(result);
        long time = System.currentTimeMillis() - l;
        log.debug("Conversion time: {}ms", time);
    }
}
