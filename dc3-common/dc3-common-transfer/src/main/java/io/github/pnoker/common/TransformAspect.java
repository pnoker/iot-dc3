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
 * @version 2025.6.0
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
