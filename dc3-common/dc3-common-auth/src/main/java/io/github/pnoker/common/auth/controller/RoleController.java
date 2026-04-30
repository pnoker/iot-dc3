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
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.query.RoleQuery;
import io.github.pnoker.common.auth.entity.vo.RoleVO;
import io.github.pnoker.common.auth.service.RoleService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Role Controller
 *
 * @author pnoker
 * @version 2026.4.30
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.ROLE_URL_PREFIX)
public class RoleController implements BaseController {

    private final RoleBuilder roleBuilder;
    private final RoleService roleService;

    public RoleController(RoleBuilder roleBuilder, RoleService roleService) {
        this.roleBuilder = roleBuilder;
        this.roleService = roleService;
    }

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RoleVO entityVO) {
        try {
            RoleBO entityBO = roleBuilder.buildBOByVO(entityVO);
            roleService.save(entityBO);
            return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            roleService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody RoleVO entityVO) {
        try {
            RoleBO entityBO = roleBuilder.buildBOByVO(entityVO);
            roleService.update(entityBO);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @GetMapping("/id/{id}")
    public Mono<R<RoleVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            RoleBO entityBO = roleService.selectById(id);
            RoleVO entityVO = roleBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @PostMapping("/list")
    public Mono<R<Page<RoleVO>>> list(@RequestBody(required = false) RoleQuery entityQuery) {
        try {
            if (Objects.isNull(entityQuery)) {
                entityQuery = new RoleQuery();
            }
            Page<RoleBO> entityPageBO = roleService.selectByPage(entityQuery);
            Page<RoleVO> entityPageVO = roleBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
