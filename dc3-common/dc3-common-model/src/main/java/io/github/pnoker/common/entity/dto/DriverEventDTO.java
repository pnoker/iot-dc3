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

package io.github.pnoker.common.entity.dto;

import io.github.pnoker.common.enums.DriverEventTypeEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 驱动事件
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DriverEventDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    private DriverEventTypeEnum type;

    /**
     * 事件内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    public DriverEventDTO(DriverEventTypeEnum type, String content) {
        this.type = type;
        this.content = content;
        this.createTime = LocalDateTimeUtil.now();
    }

    /**
     * 驱动状态
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverStatus implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 驱动ID
         */
        private Long driverId;

        /**
         * 驱动状态
         */
        private DriverStatusEnum status;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;

        public DriverStatus(Long driverId, DriverStatusEnum status) {
            this.driverId = driverId;
            this.status = status;
            this.createTime = LocalDateTimeUtil.now();
        }
    }
}
