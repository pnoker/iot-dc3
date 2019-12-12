/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.dbs.api.rtmp.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.bean.Response;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.transfer.RtmpDto;
import com.pnoker.common.entity.rtmp.Rtmp;
import com.pnoker.dbs.api.rtmp.hystrix.RtmpDbsFeignHystrix;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>Rtmp 数据 FeignClient
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@FeignClient(path = Common.Service.DC3_DBS_RTMP_URL_PREFIX, name = Common.Service.DC3_DBS, fallbackFactory = RtmpDbsFeignHystrix.class)
public interface RtmpDbsFeignClient {

    /**
     * 新增 新增 Rtmp 任务记录
     *
     * @param rtmp
     * @return rtmpId
     */
    @PostMapping("/add")
    Response<Long> add(@RequestBody Rtmp rtmp);

    /**
     * 删除 根据 ID 删除 Rtmp
     *
     * @param id rtmpId
     * @return true/false
     */
    @PostMapping("/delete/{id}")
    Response<Boolean> delete(@PathVariable(value = "id") Long id);

    /**
     * 修改 修改 Rtmp 任务记录
     *
     * @param rtmp
     * @return true/false
     */
    @PostMapping("/update")
    Response<Boolean> update(@RequestBody Rtmp rtmp);

    /**
     * 查询 根据ID查询 Rtmp
     *
     * @param id
     * @return rtmp
     */
    @GetMapping("/id/{id}")
    Response<Rtmp> selectById(@PathVariable(value = "id") Long id);

    /**
     * 分页查询 Rtmp
     *
     * @param rtmpDto
     * @return rtmpList
     */
    @PostMapping("/list")
    Response<Page<Rtmp>> list(@RequestBody(required = false) RtmpDto rtmpDto);

}
