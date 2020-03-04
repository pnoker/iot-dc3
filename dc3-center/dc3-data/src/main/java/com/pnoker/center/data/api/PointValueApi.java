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

package com.pnoker.center.data.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.data.user.feign.PointValueClient;
import com.pnoker.center.data.service.PointValueService;
import com.pnoker.common.bean.R;
import com.pnoker.common.bean.driver.PointValue;
import com.pnoker.common.bean.driver.PointValueDto;
import com.pnoker.common.constant.Common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_DATA_URL_PREFIX)
public class PointValueApi implements PointValueClient {
    @Resource
    private PointValueService pointValueService;

    @Override
    public R<Page<PointValue>> list(PointValueDto pointValueDto) {
        try {
            Page<PointValue> page = pointValueService.list(pointValueDto);
            if (null != page) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<PointValue> latest(PointValueDto pointValueDto) {
        try {
            PointValue pointValue = pointValueService.latest(pointValueDto);
            if (null != pointValue) {
                return R.ok(pointValue);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}