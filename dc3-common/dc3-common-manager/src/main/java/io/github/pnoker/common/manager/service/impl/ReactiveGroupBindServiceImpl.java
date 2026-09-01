package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.manager.entity.bo.GroupBindBO;
import io.github.pnoker.common.manager.repository.BindingFilter;
import io.github.pnoker.common.manager.repository.ReactiveGroupBindStore;
import io.github.pnoker.common.manager.repository.ReactiveGroupStore;
import io.github.pnoker.common.manager.service.ReactiveGroupBindService;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default reactive group binding application service. */
@Service
@RequiredArgsConstructor
public class ReactiveGroupBindServiceImpl implements ReactiveGroupBindService {

    private final ReactiveGroupBindStore groupBindStore;

    private final ReactiveGroupStore groupStore;

    @Override
    public Mono<GroupBindBO> add(GroupBindBO value) {
        return Mono.defer(() -> {
            validate(value, true);
            return validateOwner(value)
                    .then(Mono.defer(() -> ensureUnique(value)))
                    .then(Mono.defer(() -> groupBindStore.insert(value)))
                    .onErrorMap(DataIntegrityViolationException.class,
                            error -> new DuplicateException("Entity has been bound to a group"));
        });
    }

    @Override
    public Mono<GroupBindBO> update(GroupBindBO value) {
        return Mono.defer(() -> {
            validate(value, false);
            return groupBindStore.get(value.getTenantId(), value.getId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Group bind does not exist")))
                    .then(Mono.defer(() -> validateOwner(value)))
                    .then(Mono.defer(() -> ensureUnique(value)))
                    .then(Mono.defer(() -> groupBindStore.update(value)))
                    .switchIfEmpty(Mono.error(new RequestException("Group bind update failed")))
                    .onErrorMap(DataIntegrityViolationException.class,
                            error -> new DuplicateException("Entity has been bound to a group"));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        return groupBindStore.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Group bind does not exist")))
                .then(groupBindStore.delete(tenantId, id, operatorId, operatorName))
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new RequestException("Group bind was already deleted")));
    }

    @Override
    public Mono<GroupBindBO> getById(Long tenantId, Long id) {
        return groupBindStore.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Group bind does not exist")));
    }

    @Override
    public Mono<OffsetPage<GroupBindBO>> list(BindingFilter filter) {
        return groupBindStore.list(filter);
    }

    private Mono<Void> validateOwner(GroupBindBO value) {
        return groupStore.get(value.getTenantId(), value.getGroupId())
                .filter(group -> group.getGroupTypeFlag() == value.getEntityTypeFlag())
                .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")))
                .then();
    }

    private Mono<Void> ensureUnique(GroupBindBO value) {
        return groupBindStore.getByEntity(
                        value.getTenantId(),
                        value.getEntityTypeFlag().getIndex(),
                        value.getEntityId())
                .filter(existing -> value.getId() == null || !existing.getId().equals(value.getId()))
                .flatMap(existing -> Mono.<Void>error(new DuplicateException("Entity has been bound to a group")))
                .then();
    }

    private void validate(GroupBindBO value, boolean add) {
        if (value == null
                || value.getTenantId() == null
                || value.getTenantId() <= 0
                || value.getEntityTypeFlag() == null
                || value.getGroupId() == null
                || value.getGroupId() <= 0
                || value.getEntityId() == null
                || value.getEntityId() <= 0
                || !add && (value.getId() == null || value.getId() <= 0)) {
            throw new RequestException("Tenant, entity type, group and entity are required");
        }
    }

}
