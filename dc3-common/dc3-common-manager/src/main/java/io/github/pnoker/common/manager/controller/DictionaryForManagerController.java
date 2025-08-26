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
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 字典 Controller
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DICTIONARY_URL_PREFIX)
public class DictionaryForManagerController implements BaseController {

    private final DictionaryForManagerBuilder dictionaryForManagerBuilder;
    private final DictionaryForManagerService dictionaryForManagerService;

    public DictionaryForManagerController(DictionaryForManagerBuilder dictionaryForManagerBuilder, DictionaryForManagerService dictionaryForManagerService) {
        this.dictionaryForManagerBuilder = dictionaryForManagerBuilder;
        this.dictionaryForManagerService = dictionaryForManagerService;
    }

    /**
     * 查询驱动 Dictionary
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return R Of DictionaryVO Page
     */
    @PostMapping("/driver")
    public Mono<R<Page<DictionaryVO>>> driverDictionary(@RequestBody(required = false) DictionaryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DictionaryQuery query = Objects.isNull(entityQuery) ? new DictionaryQuery() : entityQuery;
                query.setTenantId(tenantId);
                Page<DictionaryBO> entityPageBO = dictionaryForManagerService.driverDictionary(query);
                Page<DictionaryVO> entityPageVO = dictionaryForManagerBuilder.buildVOPageByBOPage(entityPageBO);
                return Mono.just(R.ok(entityPageVO));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 查询模板 Dictionary
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return R Of DictionaryVO Page
     */
    @PostMapping("/profile")
    public Mono<R<Page<DictionaryVO>>> profileDictionary(@RequestBody(required = false) DictionaryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DictionaryQuery query = Objects.isNull(entityQuery) ? new DictionaryQuery() : entityQuery;
                query.setTenantId(tenantId);
                Page<DictionaryBO> entityPageBO = dictionaryForManagerService.profileDictionary(query);
                Page<DictionaryVO> entityPageVO = dictionaryForManagerBuilder.buildVOPageByBOPage(entityPageBO);
                return Mono.just(R.ok(entityPageVO));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 查询位号 Dictionary
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return R Of DictionaryVO Page
     */
    @PostMapping("/profile_point")
    public Mono<R<Page<DictionaryVO>>> pointDictionaryForProfile(@Validated(Parent.class) @RequestBody(required = false) DictionaryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DictionaryQuery query = Objects.isNull(entityQuery) ? new DictionaryQuery() : entityQuery;
                query.setTenantId(tenantId);
                Page<DictionaryBO> entityPageBO = dictionaryForManagerService.pointDictionaryForProfile(query);
                Page<DictionaryVO> entityPageVO = dictionaryForManagerBuilder.buildVOPageByBOPage(entityPageBO);
                return Mono.just(R.ok(entityPageVO));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 查询位号 Dictionary
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return R Of DictionaryVO Page
     */
    @PostMapping("/device_point")
    public Mono<R<Page<DictionaryVO>>> pointDictionaryForDevice(@Validated(Parent.class) @RequestBody(required = false) DictionaryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DictionaryQuery query = Objects.isNull(entityQuery) ? new DictionaryQuery() : entityQuery;
                query.setTenantId(tenantId);
                Page<DictionaryBO> entityPageBO = dictionaryForManagerService.pointDictionaryForDevice(query);
                Page<DictionaryVO> entityPageVO = dictionaryForManagerBuilder.buildVOPageByBOPage(entityPageBO);
                return Mono.just(R.ok(entityPageVO));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 查询设备 Dictionary
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return R Of DictionaryVO Page
     */
    @PostMapping("/device")
    public Mono<R<Page<DictionaryVO>>> deviceDictionary(@RequestBody(required = false) DictionaryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DictionaryQuery query = Objects.isNull(entityQuery) ? new DictionaryQuery() : entityQuery;
                query.setTenantId(tenantId);
                Page<DictionaryBO> entityPageBO = dictionaryForManagerService.deviceDictionary(query);
                Page<DictionaryVO> entityPageVO = dictionaryForManagerBuilder.buildVOPageByBOPage(entityPageBO);
                return Mono.just(R.ok(entityPageVO));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 查询设备 Dictionary
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return R Of DictionaryVO Page
     */
    @PostMapping("/driver_device")
    public Mono<R<Page<DictionaryVO>>> deviceDictionaryForDriver(@Validated(Parent.class) @RequestBody(required = false) DictionaryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DictionaryQuery query = Objects.isNull(entityQuery) ? new DictionaryQuery() : entityQuery;
                query.setTenantId(tenantId);
                Page<DictionaryBO> entityPageBO = dictionaryForManagerService.deviceDictionaryForDriver(query);
                Page<DictionaryVO> entityPageVO = dictionaryForManagerBuilder.buildVOPageByBOPage(entityPageBO);
                return Mono.just(R.ok(entityPageVO));
            } catch (Exception e) {
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }
}
