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

package io.github.pnoker.common.auth.controller;

import io.github.pnoker.common.auth.biz.DictionaryForAuthService;
import io.github.pnoker.common.auth.entity.builder.DictionaryForAuthBuilder;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.dal.entity.bo.DictionaryBO;
import io.github.pnoker.common.dal.entity.vo.DictionaryVO;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 字典 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.DICTIONARY_URL_PREFIX)
public class DictionaryForAuthController implements BaseController {

    private final DictionaryForAuthBuilder dictionaryForAuthBuilder;
    private final DictionaryForAuthService dictionaryForAuthService;

    public DictionaryForAuthController(DictionaryForAuthBuilder dictionaryForAuthBuilder, DictionaryForAuthService dictionaryForAuthService) {
        this.dictionaryForAuthBuilder = dictionaryForAuthBuilder;
        this.dictionaryForAuthService = dictionaryForAuthService;
    }

    /**
     * 查询租户字典列表
     *
     * @return 字典列表
     */
    @GetMapping("/tenant")
    public Mono<R<List<DictionaryVO>>> tenantDictionary() {
        try {
            List<DictionaryBO> entityBOList = dictionaryForAuthService.tenantDictionary();
            List<DictionaryVO> entityVOList = dictionaryForAuthBuilder.buildVOListByBOList(entityBOList);
            return Mono.just(R.ok(entityVOList));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 查询限制IP字典列表
     *
     * @return 字典列表
     */
    @GetMapping("/limited_ip")
    public Mono<R<List<DictionaryVO>>> limitedIpDictionary() {
        try {
            List<DictionaryBO> entityBOList = dictionaryForAuthService.limitedIpDictionary(getTenantId());
            List<DictionaryVO> entityVOList = dictionaryForAuthBuilder.buildVOListByBOList(entityBOList);
            return Mono.just(R.ok(entityVOList));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
