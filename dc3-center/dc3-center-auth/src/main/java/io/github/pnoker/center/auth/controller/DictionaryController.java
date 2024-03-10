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

package io.github.pnoker.center.auth.controller;

import io.github.pnoker.center.auth.biz.DictionaryService;
import io.github.pnoker.center.auth.entity.builder.DictionaryForAuthBuilder;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.bo.DictionaryBO;
import io.github.pnoker.common.entity.vo.DictionaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 字典 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@Tag(name = "接口-字典")
@RequestMapping(AuthConstant.DICTIONARY_URL_PREFIX)
public class DictionaryController implements BaseController {

    @Resource
    private DictionaryForAuthBuilder dictionaryForAuthBuilder;

    @Resource
    private DictionaryService dictionaryService;

    /**
     * 查询租户字典列表
     *
     * @return 字典列表
     */
    @GetMapping("/tenant")
    @Operation(summary = "查询-租户字典列表")
    public R<List<DictionaryVO>> tenantDictionary() {
        try {
            List<DictionaryBO> entityBOS = dictionaryService.tenantDictionary();
            List<DictionaryVO> entityVOS = dictionaryForAuthBuilder.buildVOListByBOList(entityBOS);
            return R.ok(entityVOS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 查询限制IP字典列表
     *
     * @return 字典列表
     */
    @GetMapping("/limited_ip")
    @Operation(summary = "查询-限制IP列表")
    public R<List<DictionaryVO>> limitedIpDictionary() {
        try {
            List<DictionaryBO> entityBOS = dictionaryService.limitedIpDictionary(getTenantId());
            List<DictionaryVO> entityVOS = dictionaryForAuthBuilder.buildVOListByBOList(entityBOS);
            return R.ok(entityVOS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

}
