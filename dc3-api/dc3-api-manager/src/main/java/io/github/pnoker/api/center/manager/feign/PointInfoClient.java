/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.api.center.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.fallback.PointInfoClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.PointInfoDto;
import io.github.pnoker.common.model.PointInfo;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 位号配置信息 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = ServiceConstant.Manager.POINT_INFO_URL_PREFIX, name = ServiceConstant.Manager.SERVICE_NAME, fallbackFactory = PointInfoClientFallback.class)
public interface PointInfoClient {

    /**
     * 新增 PointInfo
     *
     * @param pointInfo PointInfo
     * @return PointInfo
     */
    @PostMapping("/add")
    R<PointInfo> add(@Validated(Insert.class) @RequestBody PointInfo pointInfo);

    /**
     * 根据 ID 删除 PointInfo
     *
     * @param id PointInfo Id
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") String id);

    /**
     * 修改 PointInfo
     *
     * @param pointInfo PointInfo
     * @return PointInfo
     */
    @PostMapping("/update")
    R<PointInfo> update(@Validated(Update.class) @RequestBody PointInfo pointInfo);

    /**
     * 根据 ID 查询 PointInfo
     *
     * @param id PointInfo Id
     * @return PointInfo
     */
    @GetMapping("/id/{id}")
    R<PointInfo> selectById(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 属性ID、设备ID 和 位号ID 查询 PointInfo
     *
     * @param attributeId Attribute Id
     * @param deviceId    Device Id
     * @param pointId     Point Id
     * @return PointInfo
     */
    @GetMapping("/attribute_id/{attributeId}/device_id/{deviceId}/point_id/{pointId}")
    R<PointInfo> selectByAttributeIdAndDeviceIdAndPointId(@NotNull @PathVariable(value = "attributeId") String attributeId, @NotNull @PathVariable(value = "deviceId") String deviceId, @NotNull @PathVariable(value = "pointId") String pointId);

    /**
     * 根据 设备ID 和 位号ID 查询 PointInfo
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @return PointInfo
     */
    @GetMapping("/device_id/{deviceId}/point_id/{pointId}")
    R<List<PointInfo>> selectByDeviceIdAndPointId(@NotNull @PathVariable(value = "deviceId") String deviceId, @NotNull @PathVariable(value = "pointId") String pointId);

    /**
     * 根据 设备ID 查询 PointInfo
     *
     * @param deviceId Device Id
     * @return PointInfo
     */
    @GetMapping("/device_id/{deviceId}")
    R<List<PointInfo>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") String deviceId);

    /**
     * 分页查询 PointInfo
     *
     * @param pointInfoDto PointInfo Dto
     * @return Page<PointInfo>
     */
    @PostMapping("/list")
    R<Page<PointInfo>> list(@RequestBody(required = false) PointInfoDto pointInfoDto);

}
