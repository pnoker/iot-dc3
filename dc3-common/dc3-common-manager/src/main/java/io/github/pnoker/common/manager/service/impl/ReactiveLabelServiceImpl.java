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
import io.github.pnoker.common.manager.entity.bo.LabelBO;
import io.github.pnoker.common.manager.repository.LabelFilter;
import io.github.pnoker.common.manager.repository.ReactiveLabelStore;
import io.github.pnoker.common.manager.service.ReactiveLabelService;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default reactive label application service. */
@Service
@RequiredArgsConstructor
public class ReactiveLabelServiceImpl implements ReactiveLabelService {
    private static final Set<EntityTypeEnum> SUPPORTED =
            Set.of(EntityTypeEnum.DRIVER, EntityTypeEnum.PROFILE, EntityTypeEnum.POINT, EntityTypeEnum.DEVICE);
    private final ReactiveLabelStore labelStore;

    @Override
    public Mono<LabelBO> add(LabelBO label) {
        return Mono.defer(() -> {
            validate(label, false);
            if (label.getLabelCode() == null || label.getLabelCode().isBlank()) label.setLabelCode(CodeUtil.getCode());
            if (label.getLabelColor() == null || label.getLabelColor().isBlank()) label.setLabelColor("#F4F4F5");
            if (label.getEnableFlag() == null) label.setEnableFlag(EnableFlagEnum.ENABLE);
            return labelStore
                    .getByName(
                            label.getTenantId(),
                            label.getLabelName(),
                            label.getEntityTypeFlag().getIndex())
                    .flatMap(existing -> Mono.<LabelBO>error(new DuplicateException("Label has been duplicated")))
                    .switchIfEmpty(Mono.defer(() -> labelStore.insert(label)))
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Label has been duplicated"));
        });
    }

    @Override
    public Mono<LabelBO> update(LabelBO label) {
        return Mono.defer(() -> {
            validate(label, true);
            return labelStore
                    .get(label.getTenantId(), label.getId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Label does not exist")))
                    .flatMap(current -> labelStore
                            .getByName(
                                    label.getTenantId(),
                                    label.getLabelName(),
                                    label.getEntityTypeFlag().getIndex())
                            .filter(existing -> !existing.getId().equals(label.getId()))
                            .flatMap(existing ->
                                    Mono.<LabelBO>error(new DuplicateException("Label has been duplicated")))
                            .switchIfEmpty(Mono.defer(() -> {
                                if (label.getLabelCode() == null
                                        || label.getLabelCode().isBlank()) label.setLabelCode(current.getLabelCode());
                                if (label.getLabelColor() == null
                                        || label.getLabelColor().isBlank())
                                    label.setLabelColor(current.getLabelColor());
                                if (label.getEnableFlag() == null) label.setEnableFlag(current.getEnableFlag());
                                return labelStore.update(label);
                            })))
                    .switchIfEmpty(Mono.error(new RequestException("Label update failed")))
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Label has been duplicated"));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (tenantId == null || id == null)
            return Mono.error(new RequestException("Tenant ID and label ID are required"));
        return labelStore
                .get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Label does not exist")))
                .then(labelStore.hasActiveBindings(tenantId, id))
                .flatMap(bound -> bound
                        ? Mono.error(new AssociatedException("The label has been bound by another entity"))
                        : labelStore.delete(tenantId, id, operatorId, operatorName))
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new RequestException("Label was already deleted")));
    }

    @Override
    public Mono<LabelBO> getById(Long tenantId, Long id) {
        if (tenantId == null || id == null)
            return Mono.error(new RequestException("Tenant ID and label ID are required"));
        return labelStore.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Label does not exist")));
    }

    @Override
    public Mono<OffsetPage<LabelBO>> list(LabelFilter filter) {
        return labelStore.list(filter);
    }

    private void validate(LabelBO label, boolean update) {
        if (label == null
                || label.getTenantId() == null
                || label.getTenantId() <= 0
                || label.getLabelName() == null
                || label.getLabelName().isBlank()
                || label.getEntityTypeFlag() == null
                || !SUPPORTED.contains(label.getEntityTypeFlag())
                || update && (label.getId() == null || label.getId() <= 0)) {
            throw new RequestException("Tenant ID, label name and supported entity type are required");
        }
        label.setLabelName(label.getLabelName().trim());
    }
}
