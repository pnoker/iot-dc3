/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * @version 2025.6.0
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

}
