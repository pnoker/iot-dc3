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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resource Controller
 *
 * @author pnoker
 * @version 2026.4.30
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.RESOURCE_URL_PREFIX)
public class ResourceController implements BaseController {

    private final ResourceBuilder resourceBuilder;
    private final ResourceService resourceService;

    public ResourceController(ResourceBuilder resourceBuilder, ResourceService resourceService) {
        this.resourceBuilder = resourceBuilder;
        this.resourceService = resourceService;
    }

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody ResourceVO entityVO) {
        try {
            ResourceBO entityBO = resourceBuilder.buildBOByVO(entityVO);
            resourceService.save(entityBO);
            return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            resourceService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody ResourceVO entityVO) {
        try {
            ResourceBO entityBO = resourceBuilder.buildBOByVO(entityVO);
            resourceService.update(entityBO);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @GetMapping("/id/{id}")
    public Mono<R<ResourceVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            ResourceBO entityBO = resourceService.selectById(id);
            ResourceVO entityVO = resourceBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @PostMapping("/list")
    public Mono<R<Page<ResourceVO>>> list(@RequestBody(required = false) ResourceQuery entityQuery) {
        try {
            if (Objects.isNull(entityQuery)) {
                entityQuery = new ResourceQuery();
            }
            Page<ResourceBO> entityPageBO = resourceService.selectByPage(entityQuery);
            Page<ResourceVO> entityPageVO = resourceBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @PostMapping("/tree")
    public Mono<R<List<ResourceTreeVO>>> tree(@RequestBody(required = false) ResourceQuery entityQuery) {
        try {
            List<ResourceTreeBO> entityBOList = resourceService.selectTree(entityQuery);
            List<ResourceTreeVO> entityVOList = new ArrayList<>(entityBOList.size());
            for (ResourceTreeBO node : entityBOList) {
                entityVOList.add(toTreeVO(node));
            }
            return Mono.just(R.ok(entityVOList));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
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
        if (node.getChildren() != null) {
            List<ResourceTreeVO> childVOs = new ArrayList<>(node.getChildren().size());
            for (ResourceTreeBO child : node.getChildren()) {
                childVOs.add(toTreeVO(child));
            }
            out.setChildren(childVOs);
        }
        return out;
    }

}
