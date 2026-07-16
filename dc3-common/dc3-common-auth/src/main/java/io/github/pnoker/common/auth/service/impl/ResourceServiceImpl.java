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

package io.github.pnoker.common.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.ResourceManager;
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.ResourceTreeBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.auth.entity.query.ResourceQuery;
import io.github.pnoker.common.auth.service.ResourceService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.ResourceScopeTypeEnum;
import io.github.pnoker.common.enums.ResourceTypeEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Business service implementation for resource operations.
 *
 * @author linys
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceBuilder resourceBuilder;

    private final ResourceManager resourceManager;

    @Override
    public void add(ResourceBO entityBO) {
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create resource: resource has been duplicated");
        }

        ResourceDO entityDO = resourceBuilder.buildDOByBO(entityBO);
        if (!resourceManager.save(entityDO)) {
            throw new AddException("Failed to create resource");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!resourceManager.removeById(id)) {
            throw new DeleteException("Failed to remove resource");
        }
    }

    @Override
    public void update(ResourceBO entityBO) {
        getDOById(entityBO.getId(), true);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException("Failed to update resource: resource has been duplicated");
        }

        ResourceDO entityDO = resourceBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!resourceManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update resource");
        }
    }

    @Override
    public ResourceBO getById(Long id) {
        ResourceDO entityDO = getDOById(id, true);
        return resourceBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<ResourceBO> list(ResourceQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<ResourceDO> entityPageDO = resourceManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return resourceBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for resource search.
     *
     * @param entityQuery {@link ResourceQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link ResourceDO}
     */
    private LambdaQueryWrapper<ResourceDO> fuzzyQuery(ResourceQuery entityQuery) {
        LambdaQueryWrapper<ResourceDO> wrapper = Wrappers.<ResourceDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getResourceName()), ResourceDO::getResourceName,
                entityQuery.getResourceName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getResourceCode()), ResourceDO::getResourceCode,
                entityQuery.getResourceCode());
        // Type: multi-select wins when present; fall back to single value.
        if (CollectionUtils.isNotEmpty(entityQuery.getResourceTypeFlags())) {
            List<Byte> typeIndexes = entityQuery.getResourceTypeFlags()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(ResourceTypeEnum::getIndex)
                    .toList();
            if (!typeIndexes.isEmpty()) {
                wrapper.in(ResourceDO::getResourceTypeFlag, typeIndexes);
            }
        } else if (Objects.nonNull(entityQuery.getResourceTypeFlag())) {
            wrapper.eq(ResourceDO::getResourceTypeFlag, entityQuery.getResourceTypeFlag().getIndex());
        }
        // Scope: multi-select wins when present; fall back to single value.
        if (CollectionUtils.isNotEmpty(entityQuery.getResourceScopeFlags())) {
            List<Byte> scopeIndexes = entityQuery.getResourceScopeFlags()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(ResourceScopeTypeEnum::getIndex)
                    .toList();
            if (!scopeIndexes.isEmpty()) {
                wrapper.in(ResourceDO::getResourceScopeFlag, scopeIndexes);
            }
        } else if (Objects.nonNull(entityQuery.getResourceScopeFlag())) {
            wrapper.eq(ResourceDO::getResourceScopeFlag, entityQuery.getResourceScopeFlag().getIndex());
        }
        wrapper.eq(Objects.nonNull(entityQuery.getParentResourceId()), ResourceDO::getParentResourceId,
                entityQuery.getParentResourceId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), ResourceDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        return wrapper;
    }

    @Override
    public List<ResourceTreeBO> listTree(ResourceQuery entityQuery) {
        ResourceQuery effective = Objects.requireNonNullElseGet(entityQuery, ResourceQuery::new);
        LambdaQueryWrapper<ResourceDO> wrapper = fuzzyQuery(effective);
        // Load everything that matches, then assemble in memory by parent_resource_id.
        List<ResourceDO> rows = resourceManager.list(wrapper);
        return assembleTree(rows);
    }

    /**
     * Assemble flat resource rows into a tree, linking children to parents by id and
     * sorting each level by type then name.
     *
     * @param rows the flat resource rows
     * @return the assembled tree roots
     */
    private List<ResourceTreeBO> assembleTree(List<ResourceDO> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return List.of();
        }
        Map<Long, ResourceTreeBO> byId = new HashMap<>(rows.size());
        for (ResourceDO row : rows) {
            ResourceTreeBO node = ResourceTreeBO.fromBO(resourceBuilder.buildBOByDO(row));
            byId.put(node.getId(), node);
        }
        List<ResourceTreeBO> roots = new ArrayList<>();
        for (ResourceTreeBO node : byId.values()) {
            Long parentId = node.getParentResourceId();
            ResourceTreeBO parent = Objects.isNull(parentId) || parentId == 0L ? null : byId.get(parentId);
            if (Objects.isNull(parent)) {
                roots.add(node);
            } else {
                parent.addChild(node);
            }
        }
        Comparator<ResourceTreeBO> order = Comparator
                .comparing(ResourceTreeBO::getResourceTypeFlag, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ResourceTreeBO::getResourceName, Comparator.nullsLast(Comparator.naturalOrder()));
        sortRecursive(roots, order);
        return roots;
    }

    /**
     * Recursively sort each level of the resource tree by the given comparator.
     *
     * @param nodes the tree nodes to sort
     * @param order the comparator
     */
    private void sortRecursive(List<ResourceTreeBO> nodes, Comparator<ResourceTreeBO> order) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        nodes.sort(order);
        for (ResourceTreeBO node : nodes) {
            sortRecursive(node.getChildren(), order);
        }
    }

    /**
     * Check whether a resource is duplicated by parent resource, name, code, type,
     * scope, and bound entity. Unlike the throwing variant, this only reports the
     * duplicate without raising an exception.
     *
     * @param entityBO {@link ResourceBO} to be validated
     * @param isUpdate whether the operation is an update (true) or create (false)
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(ResourceBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<ResourceDO> wrapper = Wrappers.<ResourceDO>query().lambda();
        wrapper.eq(ResourceDO::getParentResourceId, entityBO.getParentResourceId());
        wrapper.eq(ResourceDO::getResourceName, entityBO.getResourceName());
        wrapper.eq(ResourceDO::getResourceCode, entityBO.getResourceCode());
        wrapper.eq(ResourceDO::getResourceTypeFlag, entityBO.getResourceTypeFlag());
        wrapper.eq(ResourceDO::getResourceScopeFlag, entityBO.getResourceScopeFlag());
        wrapper.eq(ResourceDO::getEntityId, entityBO.getEntityId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        ResourceDO one = resourceManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    /**
     * Get resource data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link ResourceDO} if found, otherwise {@code null} when {@code throwException}
     * is false
     */
    private ResourceDO getDOById(Long id, boolean throwException) {
        ResourceDO entityDO = resourceManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Resource does not exist");
        }
        return entityDO;
    }

}
