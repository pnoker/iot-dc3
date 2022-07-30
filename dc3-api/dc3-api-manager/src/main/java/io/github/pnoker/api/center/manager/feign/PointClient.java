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
import io.github.pnoker.api.center.manager.fallback.PointClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.PointDto;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 位号 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = ServiceConstant.Manager.POINT_URL_PREFIX, name = ServiceConstant.Manager.SERVICE_NAME, fallbackFactory = PointClientFallback.class)
public interface PointClient {

    /**
     * 新增 Point
     *
     * @param point Point
     * @return Point
     */
    @PostMapping("/add")
    R<Point> add(@Validated(Insert.class) @RequestBody Point point, @RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 ID 删除 Point
     *
     * @param id Point Id
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") String id);

    /**
     * 修改 Point
     *
     * @param point Point
     * @return Point
     */
    @PostMapping("/update")
    R<Point> update(@Validated(Update.class) @RequestBody Point point, @RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 ID 查询 Point
     *
     * @param id Point Id
     * @return Point
     */
    @GetMapping("/id/{id}")
    R<Point> selectById(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 ID 集合查询 Point
     *
     * @param pointIds Point Id Set
     * @return Map<String, Point>
     */
    @PostMapping("/ids")
    R<Map<String, Point>> selectByIds(@RequestBody Set<String> pointIds);

    /**
     * 根据 设备 ID 查询 Point
     *
     * @param deviceId Device Id
     * @return Point Array
     */
    @GetMapping("/device_id/{deviceId}")
    R<List<Point>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") String deviceId);

    /**
     * 根据 模板 ID 查询 Point
     *
     * @param profileId Profile Id
     * @return Point Array
     */
    @GetMapping("/profile_id/{profileId}")
    R<List<Point>> selectByProfileId(@NotNull @PathVariable(value = "profileId") String profileId);

    /**
     * 分页查询 Point
     *
     * @param pointDto Point Dto
     * @return Page<Point>
     */
    @PostMapping("/list")
    R<Page<Point>> list(@RequestBody(required = false) PointDto pointDto, @RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 查询 位号单位
     *
     * @param pointIds Point Id Set
     * @return Map<String, String>
     */
    @PostMapping("/unit")
    R<Map<String, String>> unit(@RequestBody Set<String> pointIds);
}
