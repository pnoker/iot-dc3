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

package com.dc3.center.data.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.data.feign.PointValueClient;
import com.dc3.center.data.service.PointValueService;
import com.dc3.common.bean.R;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.bean.driver.PointValueDto;
import com.dc3.common.constant.Common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

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
    public R<Boolean> correct(String serviceName) {
        try {
            return pointValueService.correct(serviceName) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<String> status(Long deviceId) {
        try {
            String status = pointValueService.status(deviceId);
            return R.ok(status, "ok");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<List<PointValue>> realtime(Long deviceId) {
        try {
            List<PointValue> pointValues = pointValueService.realtime(deviceId);
            if (null != pointValues) {
                return R.ok(pointValues, "ok");
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<PointValue> realtime(Long deviceId, Long pointId) {
        try {
            PointValue pointValue = pointValueService.realtime(deviceId, pointId);
            if (null != pointValue) {
                return R.ok(pointValue, "ok");
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<PointValue> latest(Long deviceId) {
        try {
            PointValue pointValue = pointValueService.latest(deviceId, null);
            if (null != pointValue) {
                return R.ok(pointValue);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<PointValue> latest(Long deviceId, Long pointId) {
        try {
            PointValue pointValue = pointValueService.latest(deviceId, pointId);
            if (null != pointValue) {
                return R.ok(pointValue);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

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

}