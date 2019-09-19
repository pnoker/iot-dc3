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

package com.pnoker.api.dbs.rtmp.feign;

import com.pnoker.common.model.domain.rtmp.Rtmp;
import com.pnoker.common.model.dto.Response;
import com.pnoker.api.security.BaseAuthConfigurer;
import com.pnoker.api.dbs.rtmp.hystrix.RtmpDbsFeignApiHystrix;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author    : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@FeignClient(name = "DC3-DBS", fallbackFactory = RtmpDbsFeignApiHystrix.class, configuration = BaseAuthConfigurer.class)
@RequestMapping(value = "/api/v3/dbs/rtmp")
public interface RtmpDbsFeignApi {
    /**
     * List 查询全部操作
     *
     * @return
     */
    @GetMapping("/list")
    Response<List<Rtmp>> list();

    /**
     * Count 数据统计操作
     *
     * @return
     */
    @GetMapping("/count")
    Response count();

    /**
     * Delete 删除操作
     *
     * @param id
     * @return
     */
    @DeleteMapping("/delete")
    Response delete(@RequestParam(value = "id") String id);
}
