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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.builder.TenantBuilder;
import io.github.pnoker.common.auth.entity.query.TenantQuery;
import io.github.pnoker.common.auth.entity.vo.TenantVO;
import io.github.pnoker.common.auth.service.TenantService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
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
 * REST controller exposing tenant management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.TENANT_URL_PREFIX)
@RequiredArgsConstructor
public class TenantController implements BaseController {

    private final TenantBuilder tenantBuilder;

    private final TenantService tenantService;

    /**
     * Tenant
     *
     * @param entityVO {@link TenantVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody TenantVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
            TenantBO userTenant = tenantService.getById(header.getTenantId());
            if (!"default".equals(userTenant.getTenantCode())) {
                throw new ServiceException("Only system administrators can create tenants");
            }
            TenantBO entityBO = tenantBuilder.buildBOByVO(entityVO);
            entityBO.setCreatorId(header.getUserId());
            entityBO.setCreatorName(header.getNickName());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            tenantService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID Tenant
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            TenantBO userTenant = tenantService.getById(tenantId);
            boolean isSystemAdmin = "default".equals(userTenant.getTenantCode());
            if (!isSystemAdmin && !Objects.equals(id, tenantId)) {
                throw new ServiceException("Access denied: cannot delete another tenant.");
            }
            tenantService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * ID Tenant
     * <ol>
     * <li>: Enable</li>
     * <li>: Name</li>
     * </ol>
     *
     * @param entityVO {@link TenantVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody TenantVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            TenantBO userTenant = tenantService.getById(tenantId);
            boolean isSystemAdmin = "default".equals(userTenant.getTenantCode());
            if (!isSystemAdmin && !Objects.equals(entityVO.getId(), tenantId)) {
                throw new ServiceException("Access denied: cannot update another tenant.");
            }
            TenantBO entityBO = tenantBuilder.buildBOByVO(entityVO);
            tenantService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID Tenant
     *
     * @param id ID
     * @return TenantVO {@link TenantVO}
     */
    @GetMapping("/get_by_id")
    public Mono<R<TenantVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            TenantBO userTenant = tenantService.getById(tenantId);
            boolean isSystemAdmin = "default".equals(userTenant.getTenantCode());
            if (!isSystemAdmin && !Objects.equals(id, tenantId)) {
                throw new NotFoundException("Resource does not exist");
            }
            TenantBO entityBO = tenantService.getById(id);
            TenantVO entityVO = tenantBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Code Tenant
     *
     * @param code TenantCode
     * @return {@link TenantVO}
     */
    @GetMapping("/get_by_code")
    public Mono<R<TenantVO>> getByCode(@NotNull @RequestParam(value = "code") String code) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            TenantBO userTenant = tenantService.getById(tenantId);
            boolean isSystemAdmin = "default".equals(userTenant.getTenantCode());
            TenantBO select = tenantService.getByCode(code);
            if (Objects.isNull(select)) {
                return R.fail(ResponseEnum.NO_RESOURCE.getRemark());
            }
            if (!isSystemAdmin && !Objects.equals(select.getId(), tenantId)) {
                return R.fail("Resource does not exist");
            }
            return R.ok(tenantBuilder.buildVOByBO(select));
        }));
    }

    /**
     * Tenant
     *
     * @param entityQuery Tenant
     * @return {@link TenantBO}
     */
    @PostMapping("/list")
    public Mono<R<Page<TenantVO>>> list(@RequestBody(required = false) TenantQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            TenantBO userTenant = tenantService.getById(tenantId);
            boolean isSystemAdmin = "default".equals(userTenant.getTenantCode());
            TenantQuery query = Objects.isNull(entityQuery) ? new TenantQuery() : entityQuery;
            if (!isSystemAdmin) {
                query.setTenantCode(userTenant.getTenantCode());
            }
            Page<TenantBO> entityPageBO = tenantService.list(query);
            Page<TenantVO> entityPageVO = tenantBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
