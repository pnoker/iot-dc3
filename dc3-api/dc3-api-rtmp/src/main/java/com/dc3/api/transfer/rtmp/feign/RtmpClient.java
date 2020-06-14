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

package com.dc3.api.transfer.rtmp.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.transfer.rtmp.hystrix.RtmpClientHystrix;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.RtmpDto;
import com.dc3.common.model.Rtmp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;

/**
 * Rtmp转码 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_RTMP_URL_PREFIX, name = Common.Service.DC3_RTMP_SERVICE_NAME, fallbackFactory = RtmpClientHystrix.class)
public interface RtmpClient {

    /**
     * 新增 Rtmp
     *
     * @param rtmp Rtmp
     * @return R<Rtmp>
     */
    @PostMapping("/add")
    R<Rtmp> add(@Validated @RequestBody Rtmp rtmp);

    /**
     * 根据 ID 删除 Rtmp
     *
     * @param id rtmpId
     * @return R<Boolean>
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") Long id);

    /**
     * 修改 Rtmp
     *
     * @param rtmp Rtmp
     * @return R<Rtmp>
     */
    @PostMapping("/update")
    R<Rtmp> update(@RequestBody Rtmp rtmp);

    /**
     * 根据 ID 查询 Rtmp
     *
     * @param id Rtmp Id
     * @return R<Rtmp>
     */
    @GetMapping("/id/{id}")
    R<Rtmp> selectById(@NotNull @PathVariable(value = "id") Long id);

    /**
     * 分页查询 Rtmp
     *
     * @param rtmpDto Rtmp Dto
     * @return R<Page < Rtmp>>
     */
    @PostMapping("/list")
    R<Page<Rtmp>> list(@RequestBody(required = false) RtmpDto rtmpDto);

    /**
     * 启动 Rtmp 转码任务
     *
     * @param id Rtmp Id
     * @return R<Boolean>
     */
    @PostMapping("/start/{id}")
    R<Boolean> start(@NotNull @PathVariable(value = "id") Long id);

    /**
     * 停止 Rtmp 转码任务
     *
     * @param id Rtmp Id
     * @return R<Boolean>
     */
    @PostMapping("/stop/{id}")
    R<Boolean> stop(@NotNull @PathVariable(value = "id") Long id);

}
