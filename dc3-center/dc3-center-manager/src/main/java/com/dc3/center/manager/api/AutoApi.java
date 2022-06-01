/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
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

package com.dc3.center.manager.api;

import com.dc3.api.center.manager.feign.AutoClient;
import com.dc3.center.manager.service.AutoService;
import com.dc3.common.bean.R;
import com.dc3.common.bean.point.PointDetail;
import com.dc3.common.constant.ServiceConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 自发现 Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Manager.AUTO_URL_PREFIX)
public class AutoApi implements AutoClient {

    @Resource
    private AutoService autoService;

    @Override
    public R<PointDetail> autoCreateDeviceAndPoint(PointDetail pointDetail, String tenantId) {
        try {
            PointDetail createDeviceAndPoint = autoService.autoCreateDeviceAndPoint(pointDetail.getDeviceName(), pointDetail.getPointName(), pointDetail.getDriverId(), tenantId);
            if (null != createDeviceAndPoint) {
                return R.ok(createDeviceAndPoint);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
