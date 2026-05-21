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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST controller exposing auth-related dictionary lookup endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.DICTIONARY_URL_PREFIX)
@RequiredArgsConstructor
public class DictionaryForAuthController implements BaseController {

    private final DictionaryForAuthBuilder dictionaryForAuthBuilder;

    private final DictionaryForAuthService dictionaryForAuthService;

    /**
     * Tenant
     *
     * @return
     */
    @GetMapping("/tenant")
    public Mono<R<List<DictionaryVO>>> tenantDictionary() {
        return async(() -> {
            List<DictionaryBO> entityBOList = dictionaryForAuthService.tenantDictionary();
            List<DictionaryVO> entityVOList = dictionaryForAuthBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        });
    }

}
