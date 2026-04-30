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
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleResourceBindBuilder;
import io.github.pnoker.common.auth.entity.query.RoleResourceBindQuery;
import io.github.pnoker.common.auth.entity.vo.ResourceVO;
import io.github.pnoker.common.auth.entity.vo.RoleResourceBindVO;
import io.github.pnoker.common.auth.service.RoleResourceBindService;
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
 * RoleResourceBind Controller
 *
 * @author pnoker
 * @version 2026.4.30
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.ROLE_RESOURCE_URL_PREFIX)
public class RoleResourceBindController implements BaseController {

    private final RoleResourceBindBuilder roleResourceBindBuilder;
    private final RoleResourceBindService roleResourceBindService;
    private final ResourceBuilder resourceBuilder;

    public RoleResourceBindController(RoleResourceBindBuilder roleResourceBindBuilder,
                                      RoleResourceBindService roleResourceBindService,
                                      ResourceBuilder resourceBuilder) {
        this.roleResourceBindBuilder = roleResourceBindBuilder;
        this.roleResourceBindService = roleResourceBindService;
        this.resourceBuilder = resourceBuilder;
    }

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RoleResourceBindVO entityVO) {
        try {
            RoleResourceBindBO entityBO = roleResourceBindBuilder.buildBOByVO(entityVO);
            roleResourceBindService.save(entityBO);
            return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            roleResourceBindService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @PostMapping("/list")
    public Mono<R<Page<RoleResourceBindVO>>> list(@RequestBody(required = false) RoleResourceBindQuery entityQuery) {
        try {
            if (Objects.isNull(entityQuery)) {
                entityQuery = new RoleResourceBindQuery();
            }
            Page<RoleResourceBindBO> entityPageBO = roleResourceBindService.selectByPage(entityQuery);
            Page<RoleResourceBindVO> entityPageVO = roleResourceBindBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @GetMapping("/list-resource-by-role/{roleId}")
    public Mono<R<List<ResourceVO>>> listResourceByRole(@NotNull @PathVariable(value = "roleId") Long roleId) {
        try {
            List<ResourceBO> entityBOList = roleResourceBindService.listResourceByRoleId(roleId);
            List<ResourceVO> entityVOList = resourceBuilder.buildVOListByBOList(entityBOList);
            return Mono.just(R.ok(entityVOList));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
