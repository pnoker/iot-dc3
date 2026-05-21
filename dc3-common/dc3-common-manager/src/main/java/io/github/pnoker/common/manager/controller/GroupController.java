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

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.dal.entity.bo.GroupBO;
import io.github.pnoker.common.dal.entity.builder.GroupBuilder;
import io.github.pnoker.common.dal.entity.query.GroupQuery;
import io.github.pnoker.common.dal.entity.vo.GroupVO;
import io.github.pnoker.common.dal.service.GroupService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
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
 * REST controller exposing group management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.GROUP_URL_PREFIX)
@RequiredArgsConstructor
public class GroupController implements BaseController {

    private final GroupBuilder groupBuilder;

    private final GroupService groupService;

    /**
     * @param entityVO {@link GroupVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody GroupVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBO entityBO = groupBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            groupService.add(entityBO);
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
            requireTenant(tenantId, groupService.getById(id));
            groupService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * @param entityVO {@link GroupVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody GroupVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBO entityBO = groupBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, groupService.getById(entityBO.getId()));
            groupService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * @param id ID
     * @return GroupVO {@link GroupVO}
     */
    @GetMapping("/get_by_id")
    public Mono<R<GroupVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBO entityBO = requireTenant(tenantId, groupService.getById(id));
            GroupVO entityVO = groupBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * @param entityQuery {@link GroupQuery}
     * @return R Of GroupVO Page
     */
    @PostMapping("/list")
    public Mono<R<Page<GroupVO>>> list(@RequestBody(required = false) GroupQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupQuery query = Objects.isNull(entityQuery) ? new GroupQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<GroupBO> entityPageBO = groupService.list(query);
            Page<GroupVO> entityPageVO = groupBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
