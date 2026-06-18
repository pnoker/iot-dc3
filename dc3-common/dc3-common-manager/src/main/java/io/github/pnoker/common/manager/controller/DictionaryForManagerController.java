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

package io.github.pnoker.common.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.dal.entity.bo.DictionaryBO;
import io.github.pnoker.common.dal.entity.vo.DictionaryVO;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.manager.biz.DictionaryForManagerService;
import io.github.pnoker.common.manager.entity.builder.DictionaryForManagerBuilder;
import io.github.pnoker.common.manager.entity.query.DictionaryQuery;
import io.github.pnoker.common.valid.Parent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * REST controller exposing dictionary management endpoints for the manager module.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "dictionary_manager", description = "Device-management dictionaries: manage lookup entries for driver types, device categories, point classifications, and other operational metadata")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DICTIONARY_URL_PREFIX)
@RequiredArgsConstructor
public class DictionaryForManagerController implements BaseController {

    private final DictionaryForManagerBuilder dictionaryForManagerBuilder;

    private final DictionaryForManagerService dictionaryForManagerService;

    /**
     * Dictionary
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return R Of DictionaryVO Page
     */
    @PreAuthorize("@perm.can('dictionary_for_manager', 'list')")
    @Operation(summary = "List Driver Dictionary", description = "Page through driver dictionary entries for the current tenant. " +
            "Use to pick a driver (protocol adapter) when registering or reassigning a device; returns a page of driver dictionaries.")
    @PostMapping("/driver")
    public Mono<R<Page<DictionaryVO>>> driverDictionary(@RequestBody(required = false) DictionaryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DictionaryQuery query = Objects.isNull(entityQuery) ? new DictionaryQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<DictionaryBO> entityPageBO = dictionaryForManagerService.driverDictionary(query);
            Page<DictionaryVO> entityPageVO = dictionaryForManagerBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Dictionary
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return R Of DictionaryVO Page
     */
    @PreAuthorize("@perm.can('dictionary_for_manager', 'list')")
    @Operation(summary = "List Profile Dictionary", description = "Page through profile template dictionaries for the current tenant. " +
            "Use to select which profile template a device instantiates; returns a page of profile dictionaries.")
    @PostMapping("/profile")
    public Mono<R<Page<DictionaryVO>>> profileDictionary(@RequestBody(required = false) DictionaryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DictionaryQuery query = Objects.isNull(entityQuery) ? new DictionaryQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<DictionaryBO> entityPageBO = dictionaryForManagerService.profileDictionary(query);
            Page<DictionaryVO> entityPageVO = dictionaryForManagerBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Dictionary
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return R Of DictionaryVO Page
     */
    @PreAuthorize("@perm.can('dictionary_for_manager', 'list')")
    @Operation(summary = "List Profile Point Dictionary", description = "Page through the point dictionaries declared on a given profile template. " +
            "Use to discover which measurable channels a profile exposes; the query must carry a valid profile parent id.")
    @PostMapping("/profile_point")
    public Mono<R<Page<DictionaryVO>>> pointDictionaryForProfile(
            @Validated(Parent.class) @RequestBody(required = false) DictionaryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DictionaryQuery query = Objects.isNull(entityQuery) ? new DictionaryQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<DictionaryBO> entityPageBO = dictionaryForManagerService.pointDictionaryForProfile(query);
            Page<DictionaryVO> entityPageVO = dictionaryForManagerBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Dictionary
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return R Of DictionaryVO Page
     */
    @PreAuthorize("@perm.can('dictionary_for_manager', 'list')")
    @Operation(summary = "List Device Point Dictionary", description = "Page through the point dictionaries available on a given device. " +
            "Use to pick a target point before reading values or sending commands; the query must carry a valid device parent id.")
    @PostMapping("/device_point")
    public Mono<R<Page<DictionaryVO>>> pointDictionaryForDevice(
            @Validated(Parent.class) @RequestBody(required = false) DictionaryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DictionaryQuery query = Objects.isNull(entityQuery) ? new DictionaryQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<DictionaryBO> entityPageBO = dictionaryForManagerService.pointDictionaryForDevice(query);
            Page<DictionaryVO> entityPageVO = dictionaryForManagerBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Dictionary
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return R Of DictionaryVO Page
     */
    @PreAuthorize("@perm.can('dictionary_for_manager', 'list')")
    @Operation(summary = "List Device Dictionary", description = "Page through device dictionaries for the current tenant. " +
            "Use to select a target device for value reads, commands or configuration; returns a page of device dictionaries.")
    @PostMapping("/device")
    public Mono<R<Page<DictionaryVO>>> deviceDictionary(@RequestBody(required = false) DictionaryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DictionaryQuery query = Objects.isNull(entityQuery) ? new DictionaryQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<DictionaryBO> entityPageBO = dictionaryForManagerService.deviceDictionary(query);
            Page<DictionaryVO> entityPageVO = dictionaryForManagerBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Dictionary
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return R Of DictionaryVO Page
     */
    @PreAuthorize("@perm.can('dictionary_for_manager', 'list')")
    @Operation(summary = "List Driver Device Dictionary", description = "Page through the device dictionaries managed by a given driver. " +
            "Use to find which devices a protocol adapter is responsible for; the query must carry a valid driver parent id.")
    @PostMapping("/driver_device")
    public Mono<R<Page<DictionaryVO>>> deviceDictionaryForDriver(
            @Validated(Parent.class) @RequestBody(required = false) DictionaryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DictionaryQuery query = Objects.isNull(entityQuery) ? new DictionaryQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<DictionaryBO> entityPageBO = dictionaryForManagerService.deviceDictionaryForDriver(query);
            Page<DictionaryVO> entityPageVO = dictionaryForManagerBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
