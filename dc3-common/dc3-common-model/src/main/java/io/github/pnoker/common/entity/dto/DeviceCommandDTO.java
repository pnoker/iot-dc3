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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data transfer object for device command dispatch.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
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
     * Type
     */
    private DeviceCommandTypeEnum type;

    /**
     *
     */
    private String content;

    /**
     * Create Time
     */
    private LocalDateTime createTime;

    public DeviceCommandDTO(DeviceCommandTypeEnum type, String content) {
        this.type = type;
        this.content = content;
        this.createTime = LocalDateTimeUtil.now();
    }

    /**
     * @author pnoker
     * @version 2025.9.0
     * @since 2016.10.1
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceRead implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Device ID
         */
        private Long deviceId;

        /**
         * Point ID
         */
        private Long pointId;

        /**
         * Create Time
         */
        private LocalDateTime createTime;

        public DeviceRead(Long deviceId, Long pointId) {
            this.deviceId = deviceId;
            this.pointId = pointId;
            this.createTime = LocalDateTimeUtil.now();
        }

    }

    /**
     * @author pnoker
     * @version 2025.9.0
     * @since 2016.10.1
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceWrite implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Device ID
         */
        private Long deviceId;

        /**
         * Point ID
         */
        private Long pointId;

        /**
         *
         */
        private String value;

        /**
         * Create Time
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
