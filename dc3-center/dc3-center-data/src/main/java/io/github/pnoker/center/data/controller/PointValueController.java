/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.center.data.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.data.biz.PointValueService;
import io.github.pnoker.center.data.entity.point.PointValue;
import io.github.pnoker.center.data.entity.query.PointValueQuery;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
@RequestMapping(DataConstant.VALUE_URL_PREFIX)
public class PointValueController implements BaseController {

    @Resource
    private PointValueService pointValueService;

    /**
     * 查询最新 PointValue 集合
     *
     * @param pointValueQuery 位号值和分页参数
     * @return 带分页的 {@link PointValue}
     */
    @PostMapping("/latest")
    public R<Page<PointValue>> latest(@RequestBody PointValueQuery pointValueQuery) {
        try {
            if (ObjectUtil.isEmpty(pointValueQuery)) {
                pointValueQuery = new PointValueQuery();
            }
            Page<PointValue> page = pointValueService.latest(pointValueQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 分页查询 PointValue
     *
     * @param pointValueQuery 位号值和分页参数
     * @return 带分页的 {@link PointValue}
     */
    @PostMapping("/list")
    public R<Page<PointValue>> list(@RequestBody(required = false) PointValueQuery pointValueQuery) {
        try {
            if (ObjectUtil.isEmpty(pointValueQuery)) {
                pointValueQuery = new PointValueQuery();
            }
            Page<PointValue> page = pointValueService.list(pointValueQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}