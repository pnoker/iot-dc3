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

import io.github.pnoker.common.enums.DeviceCommandTypeEnum;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备指令
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
public class DeviceCommandDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 指令类型
     */
    private DeviceCommandTypeEnum type;

    /**
     * 指令内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    public DeviceCommandDTO(DeviceCommandTypeEnum type, String content) {
        this.type = type;
        this.content = content;
        this.createTime = LocalDateTimeUtil.now();
    }

    /**
     * 设备读指令
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceRead implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 设备ID
         */
        private Long deviceId;

        /**
         * 位号ID
         */
        private Long pointId;


        /**
         * 创建时间
         */
        private LocalDateTime createTime;

        public DeviceRead(Long deviceId, Long pointId) {
            this.deviceId = deviceId;
            this.pointId = pointId;
            this.createTime = LocalDateTimeUtil.now();
        }
    }

    /**
     * 设备写指令
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceWrite implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 设备ID
         */
        private Long deviceId;

        /**
         * 位号ID
         */
        private Long pointId;

        /**
         * 待写入的值
         */
        private String value;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;

        public DeviceWrite(Long deviceId, Long pointId, String value) {
            this.deviceId = deviceId;
            this.pointId = pointId;
            this.value = value;
            this.createTime = LocalDateTimeUtil.now();
        }
    }
}
