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
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleUserBindBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleUserBindBuilder;
import io.github.pnoker.common.auth.entity.query.RoleUserBindQuery;
import io.github.pnoker.common.auth.entity.vo.RoleUserBindVO;
import io.github.pnoker.common.auth.entity.vo.RoleVO;
import io.github.pnoker.common.auth.service.RoleUserBindService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * RoleUserBind Controller
 *
 * @author pnoker
 * @version 2026.4.30
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.ROLE_USER_URL_PREFIX)
public class RoleUserBindController implements BaseController {

    private final RoleUserBindBuilder roleUserBindBuilder;
    private final RoleUserBindService roleUserBindService;
    private final RoleBuilder roleBuilder;

    public RoleUserBindController(RoleUserBindBuilder roleUserBindBuilder,
                                  RoleUserBindService roleUserBindService,
                                  RoleBuilder roleBuilder) {
        this.roleUserBindBuilder = roleUserBindBuilder;
        this.roleUserBindService = roleUserBindService;
        this.roleBuilder = roleBuilder;
    }

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RoleUserBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> {
            try {
                RoleUserBindBO entityBO = roleUserBindBuilder.buildBOByVO(entityVO);
                roleUserBindService.save(entityBO);
                return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            roleUserBindService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @PostMapping("/list")
    public Mono<R<Page<RoleUserBindVO>>> list(@RequestBody(required = false) RoleUserBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                RoleUserBindQuery query = Objects.isNull(entityQuery) ? new RoleUserBindQuery() : entityQuery;
                Page<RoleUserBindBO> entityPageBO = roleUserBindService.selectByPage(query, tenantId);
                Page<RoleUserBindVO> entityPageVO = roleUserBindBuilder.buildVOPageByBOPage(entityPageBO);
                return Mono.just(R.ok(entityPageVO));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/list-role-by-user/{userId}")
    public Mono<R<List<RoleVO>>> listRoleByUser(@NotNull @PathVariable(value = "userId") Long userId,
                                                @RequestParam(value = "tenantId", required = false) Long tenantId) {
        try {
            List<RoleBO> entityBOList = roleUserBindService.listRoleByTenantIdAndUserId(tenantId, userId);
            List<RoleVO> entityVOList = roleBuilder.buildVOListByBOList(entityBOList);
            return Mono.just(R.ok(entityVOList));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
