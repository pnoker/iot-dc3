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

import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.ResourceTreeBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.repository.ReactiveResourceStore;
import io.github.pnoker.common.auth.repository.ResourceFilter;
import io.github.pnoker.common.auth.security.PermissionCacheInvalidator;
import io.github.pnoker.common.auth.service.ReactiveResourceService;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Default resource service implementation. */
@Service
@RequiredArgsConstructor
public class ReactiveResourceServiceImpl implements ReactiveResourceService {

    private final ReactiveResourceStore store;
    private final ResourceBuilder builder;
    private PermissionCacheInvalidator permissionCacheInvalidator;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPermissionCacheInvalidator(PermissionCacheInvalidator invalidator) {
        this.permissionCacheInvalidator = invalidator;
    }

    @Override
    public Mono<ResourceBO> getById(Long id) {
        if (!valid(id)) return Mono.error(new RequestException("Resource ID is required"));
        return Mono.defer(() -> store.getById(id))
                .map(builder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Resource")));
    }

    @Override
    public Mono<OffsetPage<ResourceBO>> list(ResourceFilter filter) {
        return Mono.defer(() -> store.list(filter))
                .map(page -> OffsetPage.of(
                        page.items().stream().map(builder::buildBOByDO).toList(),
                        page.offset(),
                        page.limit(),
                        page.total()));
    }

    @Override
    public Flux<ResourceTreeBO> listTree(ResourceFilter filter) {
        return store.listTree(filter)
                .map(builder::buildBOByDO)
                .map(ResourceTreeBO::fromBO)
                .collectList()
                .flatMapMany(this::assembleTree);
    }

    @Override
    public Mono<ResourceBO> add(ResourceBO resource) {
        return Mono.defer(() -> {
            validate(resource, false);
            if (resource.getResourceCode() == null || resource.getResourceCode().isBlank()) {
                resource.setResourceCode(CodeUtil.getCode());
            } else {
                resource.setResourceCode(resource.getResourceCode().trim());
            }
            return requireParent(resource.getParentResourceId())
                    .then(Mono.defer(() -> store.existsDuplicate(resource)))
                    .flatMap(duplicate -> duplicate
                            ? Mono.<ResourceBO>error(new DuplicateException("Resource has been duplicated"))
                            : Mono.defer(() -> store.insert(resource))
                                    .switchIfEmpty(Mono.error(new ServiceException("Resource insert returned no row")))
                                    .doOnSuccess(saved -> invalidateAll())
                                    .map(builder::buildBOByDO))
                    .onErrorMap(
                            DuplicateKeyException.class,
                            error -> new DuplicateException("Resource code is already in use"));
        });
    }

    @Override
    public Mono<ResourceBO> update(ResourceBO resource) {
        return Mono.defer(() -> {
            validate(resource, true);
            if (resource.getResourceCode() == null || resource.getResourceCode().isBlank()) {
                return Mono.error(new RequestException("Resource code is required"));
            }
            resource.setResourceCode(resource.getResourceCode().trim());
            Long parentId = resource.getParentResourceId();
            if (parentId != null && parentId.equals(resource.getId())) {
                return Mono.error(new RequestException("Resource cannot be its own parent"));
            }
            Mono<Void> parent = requireParent(parentId);
            Mono<Void> cycle = parentId == null || parentId == 0L
                    ? Mono.empty()
                    : Mono.defer(() -> {
                        Mono<Boolean> descendant = store.isDescendant(resource.getId(), parentId);
                        if (descendant == null) return Mono.empty();
                        return descendant.flatMap(cycleDetected -> cycleDetected
                                ? Mono.<Void>error(new RequestException("Resource parent would create a cycle"))
                                : Mono.empty());
                    });
            return parent.then(cycle)
                    .then(Mono.defer(() -> store.existsDuplicate(resource)))
                    .flatMap(duplicate -> duplicate
                            ? Mono.<ResourceBO>error(new DuplicateException("Resource has been duplicated"))
                            : Mono.defer(() -> store.update(resource))
                                    .doOnSuccess(saved -> invalidateAll())
                                    .map(builder::buildBOByDO))
                    .switchIfEmpty(Mono.error(new NotFoundException("Resource")))
                    .onErrorMap(
                            DuplicateKeyException.class,
                            error -> new DuplicateException("Resource code is already in use"));
        });
    }

    @Override
    public Mono<Void> delete(Long id, Long operatorId, String operatorName) {
        return getById(id)
                .then(Mono.defer(() -> store.hasChildren(id)))
                .flatMap(hasChildren -> hasChildren
                        ? Mono.<Boolean>error(new RequestException("Resource with children cannot be deleted"))
                        : Mono.defer(() -> store.delete(id, operatorId, operatorName))
                                .defaultIfEmpty(false)
                                .doOnSuccess(deleted -> invalidateAll()))
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.<Void>empty()
                        : Mono.error(new NotFoundException("Resource")));
    }

    private Mono<Void> requireParent(Long parentId) {
        if (parentId == null || parentId == 0L) return Mono.empty();
        if (!valid(parentId)) return Mono.error(new RequestException("Resource parent ID is invalid"));
        return Mono.defer(() -> store.getById(parentId))
                .switchIfEmpty(Mono.error(new NotFoundException("Resource parent")))
                .then();
    }

    private Flux<ResourceTreeBO> assembleTree(List<ResourceTreeBO> rows) {
        Map<Long, ResourceTreeBO> byId = new HashMap<>(rows.size());
        rows.forEach(node -> node.setChildren(new ArrayList<>()));
        rows.forEach(node -> byId.put(node.getId(), node));
        List<ResourceTreeBO> roots = new ArrayList<>();
        rows.forEach(node -> {
            ResourceTreeBO parent = node.getParentResourceId() == null || node.getParentResourceId() == 0L
                    ? null
                    : byId.get(node.getParentResourceId());
            if (parent == null) roots.add(node);
            else parent.addChild(node);
        });
        Comparator<ResourceTreeBO> order = Comparator.comparing(
                        ResourceTreeBO::getResourceTypeFlag, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ResourceTreeBO::getResourceName, Comparator.nullsLast(String::compareTo))
                .thenComparing(ResourceTreeBO::getId, Comparator.nullsLast(Long::compareTo));
        sort(roots, order);
        return Flux.fromIterable(roots);
    }

    private void sort(List<ResourceTreeBO> nodes, Comparator<ResourceTreeBO> order) {
        nodes.sort(order);
        nodes.forEach(node -> sort(node.getChildren(), order));
    }

    private void validate(ResourceBO resource, boolean update) {
        if (resource == null || (update && !valid(resource.getId()))) {
            throw new RequestException("Resource is invalid");
        }
        if (resource.getResourceName() == null || resource.getResourceName().isBlank()) {
            throw new RequestException("Resource name is required");
        }
        if (resource.getEntityId() == null || resource.getEntityId() < 0) {
            throw new RequestException("Resource entity ID is invalid");
        }
        resource.setResourceName(resource.getResourceName().trim());
        resource.setServiceName(
                resource.getServiceName() == null
                        ? ""
                        : resource.getServiceName().trim());
    }

    private boolean valid(Long id) {
        return id != null && id > 0;
    }

    private void invalidateAll() {
        if (permissionCacheInvalidator != null) permissionCacheInvalidator.invalidateAll();
    }
}
