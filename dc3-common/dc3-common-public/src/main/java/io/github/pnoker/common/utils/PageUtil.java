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

package io.github.pnoker.common.utils;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.entity.common.Pages;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * 分页 相关工具类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
public class PageUtil {

    private PageUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 自定义 Pages 转 MyBatis Plus Page
     *
     * @param pages {@link Pages}
     * @param <T>   T
     * @return {@link Page}
     */
    public static <T> Page<T> page(Pages pages) {
        Page<T> page = new Page<>();
        if (Objects.isNull(pages)) {
            pages = new Pages();
        }

        if (pages.getCurrent() < DefaultConstant.ONE) {
            pages.setCurrent(DefaultConstant.ONE);
        }
        page.setCurrent(pages.getCurrent());

        if (pages.getSize() > DefaultConstant.MAX_PAGE_SIZE) {
            pages.setSize(DefaultConstant.MAX_PAGE_SIZE);
        }
        page.setSize(pages.getSize());

        List<OrderItem> orders = pages.getOrders();
        boolean anyMatch = orders.stream()
                .filter(order -> Objects.nonNull(order) && CharSequenceUtil.isNotEmpty(order.getColumn()))
                .anyMatch(order -> "create_time".equals(order.getColumn()));
        if (!anyMatch) {
            orders.add(OrderItem.desc("create_time"));
        }
        List<OrderItem> orderItemList = orders.stream().filter(order -> Objects.nonNull(order) && CharSequenceUtil.isNotEmpty(order.getColumn())).toList();
        page.setOrders(orderItemList);
        return page;
    }
}
