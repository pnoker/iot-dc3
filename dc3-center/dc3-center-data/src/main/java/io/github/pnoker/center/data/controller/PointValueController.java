/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.data.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.data.dto.PointValueDto;
import io.github.pnoker.center.data.service.PointValueService;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.constant.service.DataServiceConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * PointValue Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(DataServiceConstant.VALUE_URL_PREFIX)
public class PointValueController {

    @Resource
    private PointValueService pointValueService;

    /**
     * 查询最新 PointValue 集合
     *
     * @param pointValueDto 位号值和分页参数
     * @param tenantId      租户ID
     * @return 带分页的 {@link io.github.pnoker.common.bean.point.PointValue}
     */
    @PostMapping("/latest")
    public R<Page<PointValue>> latest(PointValueDto pointValueDto, String tenantId) {
        try {
            if (ObjectUtil.isEmpty(pointValueDto)) {
                pointValueDto = new PointValueDto();
            }
            Page<PointValue> page = pointValueService.latest(pointValueDto, tenantId);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 模糊分页查询 PointValue
     *
     * @param pointValueDto 位号值和分页参数
     * @param tenantId      租户ID
     * @return 带分页的 {@link io.github.pnoker.common.bean.point.PointValue}
     */
    @PostMapping("/list")
    public R<Page<PointValue>> list(PointValueDto pointValueDto, String tenantId) {
        try {
            if (ObjectUtil.isEmpty(pointValueDto)) {
                pointValueDto = new PointValueDto();
            }
            Page<PointValue> page = pointValueService.list(pointValueDto, tenantId);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}