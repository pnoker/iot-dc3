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

import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.CallTypeEnum;
import io.github.pnoker.common.enums.CommandTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ConflictException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.repository.CommandFilter;
import io.github.pnoker.common.manager.repository.ReactiveCommandParamStore;
import io.github.pnoker.common.manager.repository.ReactiveCommandStore;
import io.github.pnoker.common.manager.repository.ReactiveProfileStore;
import io.github.pnoker.common.manager.service.ReactiveCommandService;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReactiveCommandServiceImpl implements ReactiveCommandService {
    private final ReactiveCommandStore commandStore;
    private final ReactiveProfileStore profileStore;
    private final io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher metadataEventPublisher;
    private final ReactiveCommandParamStore commandParamStore;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<CommandBO> add(CommandBO value) {
        return Mono.defer(() -> {
            validate(value, false);
            value.setCommandCode(
                    value.getCommandCode() == null || value.getCommandCode().isBlank()
                            ? CodeUtil.getCode()
                            : value.getCommandCode().trim());
            return ensureProfile(value)
                    .then(commandStore.existsByNameOrCode(
                            value.getTenantId(),
                            value.getProfileId(),
                            value.getCommandName(),
                            value.getCommandCode(),
                            null))
                    .flatMap(duplicate -> duplicate
                            ? Mono.<CommandBO>error(new DuplicateException("Command has been duplicated"))
                            : commandStore.insert(normalize(value, false)))
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Command has been duplicated"))
                    .doOnSuccess(saved -> publish(saved, MetadataOperateTypeEnum.ADD));
        });
    }

    @Override
    public Mono<CommandBO> update(CommandBO value) {
        return Mono.defer(() -> {
            validate(value, true);
            return commandStore
                    .get(value.getTenantId(), value.getId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Command does not exist")))
                    .flatMap(current -> ensureProfile(value)
                            .then(commandStore.existsByNameOrCode(
                                    value.getTenantId(),
                                    value.getProfileId(),
                                    value.getCommandName(),
                                    current.getCommandCode(),
                                    value.getId()))
                            .flatMap(duplicate -> duplicate
                                    ? Mono.<CommandBO>error(new DuplicateException("Command has been duplicated"))
                                    : commandStore.update(normalize(value, true), value.getVersion())))
                    .switchIfEmpty(Mono.error(new ConflictException("Command version conflict")))
                    .doOnSuccess(saved -> publish(saved, MetadataOperateTypeEnum.UPDATE));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (tenantId == null || id == null)
            return Mono.error(new RequestException("Tenant ID and command ID are required"));
        Mono<Boolean> deletion = commandStore
                .get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Command does not exist")))
                .flatMap(command -> commandParamStore
                        .deleteByCommandId(tenantId, id, operatorId, operatorName)
                        .then(commandStore.delete(tenantId, id, expectedVersion, operatorId, operatorName)))
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new ConflictException("Command version conflict")));
        return transactionalOperator
                .transactional(deletion)
                .doOnSuccess(ignored -> metadataEventPublisher.publishEvent(new MetadataEvent(
                        this, tenantId, id, MetadataTypeEnum.COMMAND, MetadataOperateTypeEnum.DELETE)));
    }

    @Override
    public Mono<CommandBO> getById(Long tenantId, Long id) {
        if (tenantId == null || id == null)
            return Mono.error(new RequestException("Tenant ID and command ID are required"));
        return commandStore
                .get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Command does not exist")));
    }

    @Override
    public Flux<CommandBO> listByIds(Long tenantId, List<Long> ids) {
        return commandStore.listByIds(tenantId, ids);
    }

    @Override
    public Flux<CommandBO> listByProfileId(Long tenantId, Long profileId) {
        return commandStore.listByProfileId(tenantId, profileId);
    }

    @Override
    public Flux<CommandBO> listByDeviceId(Long tenantId, Long deviceId) {
        return commandStore.listByDeviceId(tenantId, deviceId);
    }

    @Override
    public Mono<OffsetPage<CommandBO>> list(CommandFilter filter) {
        return commandStore.list(filter);
    }

    private Mono<Void> ensureProfile(CommandBO value) {
        return profileStore
                .get(value.getTenantId(), value.getProfileId())
                .switchIfEmpty(Mono.error(new NotFoundException("Profile does not exist")))
                .then();
    }

    private void validate(CommandBO value, boolean update) {
        if (value == null
                || value.getTenantId() == null
                || value.getTenantId() <= 0
                || value.getProfileId() == null
                || value.getProfileId() <= 0
                || value.getCommandName() == null
                || value.getCommandName().isBlank()
                || value.getTimeout() == null
                || value.getTimeout() < 1)
            throw new RequestException("Tenant ID, profile ID, command name and timeout are required");
        if (update && (value.getId() == null || value.getVersion() == null || value.getVersion() < 0))
            throw new RequestException("Command ID and version are required for update");
        value.setCommandName(value.getCommandName().trim());
    }

    private CommandBO normalize(CommandBO source, boolean update) {
        source.setCommandTypeFlag(
                source.getCommandTypeFlag() == null ? CommandTypeEnum.CUSTOM : source.getCommandTypeFlag());
        source.setCallTypeFlag(source.getCallTypeFlag() == null ? CallTypeEnum.SYNC : source.getCallTypeFlag());
        source.setEnableFlag(source.getEnableFlag() == null ? EnableFlagEnum.ENABLE : source.getEnableFlag());
        source.setRemark(source.getRemark() == null ? "" : source.getRemark());
        if (!update && source.getVersion() == null) source.setVersion(0);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!update && source.getCreateTime() == null) source.setCreateTime(now);
        source.setOperateTime(now);
        return source;
    }

    private void publish(CommandBO value, MetadataOperateTypeEnum operation) {
        if (value != null)
            metadataEventPublisher.publishEvent(
                    new MetadataEvent(this, value.getTenantId(), value.getId(), MetadataTypeEnum.COMMAND, operation));
    }
}
