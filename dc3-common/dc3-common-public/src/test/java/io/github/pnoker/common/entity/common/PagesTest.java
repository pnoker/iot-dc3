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

package io.github.pnoker.common.entity.common;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import io.github.pnoker.common.constant.common.DefaultConstant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PagesTest {

    @Test
    void defaultsCurrentToOneAndSizeToPageSize() {
        Pages pages = new Pages();
        assertThat(pages.getCurrent()).isEqualTo(1L);
        assertThat(pages.getSize()).isEqualTo((long) DefaultConstant.PAGE_SIZE);
    }

    @Test
    void allowsSettingTimeRangeAndOrders() {
        Pages pages = new Pages();
        pages.setStartTime(100L);
        pages.setEndTime(200L);
        pages.getOrders().add(OrderItem.asc("name"));
        assertThat(pages.getStartTime()).isEqualTo(100L);
        assertThat(pages.getEndTime()).isEqualTo(200L);
        assertThat(pages.getOrders()).hasSize(1);
    }

    @Test
    void ordersListIsMutableInitially() {
        Pages pages = new Pages();
        assertThat(pages.getOrders()).isEmpty();
        pages.getOrders().add(OrderItem.desc("create_time"));
        assertThat(pages.getOrders()).extracting(OrderItem::getColumn).contains("create_time");
    }
}
