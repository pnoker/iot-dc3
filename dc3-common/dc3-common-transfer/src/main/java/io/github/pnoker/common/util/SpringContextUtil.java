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

package io.github.pnoker.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Spring上下文工具
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext context;


    /**
     * 获取示例, 如果找不到会报错
     */
    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }

    private static void setContext(ApplicationContext context) {
        SpringContextUtil.context = context;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        setContext(applicationContext);
    }

}
