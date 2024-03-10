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

import io.github.pnoker.center.data.biz.PointValueCommandService;
import io.github.pnoker.center.data.entity.vo.PointValueReadVO;
import io.github.pnoker.center.data.entity.vo.PointValueWriteVO;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.R;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "接口-位号数据指令")
@RequestMapping(DataConstant.VALUE_COMMAND_URL_PREFIX)
public class PointValueCommandController implements BaseController {

    @Resource
    private PointValueCommandService pointValueCommandService;

    /**
     * 读指令
     *
     * @param entityVO PointValueReadVO
     * @return PointValue
     */
    @PostMapping("/read")
    public R<Boolean> read(@Validated @RequestBody PointValueReadVO entityVO) {
        try {
            pointValueCommandService.read(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.ok();
    }

    /**
     * 写指令
     *
     * @param entityVO PointValueWriteVO
     * @return PointValue
     */
    @PostMapping("/write")
    public R<Boolean> write(@Validated @RequestBody PointValueWriteVO entityVO) {
        try {
            pointValueCommandService.write(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.ok();
    }

}