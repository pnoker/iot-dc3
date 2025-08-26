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
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页参数配置
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */

@Getter
@Setter
public class Pages implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long current = 1;

    private long size = DefaultConstant.PAGE_SIZE;

    private long startTime;

    private long endTime;

    private List<OrderItem> orders = new ArrayList<>(2);
}
