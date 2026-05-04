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
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.query.UserQuery;
import io.github.pnoker.common.auth.entity.vo.UserVO;
import io.github.pnoker.common.auth.service.UserService;
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
 * User Profile Controller (dc3_user)
 *
 * @author pnoker
 * @version 2026.5.5
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.USER_PROFILE_URL_PREFIX)
public class UserController implements BaseController {

    private final UserBuilder userBuilder;
    private final UserService userService;

    public UserController(UserBuilder userBuilder, UserService userService) {
        this.userBuilder = userBuilder;
        this.userService = userService;
    }

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody UserVO entityVO) {
        return getUserHeader().flatMap(header -> {
            try {
                UserBO entityBO = userBuilder.buildBOByVO(entityVO);
                entityBO.setCreatorId(header.getUserId());
                entityBO.setCreatorName(header.getNickName());
                entityBO.setOperatorId(header.getUserId());
                entityBO.setOperatorName(header.getNickName());
                userService.save(entityBO);
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
            userService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody UserVO entityVO) {
        return getUserHeader().flatMap(header -> {
            try {
                UserBO entityBO = userBuilder.buildBOByVO(entityVO);
                entityBO.setOperatorId(header.getUserId());
                entityBO.setOperatorName(header.getNickName());
                userService.update(entityBO);
                return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/id/{id}")
    public Mono<R<UserVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            UserBO entityBO = userService.selectById(id);
            UserVO entityVO = userBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @GetMapping("/name/{name}")
    public Mono<R<UserVO>> selectByName(@NotNull @PathVariable(value = "name") String name) {
        try {
            UserBO entityBO = userService.selectByUserName(name, false);
            if (Objects.isNull(entityBO)) {
                return Mono.just(R.fail(ResponseEnum.NO_RESOURCE.getText()));
            }
            UserVO entityVO = userBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @PostMapping("/list")
    public Mono<R<Page<UserVO>>> list(@RequestBody(required = false) UserQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                UserQuery query = Objects.isNull(entityQuery) ? new UserQuery() : entityQuery;
                // Overwrite whatever the client sent. Tenant scope is a hard
                // boundary, not a filter — a caller cannot reach across tenants.
                query.setTenantId(tenantId);
                Page<UserBO> entityPageBO = userService.selectByPage(query);
                Page<UserVO> entityPageVO = userBuilder.buildVOPageByBOPage(entityPageBO);
                return Mono.just(R.ok(entityPageVO));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

}
