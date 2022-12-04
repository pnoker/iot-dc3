/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.api.transfer.rtmp.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.transfer.rtmp.fallback.RtmpClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.RtmpServiceConstant;
import io.github.pnoker.common.dto.RtmpDto;
import io.github.pnoker.common.model.Rtmp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

/**
 * Rtmp转码 FeignClient
 *
 * @author pnoker
 * @since 2022.1.0
 */
@FeignClient(path = RtmpServiceConstant.URL_PREFIX, name = RtmpServiceConstant.SERVICE_NAME, fallbackFactory = RtmpClientFallback.class)
public interface RtmpClient {

    /**
     * 新增转码任务
     *
     * @param rtmp     转码任务
     * @param tenantId 租户ID
     * @return {@link io.github.pnoker.common.dto.RtmpDto}
     */
    @PostMapping("/add")
    R<Rtmp> add(@Validated @RequestBody Rtmp rtmp, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 ID 删除转码任务
     *
     * @param id 任务ID
     * @return R Of Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") String id);

    /**
     * 修改转码任务
     *
     * @param rtmp     转码任务
     * @param tenantId 租户ID
     * @return {@link io.github.pnoker.common.dto.RtmpDto}
     */
    @PostMapping("/update")
    R<Rtmp> update(@RequestBody Rtmp rtmp, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 ID 查询转码任务
     *
     * @param id 任务ID
     * @return {@link io.github.pnoker.common.dto.RtmpDto}
     */
    @GetMapping("/id/{id}")
    R<Rtmp> selectById(@NotNull @PathVariable(value = "id") String id);

    /**
     * 模糊分页查询转码任务
     *
     * @param rtmpDto  转码任务和分页参数
     * @param tenantId 租户ID
     * @return 带分页的 {@link io.github.pnoker.common.dto.RtmpDto}
     */
    @PostMapping("/list")
    R<Page<Rtmp>> list(@RequestBody(required = false) RtmpDto rtmpDto, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 启动转码任务
     *
     * @param id 任务ID
     * @return 是否启动
     */
    @PostMapping("/start/{id}")
    R<Boolean> start(@NotNull @PathVariable(value = "id") String id);

    /**
     * 停止转码任务
     *
     * @param id 任务ID
     * @return 是否停止
     */
    @PostMapping("/stop/{id}")
    R<Boolean> stop(@NotNull @PathVariable(value = "id") String id);

}
