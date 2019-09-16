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

package com.pnoker.transfer.rtmp.feign;

import com.pnoker.common.model.domain.rtmp.Rtmp;
import com.pnoker.common.model.dto.Response;
import com.pnoker.security.BaseAuthConfigurer;
import com.pnoker.transfer.rtmp.hystrix.RtmpDbsFeignApiHystrix;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author    : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@RequestMapping(value = "/api/v3/dbs/rtmp")
@FeignClient(name = "DC3-DBS", fallbackFactory = RtmpDbsFeignApiHystrix.class, configuration = BaseAuthConfigurer.class)
public interface RtmpDbsFeignApi {
    /**
     * Add 新增操作
     *
     * @return
     */
    @PostMapping("/add")
    Response<Boolean> add(@RequestBody Rtmp rtmp);

    /**
     * Delete 删除操作
     *
     * @param id
     * @return
     */
    @DeleteMapping("/delete")
    Response<Boolean> delete(@RequestParam(value = "id") String id);

    /**
     * Update 新增操作
     *
     * @return
     */
    @PutMapping("/update")
    Response<Boolean> update(@RequestBody Rtmp rtmp);

    /**
     * List 查询全部操作
     *
     * @return
     */
    @GetMapping("/list")
    Response<List<Rtmp>> list();

}
