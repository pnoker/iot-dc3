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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "dictionary_auth", description = "Authorization dictionaries: manage lookup entries for identity types, permission categories, and other auth-related metadata classifications")
@Slf4j
@RestController
@RequestMapping(AuthConstant.DICTIONARY_URL_PREFIX)
@RequiredArgsConstructor
public class DictionaryForAuthController implements BaseController {

    private final DictionaryForAuthBuilder dictionaryForAuthBuilder;

    private final DictionaryForAuthService dictionaryForAuthService;

    /**
     * List tenants as dictionary options for Auth Center selection pickers.
     *
     * @return the full set of tenant dictionary options (id and display label), unpaged
     */
    @PreAuthorize("@perm.can('dictionary_for_auth', 'get')")
    @Operation(summary = "List Tenant Dictionary", description = "List tenants as dictionary options (id and display label) for Auth Center selection. " +
            "Use to populate tenant pickers such as the membership switcher; returns the full option set without paging.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/tenant")
    public Mono<R<List<DictionaryVO>>> tenantDictionary() {
        return async(() -> {
            List<DictionaryBO> entityBOList = dictionaryForAuthService.tenantDictionary();
            List<DictionaryVO> entityVOList = dictionaryForAuthBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        });
    }

}
