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
package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.GroupBO;
import io.github.pnoker.common.manager.repository.GroupFilter;
import io.github.pnoker.common.manager.repository.ReactiveGroupStore;
import io.github.pnoker.common.manager.service.ReactiveGroupService;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default reactive group application service. */
@Service
@RequiredArgsConstructor
public class ReactiveGroupServiceImpl implements ReactiveGroupService {

    private static final Set<EntityTypeEnum> SUPPORTED =
            Set.of(EntityTypeEnum.DRIVER, EntityTypeEnum.PROFILE, EntityTypeEnum.POINT, EntityTypeEnum.DEVICE);

    private final ReactiveGroupStore groupStore;

    @Override
    public Mono<GroupBO> add(GroupBO group) {
        return Mono.defer(() -> {
            validate(group, false);
            return normalizeParent(group)
                    .then(Mono.defer(() -> {
                        if (group.getGroupCode() == null || group.getGroupCode().isBlank()) {
                            group.setGroupCode(CodeUtil.getCode());
                        }
                        if (group.getGroupIndex() == null) {
                            group.setGroupIndex((byte) 0);
                        }
                        if (group.getEnableFlag() == null) {
                            group.setEnableFlag(EnableFlagEnum.ENABLE);
                        }
                        return ensureUnique(group).then(groupStore.insert(group));
                    }))
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Group has been duplicated"));
        });
    }

    @Override
    public Mono<GroupBO> update(GroupBO group) {
        return Mono.defer(() -> {
            validate(group, true);
            return groupStore
                    .get(group.getTenantId(), group.getId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Group does not exist")))
                    .flatMap(current -> normalizeParent(group)
                            .then(Mono.defer(() -> ensureUnique(group)))
                            .then(Mono.defer(() -> {
                                if (group.getGroupCode() == null
                                        || group.getGroupCode().isBlank()) {
                                    group.setGroupCode(current.getGroupCode());
                                }
                                if (group.getGroupIndex() == null) {
                                    group.setGroupIndex(current.getGroupIndex());
                                }
                                if (group.getEnableFlag() == null) {
                                    group.setEnableFlag(current.getEnableFlag());
                                }
                                return groupStore.update(group);
                            })))
                    .switchIfEmpty(Mono.error(new RequestException("Group update failed")))
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Group has been duplicated"));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (tenantId == null || id == null) {
            return Mono.error(new RequestException("Tenant ID and group ID are required"));
        }
        return groupStore
                .get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Group does not exist")))
                .then(Mono.zip(groupStore.hasChildren(tenantId, id), groupStore.hasActiveBindings(tenantId, id)))
                .flatMap(state -> {
                    if (state.getT1()) {
                        return Mono.error(
                                new AssociatedException("Failed to remove group: there are subgroups under the group"));
                    }
                    if (state.getT2()) {
                        return Mono.error(new AssociatedException(
                                "Failed to remove group: the group has been bound by another entity"));
                    }
                    return groupStore.delete(tenantId, id, operatorId, operatorName);
                })
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new RequestException("Group was already deleted")));
    }

    @Override
    public Mono<GroupBO> getById(Long tenantId, Long id) {
        if (tenantId == null || id == null) {
            return Mono.error(new RequestException("Tenant ID and group ID are required"));
        }
        return groupStore.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Group does not exist")));
    }

    @Override
    public Mono<OffsetPage<GroupBO>> list(GroupFilter filter) {
        return groupStore.list(filter);
    }

    private Mono<Void> ensureUnique(GroupBO group) {
        return groupStore
                .getByName(
                        group.getTenantId(),
                        group.getGroupTypeFlag().getIndex(),
                        group.getParentGroupId(),
                        group.getGroupName())
                .filter(existing -> group.getId() == null || !existing.getId().equals(group.getId()))
                .flatMap(existing -> Mono.<Void>error(new DuplicateException("Group has been duplicated")))
                .then();
    }

    private Mono<Void> normalizeParent(GroupBO group) {
        Long parentId = group.getParentGroupId();
        if (parentId == null || parentId <= 0) {
            group.setParentGroupId(0L);
            group.setGroupLevel((byte) 0);
            return Mono.empty();
        }
        if (parentId.equals(group.getId())) {
            return Mono.error(new RequestException("Group parent can't be itself"));
        }
        return groupStore
                .get(group.getTenantId(), parentId)
                .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")))
                .flatMap(parent -> {
                    if (parent.getGroupTypeFlag() != group.getGroupTypeFlag()) {
                        return Mono.error(new NotFoundException("Resource does not exist"));
                    }
                    group.setGroupLevel((byte) ((parent.getGroupLevel() == null ? 0 : parent.getGroupLevel()) + 1));
                    return ensureAcyclic(group.getTenantId(), group.getId(), parent, new HashSet<>());
                });
    }

    private Mono<Void> ensureAcyclic(Long tenantId, Long groupId, GroupBO current, Set<Long> visited) {
        if (groupId == null || current == null || current.getId() == null) {
            return Mono.empty();
        }
        if (groupId.equals(current.getId())) {
            return Mono.error(new RequestException("Group parent can't be a descendant group"));
        }
        if (!visited.add(current.getId()) || current.getParentGroupId() == null || current.getParentGroupId() <= 0) {
            return Mono.empty();
        }
        return groupStore
                .get(tenantId, current.getParentGroupId())
                .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")))
                .flatMap(parent -> ensureAcyclic(tenantId, groupId, parent, visited));
    }

    private void validate(GroupBO group, boolean update) {
        if (group == null
                || group.getTenantId() == null
                || group.getTenantId() <= 0
                || group.getGroupName() == null
                || group.getGroupName().isBlank()
                || group.getGroupTypeFlag() == null
                || !SUPPORTED.contains(group.getGroupTypeFlag())
                || update && (group.getId() == null || group.getId() <= 0)) {
            throw new RequestException("Tenant ID, group name and supported group type are required");
        }
        group.setGroupName(group.getGroupName().trim());
    }
}
