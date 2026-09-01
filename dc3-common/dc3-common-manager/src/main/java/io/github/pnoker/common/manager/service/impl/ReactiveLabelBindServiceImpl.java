package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.manager.entity.bo.LabelBindBO;
import io.github.pnoker.common.manager.repository.BindingFilter;
import io.github.pnoker.common.manager.repository.ReactiveLabelBindStore;
import io.github.pnoker.common.manager.repository.ReactiveLabelStore;
import io.github.pnoker.common.manager.service.ReactiveLabelBindService;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default reactive label binding application service. */
@Service
@RequiredArgsConstructor
public class ReactiveLabelBindServiceImpl implements ReactiveLabelBindService {

    private final ReactiveLabelBindStore labelBindStore;

    private final ReactiveLabelStore labelStore;

    @Override
    public Mono<LabelBindBO> add(LabelBindBO value) {
        return Mono.defer(() -> {
            validate(value, true);
            return validateOwner(value)
                    .then(Mono.defer(() -> ensureUnique(value)))
                    .then(Mono.defer(() -> labelBindStore.insert(value)))
                    .onErrorMap(DataIntegrityViolationException.class,
                            error -> new DuplicateException("Entity has been bound to the label"));
        });
    }

    @Override
    public Mono<LabelBindBO> update(LabelBindBO value) {
        return Mono.defer(() -> {
            validate(value, false);
            return labelBindStore.get(value.getTenantId(), value.getId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Label bind does not exist")))
                    .then(Mono.defer(() -> validateOwner(value)))
                    .then(Mono.defer(() -> ensureUnique(value)))
                    .then(Mono.defer(() -> labelBindStore.update(value)))
                    .switchIfEmpty(Mono.error(new RequestException("Label bind update failed")))
                    .onErrorMap(DataIntegrityViolationException.class,
                            error -> new DuplicateException("Entity has been bound to the label"));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        return labelBindStore.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Label bind does not exist")))
                .then(labelBindStore.delete(tenantId, id, operatorId, operatorName))
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new RequestException("Label bind was already deleted")));
    }

    @Override
    public Mono<LabelBindBO> getById(Long tenantId, Long id) {
        return labelBindStore.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Label bind does not exist")));
    }

    @Override
    public Mono<OffsetPage<LabelBindBO>> list(BindingFilter filter) {
        return labelBindStore.list(filter);
    }

    private Mono<Void> validateOwner(LabelBindBO value) {
        return labelStore.get(value.getTenantId(), value.getLabelId())
                .filter(label -> label.getEntityTypeFlag() == value.getEntityTypeFlag())
                .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")))
                .then();
    }

    private Mono<Void> ensureUnique(LabelBindBO value) {
        return labelBindStore.getByEntity(
                        value.getTenantId(),
                        value.getEntityTypeFlag().getIndex(),
                        value.getLabelId(),
                        value.getEntityId())
                .filter(existing -> value.getId() == null || !existing.getId().equals(value.getId()))
                .flatMap(existing -> Mono.<Void>error(new DuplicateException("Entity has been bound to the label")))
                .then();
    }

    private void validate(LabelBindBO value, boolean add) {
        if (value == null
                || value.getTenantId() == null
                || value.getTenantId() <= 0
                || value.getEntityTypeFlag() == null
                || value.getLabelId() == null
                || value.getLabelId() <= 0
                || value.getEntityId() == null
                || value.getEntityId() <= 0
                || !add && (value.getId() == null || value.getId() <= 0)) {
            throw new RequestException("Tenant, entity type, label and entity are required");
        }
    }

}
