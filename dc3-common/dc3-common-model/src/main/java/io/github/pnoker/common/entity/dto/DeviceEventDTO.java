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

package io.github.pnoker.common.entity.dto;

import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 设备事件
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DeviceEventDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    private DeviceEventTypeEnum type;

    /**
     * 事件内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    public DeviceEventDTO(DeviceEventTypeEnum type, String content) {
        this.type = type;
        this.content = content;
        this.createTime = LocalDateTime.now();
    }

    /**
     * 设备状态
     *
     * @author pnoker
     * @since 2022.1.0
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceStatus implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 设备ID
         */
        private Long deviceId;

        /**
         * 设备状态
         */
        private DeviceStatusEnum status;

        /**
         * 设备状态失效时间
         */
        @Builder.Default
        private int timeOut = 15;

        /**
         * 设备状态失效时间单位
         */
        @Builder.Default
        private TimeUnit timeUnit = TimeUnit.MINUTES;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;

        public DeviceStatus(Long deviceId, DeviceStatusEnum status) {
            this.deviceId = deviceId;
            this.status = status;
            this.createTime = LocalDateTime.now();
        }

        public DeviceStatus(Long deviceId, DeviceStatusEnum status, int timeOut, TimeUnit timeUnit) {
            this.deviceId = deviceId;
            this.status = status;
            this.timeOut = timeOut;
            this.timeUnit = timeUnit;
            this.createTime = LocalDateTime.now();
        }
    }
}
