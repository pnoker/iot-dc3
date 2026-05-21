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
import io.github.pnoker.common.dal.entity.bo.LabelBO;
import io.github.pnoker.common.dal.entity.bo.LabelBindBO;
import io.github.pnoker.common.dal.entity.builder.LabelBindBuilder;
import io.github.pnoker.common.dal.entity.query.LabelBindQuery;
import io.github.pnoker.common.dal.entity.vo.LabelBindVO;
import io.github.pnoker.common.dal.service.LabelBindService;
import io.github.pnoker.common.dal.service.LabelService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.manager.service.ProfileService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * REST controller exposing label binding management endpoints.
 *
 * @author pnoker
 * @version 2026.5.11
 * @since 2026.5.11
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.LABEL_BIND_URL_PREFIX)
@RequiredArgsConstructor
public class LabelBindController implements BaseController {

    private final LabelBindBuilder labelBindBuilder;

    private final LabelBindService labelBindService;

    private final LabelService labelService;

    private final DriverService driverService;

    private final ProfileService profileService;

    private final PointService pointService;

    private final DeviceService deviceService;

    /**
     * @param entityVO {@link LabelBindVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody LabelBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBindBO entityBO = labelBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            validateBind(tenantId, entityBO);
            labelBindService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, labelBindService.getById(id));
            labelBindService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * @param entityVO {@link LabelBindVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody LabelBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBindBO entityBO = labelBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, labelBindService.getById(entityBO.getId()));
            validateBind(tenantId, entityBO);
            labelBindService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * @param id ID
     * @return LabelBindVO {@link LabelBindVO}
     */
    @GetMapping("/get_by_id")
    public Mono<R<LabelBindVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBindBO entityBO = requireTenant(tenantId, labelBindService.getById(id));
            LabelBindVO entityVO = labelBindBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * @param entityQuery {@link LabelBindQuery}
     * @return R Of LabelBindVO Page
     */
    @PostMapping("/list")
    public Mono<R<Page<LabelBindVO>>> list(@RequestBody(required = false) LabelBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBindQuery query = Objects.isNull(entityQuery) ? new LabelBindQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<LabelBindBO> entityPageBO = labelBindService.list(query);
            Page<LabelBindVO> entityPageVO = labelBindBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    private void validateBind(Long tenantId, LabelBindBO entityBO) {
        EntityTypeFlagEnum entityTypeFlag = entityBO.getEntityTypeFlag();
        LabelBO labelBO = requireTenant(tenantId, labelService.getById(entityBO.getLabelId()));
        if (!Objects.equals(labelBO.getEntityTypeFlag(), entityTypeFlag)) {
            throw new NotFoundException("Resource does not exist");
        }
        requireEntityTenant(tenantId, entityTypeFlag, entityBO.getEntityId());
    }

    private void requireEntityTenant(Long tenantId, EntityTypeFlagEnum entityTypeFlag, Long entityId) {
        switch (entityTypeFlag) {
            case DRIVER -> requireTenant(tenantId, driverService.getById(entityId));
            case PROFILE -> requireTenant(tenantId, profileService.getById(entityId));
            case POINT -> requireTenant(tenantId, pointService.getById(entityId));
            case DEVICE -> requireTenant(tenantId, deviceService.getById(entityId));
            default -> throw new NotFoundException("Resource does not exist");
        }
    }

}
