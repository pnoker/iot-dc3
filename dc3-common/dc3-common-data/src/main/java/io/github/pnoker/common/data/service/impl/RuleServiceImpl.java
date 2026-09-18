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
package io.github.pnoker.common.data.service.impl;

import io.github.pnoker.common.data.biz.alarm.RuleRegistry;
import io.github.pnoker.common.data.biz.alarm.WindowSpec;
import io.github.pnoker.common.data.biz.alarm.WindowSpecParser;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.builder.RuleBuilder;
import io.github.pnoker.common.data.entity.model.RuleDO;
import io.github.pnoker.common.data.entity.query.RuleQuery;
import io.github.pnoker.common.data.repository.ReactiveRuleStore;
import io.github.pnoker.common.data.service.RuleService;
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Reactive tenant-scoped alarm rule service. */
@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final RuleBuilder ruleBuilder;
    private final ReactiveRuleStore ruleStore;
    private final RuleRegistry ruleRegistry;

    @Override
    public Mono<RuleBO> add(RuleBO entityBO) {
        return Mono.defer(() -> {
            validate(entityBO);
            RuleDO entityDO = ruleBuilder.buildDOByBO(entityBO);
            return ruleStore
                    .existsActiveCode(value(entityDO.getTenantId()), entityDO.getRuleCode(), null)
                    .flatMap(duplicate -> duplicate
                            ? Mono.<RuleBO>error(new DuplicateException("Alarm rule has been duplicated"))
                            : ruleStore
                                    .insert(entityDO)
                                    .map(ruleBuilder::buildBOByDO)
                                    .switchIfEmpty(Mono.error(new AddException("Failed to create alarm rule"))))
                    .doOnSuccess(created -> {
                        if (created != null) ruleRegistry.invalidateTenant(created.getTenantId());
                    })
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Alarm rule has been duplicated"));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id) {
        return ruleStore
                .get(value(tenantId), value(id))
                .switchIfEmpty(Mono.error(new NotFoundException("Alarm rule does not exist")))
                .flatMap(existing -> ruleStore
                        .hasChildren(value(tenantId), value(id))
                        .flatMap(hasChildren -> hasChildren
                                ? Mono.<Boolean>error(new AssociatedException(
                                        "Failed to remove alarm rule: some sub alarm rules exists in the alarm rule"))
                                : ruleStore.softDelete(value(tenantId), value(id))))
                .flatMap(deleted ->
                        deleted ? Mono.just(true) : Mono.error(new DeleteException("Failed to remove alarm rule")))
                .doOnSuccess(deleted -> ruleRegistry.invalidateTenant(value(tenantId)));
    }

    @Override
    public Mono<RuleBO> update(RuleBO entityBO) {
        return Mono.defer(() -> {
            validate(entityBO);
            if (!valid(entityBO.getTenantId()) || !valid(entityBO.getId())) {
                return Mono.error(new UpdateException("Rule tenant and id are required"));
            }
            RuleDO entityDO = ruleBuilder.buildDOByBO(entityBO);
            entityDO.setTenantId(entityBO.getTenantId());
            entityDO.setId(entityBO.getId());
            return ruleStore
                    .get(value(entityBO.getTenantId()), value(entityBO.getId()))
                    .switchIfEmpty(Mono.error(new NotFoundException("Alarm rule does not exist")))
                    .then(ruleStore.existsActiveCode(
                            value(entityBO.getTenantId()), entityDO.getRuleCode(), entityBO.getId()))
                    .flatMap(duplicate -> duplicate
                            ? Mono.<RuleBO>error(new DuplicateException("Alarm rule has been duplicated"))
                            : ruleStore
                                    .update(entityDO)
                                    .map(ruleBuilder::buildBOByDO)
                                    .switchIfEmpty(Mono.error(new UpdateException("Failed to update alarm rule"))))
                    .doOnSuccess(updated -> {
                        if (updated != null) ruleRegistry.invalidateTenant(updated.getTenantId());
                    })
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Alarm rule has been duplicated"));
        });
    }

    @Override
    public Mono<RuleBO> getById(Long tenantId, Long id) {
        return ruleStore
                .get(value(tenantId), value(id))
                .switchIfEmpty(Mono.error(new NotFoundException("Alarm rule does not exist")))
                .map(ruleBuilder::buildBOByDO);
    }

    @Override
    public Mono<OffsetPage<RuleBO>> list(Long tenantId, RuleQuery entityQuery) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            RuleQuery query = entityQuery == null ? new RuleQuery() : entityQuery;
            PageRequest page = new PageRequest(query.getOffset(), query.getLimit(), query.getSort());
            return ruleStore
                    .list(
                            tenantId,
                            query.getRuleName(),
                            query.getRuleCode(),
                            query.getEntityId(),
                            query.getAlarmTargetTypeFlag(),
                            query.getEnableFlag(),
                            page)
                    .map(result -> OffsetPage.of(
                            result.items().stream()
                                    .map(ruleBuilder::buildBOByDO)
                                    .toList(),
                            result.offset(),
                            result.limit(),
                            result.total()));
        });
    }

    private void validate(RuleBO entityBO) {
        if (entityBO == null) throw new IllegalArgumentException("rule is required");
        if (!valid(entityBO.getTenantId())) throw new IllegalArgumentException("tenantId is required");
        validateWindowMode(entityBO);
    }

    private void validateWindowMode(RuleBO entityBO) {
        if (entityBO.getRuleExt() == null || entityBO.getRuleExt().getContent() == null) return;
        RuleExt.Window window = entityBO.getRuleExt().getContent().getWindow();
        if (window == null) return;
        WindowSpec spec = WindowSpecParser.parse(window);
        if (!spec.valid()) throw new UnSupportException("Invalid rule window: " + spec.reason());
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private boolean valid(Long value) {
        return value != null && value > 0;
    }

    private void requireTenant(Long tenantId) {
        if (!valid(tenantId)) throw new IllegalArgumentException("tenantId is required");
    }
}
