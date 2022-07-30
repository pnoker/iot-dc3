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

package io.github.pnoker.center.data.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.data.feign.PointValueClient;
import io.github.pnoker.center.data.service.PointValueService;
import io.github.pnoker.common.bean.Pages;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.PointValueDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Data.VALUE_URL_PREFIX)
public class PointValueApi implements PointValueClient {

    @Resource
    private PointValueService pointValueService;

    @Override
    public R<List<PointValue>> latest(String deviceId, Boolean history) {
        try {
            List<PointValue> pointValues = pointValueService.realtime(deviceId);
            if (null == pointValues) {
                pointValues = pointValueService.latest(deviceId);
            }
            if (null != pointValues) {
                // 返回最近100个非字符类型的历史值
                if (history) {
                    pointValues.forEach(pointValue -> {
                        PointValueDto pointValueDto = (new PointValueDto()).setDeviceId(deviceId).setPointId(pointValue.getPointId()).setPage((new Pages()).setSize(100));
                        Page<PointValue> page = pointValueService.list(pointValueDto);
                        if (null != page) {
                            pointValue.setChildren(page.getRecords().stream()
                                    .map(pointValueChild -> pointValueChild.setId(null).setDeviceId(null).setPointId(null)).collect(Collectors.toList()));
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
    public R<PointValue> latest(String deviceId, String pointId, Boolean history) {
        try {
            PointValue pointValue = pointValueService.realtime(deviceId, pointId);
            if (null == pointValue) {
                pointValue = pointValueService.latest(deviceId, pointId);
            }
            if (null != pointValue) {
                // 返回最近100个非字符类型的历史值
                if (history) {
                    PointValueDto pointValueDto = (new PointValueDto()).setDeviceId(deviceId).setPointId(pointId).setPage((new Pages()).setSize(100));
                    Page<PointValue> page = pointValueService.list(pointValueDto);
                    if (null != page) {
                        pointValue.setChildren(page.getRecords().stream()
                                .map(pointValueChild -> pointValueChild.setId(null).setDeviceId(null).setPointId(null)).collect(Collectors.toList()));
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