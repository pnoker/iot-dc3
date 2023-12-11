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

package io.github.pnoker.center.data.entity.point;

import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 设备位号详情
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
public class PointDetail {

    @NotBlank(message = "device name can't be empty", groups = {Add.class})
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$",
            message = "设备名称格式无效",
            groups = {Add.class, Update.class})
    private String deviceName;

    @NotBlank(message = "point name can't be empty", groups = {Add.class})
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$",
            message = "Invalid point name",
            groups = {Add.class, Update.class})
    private String pointName;

    @NotNull(message = "driver id can't be empty",
            groups = {Add.class, Update.class})
    private Long driverId;

    private Long deviceId;
    private Long pointId;

    public PointDetail(Long deviceId, Long pointId) {
        this.deviceId = deviceId;
        this.pointId = pointId;
    }
}
