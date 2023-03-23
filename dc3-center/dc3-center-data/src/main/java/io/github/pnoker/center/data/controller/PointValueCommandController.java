/*
 * Copyright 2016-present the original author or authors.
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
import io.github.pnoker.center.data.entity.vo.PointValueReadVO;
import io.github.pnoker.center.data.entity.vo.PointValueWriteVO;
import io.github.pnoker.center.data.service.PointValueCommandService;
import io.github.pnoker.common.constant.service.DataServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.point.PointValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping(DataServiceConstant.VALUE_COMMAND_URL_PREFIX)
public class PointValueCommandController {

    @Resource
    private PointValueCommandService pointValueCommandService;

    /**
     * 读指令
     *
     * @param entityVO PointValueReadVO
     * @return PointValue
     */
    @PostMapping("/read")
    public R<PointValue> read(@Validated @RequestBody PointValueReadVO entityVO) {
        try {
            PointValue pointValue = pointValueCommandService.read(entityVO);
            if (ObjectUtil.isNotNull(pointValue)) {
                return R.ok(pointValue);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 写指令
     *
     * @param entityVO PointValueWriteVO
     * @return PointValue
     */
    @PostMapping("/write")
    public R<PointValue> write(@Validated @RequestBody PointValueWriteVO entityVO) {
        try {
            PointValue pointValue = pointValueCommandService.write(entityVO);
            if (ObjectUtil.isNotNull(pointValue)) {
                return R.ok(pointValue);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}