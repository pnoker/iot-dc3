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

package io.github.pnoker.common.utils;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.entity.common.Pages;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Converts {@code Pages} DTO to MyBatis-Plus {@code Page}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class PageUtil {

    private PageUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Convert custom {@link Pages} object to MyBatis-Plus {@link Page}.
     *
     * @param pages {@link Pages}
     * @param <T>   Entity type
     * @return MyBatis-Plus {@link Page}
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
                .filter(order -> Objects.nonNull(order) && StringUtils.isNotEmpty(order.getColumn()))
                .anyMatch(order -> "create_time".equals(order.getColumn()));
        if (!anyMatch) {
            orders.add(OrderItem.desc("create_time"));
        }
        List<OrderItem> orderItemList = orders.stream()
                .filter(order -> Objects.nonNull(order) && StringUtils.isNotEmpty(order.getColumn()))
                .toList();
        page.setOrders(orderItemList);
        return page;
    }

    /**
     * Copy pagination metadata from {@code source} and map every record through
     * {@code mapper}.
     * <p>
     * Used by every {@code @Mapper} interface in place of a hand-written MapStruct
     * page-mapping method. Replacing those abstract methods with a {@code default}
     * delegation kills six {@code @Mapping(target=..., ignore=true)} annotations per
     * builder — those existed only because MapStruct couldn't figure out what to do with
     * MyBatis-Plus's {@code Page} bookkeeping fields ({@code orders}, {@code countId},
     * {@code maxLimit}, etc.).
     *
     * @param source source page (may be {@code null})
     * @param mapper per-record mapping function
     * @param <S>    source record type
     * @param <T>    target record type
     * @return target page with the same {@code current/size/total/pages} as {@code source}
     * (empty page when {@code source} is {@code null}).
     */
    public static <S, T> Page<T> copyPage(Page<S> source, Function<? super S, ? extends T> mapper) {
        if (Objects.isNull(source)) {
            return new Page<>();
        }
        Page<T> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<S> records = source.getRecords();
        if (Objects.nonNull(records)) {
            target.setRecords(records.stream().<T>map(mapper).toList());
        }
        return target;
    }

}
