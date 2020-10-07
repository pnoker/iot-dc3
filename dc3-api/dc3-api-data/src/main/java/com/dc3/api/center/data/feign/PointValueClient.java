/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.api.center.data.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.data.hystrix.PointValueClientHystrix;
import com.dc3.common.bean.R;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.bean.driver.PointValueDto;
import com.dc3.common.constant.Common;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 数据 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_DATA_URL_PREFIX, name = Common.Service.DC3_DATA_SERVICE_NAME, fallbackFactory = PointValueClientHystrix.class)
public interface PointValueClient {

    /**
     * 数据纠正
     *
     * @param serviceName Driver Service Name
     */
    @GetMapping("/correct/serviceName/{serviceName}")
    R<Boolean> correct(@NotNull @PathVariable(value = "serviceName") String serviceName);

    /**
     * 获取设备状态
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param deviceId Device Id
     * @return String
     */
    @GetMapping("/status/deviceId/{deviceId}")
    R<String> status(@NotNull @PathVariable(value = "deviceId") Long deviceId);

    /**
     * 获取实时值，读 Redis
     *
     * @param deviceId Device Id
     * @return String Value
     */
    @GetMapping("/realtime/deviceId/{deviceId}")
    R<List<PointValue>> realtime(@NotNull @PathVariable(value = "deviceId") Long deviceId);

    /**
     * 获取实时值，读 Redis
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @return String Value
     */
    @GetMapping("/realtime/deviceId/{deviceId}/pointId/{pointId}")
    R<PointValue> realtime(@NotNull @PathVariable(value = "deviceId") Long deviceId, @NotNull @PathVariable(value = "pointId") Long pointId);

    /**
     * 查询最新 PointValue
     *
     * @param deviceId Device Id
     * @return PointValue
     */
    @GetMapping("/latest/deviceId/{deviceId}")
    R<PointValue> latest(@NotNull @PathVariable(value = "deviceId") Long deviceId);

    /**
     * 查询最新 PointValue
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @return PointValue
     */
    @GetMapping("/latest/deviceId/{deviceId}/pointId/{pointId}")
    R<PointValue> latest(@NotNull @PathVariable(value = "deviceId") Long deviceId, @NotNull @PathVariable(value = "pointId") Long pointId);

    /**
     * 分页查询 PointValue
     *
     * @param pointValueDto PointValueDto
     * @return Page<PointValue>
     */
    @PostMapping("/list")
    R<Page<PointValue>> list(@RequestBody(required = false) PointValueDto pointValueDto);
}
