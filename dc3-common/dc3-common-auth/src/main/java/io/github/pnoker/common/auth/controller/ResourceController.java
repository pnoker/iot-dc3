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

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.ResourceTreeBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.query.ResourceQuery;
import io.github.pnoker.common.auth.entity.vo.ResourceTreeVO;
import io.github.pnoker.common.auth.entity.vo.ResourceVO;
import io.github.pnoker.common.auth.service.ResourceService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * REST controller exposing resource management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.RESOURCE_URL_PREFIX)
@RequiredArgsConstructor
public class ResourceController implements BaseController {

    private final ResourceBuilder resourceBuilder;

    private final ResourceService resourceService;

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody ResourceVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
            ResourceBO entityBO = resourceBuilder.buildBOByVO(entityVO);
            entityBO.setCreatorId(header.getUserId());
            entityBO.setCreatorName(header.getNickName());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            resourceService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return async(() -> {
            resourceService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        });
    }

    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody ResourceVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
            ResourceBO entityBO = resourceBuilder.buildBOByVO(entityVO);
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            resourceService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @GetMapping("/get_by_id")
    public Mono<R<ResourceVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return async(() -> {
            ResourceBO entityBO = resourceService.getById(id);
            ResourceVO entityVO = resourceBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    @PostMapping("/list")
    public Mono<R<Page<ResourceVO>>> list(@RequestBody(required = false) ResourceQuery entityQuery) {
        return async(() -> {
            ResourceQuery query = Objects.isNull(entityQuery) ? new ResourceQuery() : entityQuery;
            Page<ResourceBO> entityPageBO = resourceService.list(query);
            Page<ResourceVO> entityPageVO = resourceBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        });
    }

    @PostMapping("/list_tree")
    public Mono<R<List<ResourceTreeVO>>> listTree(@RequestBody(required = false) ResourceQuery entityQuery) {
        return async(() -> {
            List<ResourceTreeBO> entityBOList = resourceService.listTree(entityQuery);
            List<ResourceTreeVO> entityVOList = new ArrayList<>(entityBOList.size());
            for (ResourceTreeBO node : entityBOList) {
                entityVOList.add(toTreeVO(node));
            }
            return R.ok(entityVOList);
        });
    }

    private ResourceTreeVO toTreeVO(ResourceTreeBO node) {
        ResourceVO flat = resourceBuilder.buildVOByBO(node);
        ResourceTreeVO out = new ResourceTreeVO();
        out.setId(flat.getId());
        out.setParentResourceId(flat.getParentResourceId());
        out.setResourceName(flat.getResourceName());
        out.setResourceCode(flat.getResourceCode());
        out.setResourceTypeFlag(flat.getResourceTypeFlag());
        out.setResourceScopeFlag(flat.getResourceScopeFlag());
        out.setEntityId(flat.getEntityId());
        out.setResourceExt(flat.getResourceExt());
        out.setEnableFlag(flat.getEnableFlag());
        out.setRemark(flat.getRemark());
        out.setCreatorId(flat.getCreatorId());
        out.setCreatorName(flat.getCreatorName());
        out.setCreateTime(flat.getCreateTime());
        out.setOperatorId(flat.getOperatorId());
        out.setOperatorName(flat.getOperatorName());
        out.setOperateTime(flat.getOperateTime());
        if (Objects.nonNull(node.getChildren())) {
            List<ResourceTreeVO> childVOs = new ArrayList<>(node.getChildren().size());
            for (ResourceTreeBO child : node.getChildren()) {
                childVOs.add(toTreeVO(child));
            }
            out.setChildren(childVOs);
        }
        return out;
    }

}
