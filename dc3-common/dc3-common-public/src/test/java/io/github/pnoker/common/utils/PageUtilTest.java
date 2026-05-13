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
import io.github.pnoker.common.entity.common.Pages;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageUtilTest {

    @Test
    void pageCoercesNullPagesToFirstPage() {
        Page<Object> page = PageUtil.page(null);
        assertThat(page.getCurrent()).isEqualTo(1L);
        assertThat(page.getSize()).isEqualTo((long) DefaultConstant.PAGE_SIZE);
    }

    @Test
    void pageCoercesNonPositiveCurrentToOne() {
        Pages pages = new Pages();
        pages.setCurrent(0);
        Page<Object> page = PageUtil.page(pages);
        assertThat(page.getCurrent()).isEqualTo(1L);
    }

    @Test
    void pageCapsSizeAtMaxPageSize() {
        Pages pages = new Pages();
        pages.setSize(DefaultConstant.MAX_PAGE_SIZE + 100);
        Page<Object> page = PageUtil.page(pages);
        assertThat(page.getSize()).isEqualTo((long) DefaultConstant.MAX_PAGE_SIZE);
    }

    @Test
    void pageInjectsCreateTimeOrderWhenAbsent() {
        Page<Object> page = PageUtil.page(new Pages());
        assertThat(page.orders()).extracting(OrderItem::getColumn).contains("create_time");
    }

    @Test
    void pagePreservesUserOrdersAndAddsCreateTime() {
        Pages pages = new Pages();
        pages.getOrders().add(OrderItem.asc("name"));
        Page<Object> page = PageUtil.page(pages);
        assertThat(page.orders()).extracting(OrderItem::getColumn)
                .contains("name", "create_time");
    }

    @Test
    void pageDoesNotDuplicateExistingCreateTimeOrder() {
        Pages pages = new Pages();
        pages.getOrders().add(OrderItem.desc("create_time"));
        Page<Object> page = PageUtil.page(pages);
        long createTimeOrders = page.orders().stream()
                .filter(it -> "create_time".equals(it.getColumn()))
                .count();
        assertThat(createTimeOrders).isEqualTo(1L);
    }

    @Test
    void copyPageReturnsEmptyPageForNullSource() {
        Page<String> copied = PageUtil.copyPage(null, Object::toString);
        assertThat(copied.getRecords()).isEmpty();
    }

    @Test
    void copyPageMapsRecordsAndPreservesMetadata() {
        Page<Integer> source = new Page<>(2, 10, 25);
        List<Integer> records = new ArrayList<>(List.of(1, 2, 3));
        source.setRecords(records);
        Page<String> copied = PageUtil.copyPage(source, Object::toString);
        assertThat(copied.getCurrent()).isEqualTo(2);
        assertThat(copied.getSize()).isEqualTo(10);
        assertThat(copied.getTotal()).isEqualTo(25);
        assertThat(copied.getRecords()).containsExactly("1", "2", "3");
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<PageUtil> constructor = PageUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
