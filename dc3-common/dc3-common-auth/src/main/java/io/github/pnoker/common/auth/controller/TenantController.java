/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 用户 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.TENANT_URL_PREFIX)
public class TenantController implements BaseController {

    private final TenantBuilder tenantBuilder;
    private final TenantService tenantService;

    public TenantController(TenantBuilder tenantBuilder, TenantService tenantService) {
        this.tenantBuilder = tenantBuilder;
        this.tenantService = tenantService;
    }

    /**
     * 新增租户
     *
     * @param entityVO {@link TenantVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody TenantVO entityVO) {
        try {
            TenantBO entityBO = tenantBuilder.buildBOByVO(entityVO);
            tenantService.save(entityBO);
            return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 删除租户
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            tenantService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 更新租户
     * <ol>
     * <li>支持更新: Enable</li>
     * <li>不支持更新: Name</li>
     * </ol>
     *
     * @param entityVO {@link TenantVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody TenantVO entityVO) {
        try {
            TenantBO entityBO = tenantBuilder.buildBOByVO(entityVO);
            tenantService.update(entityBO);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 查询租户
     *
     * @param id ID
     * @return TenantVO {@link TenantVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<TenantVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            TenantBO entityBO = tenantService.selectById(id);
            TenantVO entityVO = tenantBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 Code 查询租户
     *
     * @param code 租户Code
     * @return {@link TenantBO}
     */
    @GetMapping("/code/{code}")
    public Mono<R<TenantBO>> selectByCode(@NotNull @PathVariable(value = "code") String code) {
        try {
            TenantBO select = tenantService.selectByCode(code);
            if (Objects.nonNull(select)) {
                return Mono.just(R.ok(select));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
        return Mono.just(R.fail(ResponseEnum.NO_RESOURCE.getText()));
    }

    /**
     * 分页查询租户
     *
     * @param entityQuery 租户和分页参数
     * @return 带分页的 {@link TenantBO}
     */
    @PostMapping("/list")
    public Mono<R<Page<TenantVO>>> list(@RequestBody(required = false) TenantQuery entityQuery) {
        try {
            if (Objects.isNull(entityQuery)) {
                entityQuery = new TenantQuery();
            }
            Page<TenantBO> entityPageBO = tenantService.selectByPage(entityQuery);
            Page<TenantVO> entityPageVO = tenantBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
