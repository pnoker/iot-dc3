/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.dc3.common.bean.Pages;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.PointValueDto;
import com.dc3.common.model.PointValue;
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
@RequestMapping(Common.Service.DC3_DATA_POINT_VALUE_URL_PREFIX)
public class PointValueApi implements PointValueClient {

    @Resource
    private PointValueService pointValueService;

    @Override
    public R<List<PointValue>> latest(Long deviceId, Boolean history) {
        try {
            List<PointValue> pointValues = pointValueService.realtime(deviceId);
            if (null == pointValues) {
                pointValues = pointValueService.latest(deviceId);
            }
            if (null != pointValues) {
                if (history) {
                    pointValues.forEach(pointValue -> {
                        PointValueDto pointValueDto = (new PointValueDto()).setDeviceId(deviceId).setPointId(pointValue.getPointId()).setPage((new Pages()).setSize(100L));
                        Page<PointValue> page = pointValueService.list(pointValueDto);
                        if (null != page) {
                            pointValue.setChildren(page.getRecords());
                        }
                    });
                }
                return R.ok(pointValues);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<PointValue> latest(Long deviceId, Long pointId, Boolean history) {
        try {
            PointValue pointValue = pointValueService.realtime(deviceId, pointId);
            if (null == pointValue) {
                pointValue = pointValueService.latest(deviceId, pointId);
            }
            if (null != pointValue) {
                if (history) {
                    PointValueDto pointValueDto = (new PointValueDto()).setDeviceId(deviceId).setPointId(pointId).setPage((new Pages()).setSize(100L));
                    Page<PointValue> page = pointValueService.list(pointValueDto);
                    if (null != page) {
                        pointValue.setChildren(page.getRecords());
                    }
                }
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