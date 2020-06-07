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

package com.dc3.api.center.data.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.data.hystrix.PointValueClientHystrix;
import com.dc3.common.bean.R;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.bean.driver.PointValueDto;
import com.dc3.common.constant.Common;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 数据 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_DATA_URL_PREFIX, name = Common.Service.DC3_DATA_SERVICE_NAME, fallbackFactory = PointValueClientHystrix.class)
public interface PointValueClient {

    /**
     * 分页查询 PointValue
     *
     * @param pointValueDto
     * @return Page<PointValue>
     */
    @PostMapping("/list")
    R<Page<PointValue>> list(@RequestBody(required = false) PointValueDto pointValueDto);

    /**
     * 查询最新 PointValue
     *
     * @param pointValueDto
     * @return PointValue
     */
    @PostMapping("/latest")
    R<PointValue> latest(@RequestBody PointValueDto pointValueDto);

}
