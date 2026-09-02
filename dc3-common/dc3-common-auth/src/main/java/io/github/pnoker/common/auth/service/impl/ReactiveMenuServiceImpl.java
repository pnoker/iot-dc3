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

import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.bo.MenuTreeBO;
import io.github.pnoker.common.auth.entity.builder.MenuBuilder;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.auth.repository.MenuFilter;
import io.github.pnoker.common.auth.repository.ReactiveMenuStore;
import io.github.pnoker.common.auth.repository.ReactiveResourceStore;
import io.github.pnoker.common.auth.security.PermissionCacheInvalidator;
import io.github.pnoker.common.auth.service.ReactiveMenuService;
import io.github.pnoker.common.enums.ResourceScopeTypeEnum;
import io.github.pnoker.common.enums.ResourceTypeEnum;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReactiveMenuServiceImpl implements ReactiveMenuService {

    private final ReactiveMenuStore store;
    private final ReactiveResourceStore resourceStore;
    private final MenuBuilder builder;
    private PermissionCacheInvalidator permissionCacheInvalidator;
    private TransactionalOperator transactionalOperator;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPermissionCacheInvalidator(PermissionCacheInvalidator invalidator) {
        this.permissionCacheInvalidator = invalidator;
    }

    @Autowired(required = false)
    void setTransactionalOperator(TransactionalOperator transactionalOperator) {
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<MenuBO> getById(Long id) {
        if (!valid(id)) return Mono.error(new RequestException("Menu ID is required"));
        return Mono.defer(() -> store.getById(id))
                .map(builder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Menu")));
    }

    @Override
    public Mono<OffsetPage<MenuBO>> list(MenuFilter filter) {
        return Mono.defer(() -> store.list(filter))
                .map(page -> OffsetPage.of(
                        page.items().stream().map(builder::buildBOByDO).toList(),
                        page.offset(),
                        page.limit(),
                        page.total()));
    }

    @Override
    public Flux<MenuTreeBO> listTree(MenuFilter filter) {
        return Mono.defer(() -> store.listTree(filter)
                        .map(builder::buildBOByDO)
                        .map(MenuTreeBO::fromBO)
                        .collectList())
                .flatMapMany(this::assembleTree);
    }

    @Override
    public Mono<MenuBO> add(MenuBO menu) {
        Mono<MenuBO> operation = Mono.defer(() -> {
            validate(menu, false);
            if (menu.getMenuCode() == null || menu.getMenuCode().isBlank()) menu.setMenuCode(CodeUtil.getCode());
            else menu.setMenuCode(menu.getMenuCode().trim());
            return requireParent(menu.getParentMenuId())
                    .then(Mono.defer(() -> store.existsDuplicate(menu)))
                    .flatMap(duplicate -> duplicate
                            ? Mono.<MenuBO>error(new DuplicateException("Menu has been duplicated"))
                            : Mono.defer(() -> store.insert(menu))
                                    .switchIfEmpty(Mono.error(new ServiceException("Menu insert returned no row")))
                                    .map(builder::buildBOByDO)
                                    .flatMap(saved -> syncMenuResource(saved)
                                            .doOnSuccess(ignored -> invalidateAll())
                                            .thenReturn(saved)))
                    .onErrorMap(
                            DuplicateKeyException.class,
                            error -> new DuplicateException("Menu code is already in use"));
        });
        return transactional(operation);
    }

    @Override
    public Mono<MenuBO> update(MenuBO menu) {
        Mono<MenuBO> operation = Mono.defer(() -> {
            validate(menu, true);
            if (menu.getMenuCode() == null || menu.getMenuCode().isBlank()) {
                return Mono.error(new RequestException("Menu code is required"));
            }
            menu.setMenuCode(menu.getMenuCode().trim());
            Long parentId = menu.getParentMenuId();
            if (parentId != null && parentId.equals(menu.getId())) {
                return Mono.error(new RequestException("Menu cannot be its own parent"));
            }
            Mono<Void> cycle = parentId == null || parentId == 0L
                    ? Mono.empty()
                    : Mono.defer(() -> store.isDescendant(menu.getId(), parentId))
                            .flatMap(found -> found
                                    ? Mono.<Void>error(new RequestException("Menu parent would create a cycle"))
                                    : Mono.empty());
            return requireParent(parentId)
                    .then(cycle)
                    .then(Mono.defer(() -> store.existsDuplicate(menu)))
                    .flatMap(duplicate -> duplicate
                            ? Mono.<MenuBO>error(new DuplicateException("Menu has been duplicated"))
                            : Mono.defer(() -> store.update(menu))
                                    .map(builder::buildBOByDO)
                                    .flatMap(saved -> syncMenuResource(saved)
                                            .doOnSuccess(ignored -> invalidateAll())
                                            .thenReturn(saved)))
                    .switchIfEmpty(Mono.error(new NotFoundException("Menu")))
                    .onErrorMap(
                            DuplicateKeyException.class,
                            error -> new DuplicateException("Menu code is already in use"));
        });
        return transactional(operation);
    }

    @Override
    public Mono<Void> delete(Long id, Long operatorId, String operatorName) {
        Mono<Void> operation = getById(id)
                .then(Mono.defer(() -> store.hasChildren(id)))
                .flatMap(hasChildren -> hasChildren
                        ? Mono.<Boolean>error(new RequestException("Menu with children cannot be deleted"))
                        : Mono.defer(() -> store.delete(id, operatorId, operatorName))
                                .defaultIfEmpty(false))
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? removeMenuResource(id, operatorId, operatorName)
                                .doOnSuccess(ignored -> invalidateAll())
                                .thenReturn(true)
                        : Mono.just(false))
                .flatMap(deleted ->
                        Boolean.TRUE.equals(deleted) ? Mono.<Void>empty() : Mono.error(new NotFoundException("Menu")));
        return transactional(operation);
    }

    private <T> Mono<T> transactional(Mono<T> operation) {
        return transactionalOperator == null ? operation : transactionalOperator.transactional(operation);
    }

    private Mono<Void> requireParent(Long parentId) {
        if (parentId == null || parentId == 0L) return Mono.empty();
        if (!valid(parentId)) return Mono.error(new RequestException("Menu parent ID is invalid"));
        return Mono.defer(() -> store.getById(parentId))
                .switchIfEmpty(Mono.error(new NotFoundException("Menu parent")))
                .then();
    }

    private Mono<Void> syncMenuResource(MenuBO menu) {
        if (menu == null || !valid(menu.getId())) return Mono.empty();
        Mono<Long> parent = menu.getParentMenuId() == null || menu.getParentMenuId() == 0L
                ? Mono.just(0L)
                : Mono.defer(() -> resourceStore.getByTypeAndEntity(ResourceTypeEnum.MENU, menu.getParentMenuId()))
                        .map(ResourceDO::getId)
                        .switchIfEmpty(Mono.error(new NotFoundException("Menu parent resource")));
        return parent.flatMap(parentId -> Mono.defer(
                        () -> resourceStore.getByTypeAndEntity(ResourceTypeEnum.MENU, menu.getId()))
                .flatMap(existing -> resourceStore
                        .update(resourceMirror(menu, parentId, existing.getId()))
                        .switchIfEmpty(Mono.error(new ServiceException("Menu resource mirror update returned no row"))))
                .switchIfEmpty(Mono.defer(() -> resourceStore.insert(resourceMirror(menu, parentId, null))))
                .then());
    }

    private Mono<Void> removeMenuResource(Long menuId, Long operatorId, String operatorName) {
        return Mono.defer(() -> resourceStore.getByTypeAndEntity(ResourceTypeEnum.MENU, menuId))
                .flatMap(existing -> resourceStore.delete(existing.getId(), operatorId, operatorName))
                .then();
    }

    private io.github.pnoker.common.auth.entity.bo.ResourceBO resourceMirror(MenuBO menu, Long parentId, Long id) {
        io.github.pnoker.common.auth.entity.bo.ResourceBO resource =
                new io.github.pnoker.common.auth.entity.bo.ResourceBO();
        resource.setId(id);
        resource.setParentResourceId(parentId);
        resource.setResourceName(menu.getMenuName());
        resource.setResourceCode("menu:" + (menu.getMenuCode() == null ? "" : menu.getMenuCode()));
        resource.setResourceTypeFlag(ResourceTypeEnum.MENU);
        resource.setResourceScopeFlag(ResourceScopeTypeEnum.LIST);
        resource.setEntityId(menu.getId());
        resource.setEnableFlag(menu.getEnableFlag());
        resource.setRemark(menu.getRemark());
        resource.setOperatorId(menu.getOperatorId());
        resource.setOperatorName(menu.getOperatorName());
        return resource;
    }

    private Flux<MenuTreeBO> assembleTree(List<MenuTreeBO> rows) {
        Map<Long, MenuTreeBO> byId = new HashMap<>(rows.size());
        rows.forEach(node -> node.setChildren(new ArrayList<>()));
        rows.forEach(node -> byId.put(node.getId(), node));
        List<MenuTreeBO> roots = new ArrayList<>();
        rows.forEach(node -> {
            MenuTreeBO parent = node.getParentMenuId() == null || node.getParentMenuId() == 0L
                    ? null
                    : byId.get(node.getParentMenuId());
            if (parent == null) roots.add(node);
            else parent.addChild(node);
        });
        Comparator<MenuTreeBO> order = Comparator.comparing(
                        MenuTreeBO::getMenuIndex, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MenuTreeBO::getMenuName, Comparator.nullsLast(String::compareTo))
                .thenComparing(MenuTreeBO::getId, Comparator.nullsLast(Long::compareTo));
        sort(roots, order);
        return Flux.fromIterable(roots);
    }

    private void sort(List<MenuTreeBO> nodes, Comparator<MenuTreeBO> order) {
        nodes.sort(order);
        nodes.forEach(node -> sort(node.getChildren(), order));
    }

    private void validate(MenuBO menu, boolean update) {
        if (menu == null || (update && !valid(menu.getId()))) throw new RequestException("Menu is invalid");
        if (menu.getMenuName() == null || menu.getMenuName().isBlank())
            throw new RequestException("Menu name is required");
        menu.setMenuName(menu.getMenuName().trim());
    }

    private boolean valid(Long id) {
        return id != null && id > 0;
    }

    private void invalidateAll() {
        if (permissionCacheInvalidator != null) permissionCacheInvalidator.invalidateAll();
    }
}
