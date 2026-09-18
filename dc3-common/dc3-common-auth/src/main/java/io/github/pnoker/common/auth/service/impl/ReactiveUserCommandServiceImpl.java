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

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.model.UserDO;
import io.github.pnoker.common.auth.service.ReactiveUserCommandService;
import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

/** Atomic non-blocking user lifecycle implementation. */
@Service
@RequiredArgsConstructor
public class ReactiveUserCommandServiceImpl implements ReactiveUserCommandService {

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final R2dbcDialect dialect;
    private final ReactiveUserService reactiveUserService;
    private final UserBuilder userBuilder;

    @Override
    public Mono<UserBO> add(Long tenantId, UserBO user, Long operatorId, String operatorName) {
        if (tenantId == null || tenantId <= 0 || user == null || blank(user.getUserName())) {
            return Mono.error(new RequestException("User name is required"));
        }
        UserDO row = userBuilder.buildDOByBO(user);
        long principalId = id();
        long userId = id();
        LocalDateTime now = now();
        String principalName = user.getUserName().trim();
        String displayName = user.getNickName() == null ? "" : user.getNickName();
        Mono<Void> principal = databaseClient
                .sql(
                        "INSERT INTO dc3_auth.dc3_principal (id,principal_type,principal_name,display_name,source_type,enable_flag,locked_flag,last_login_time,principal_ext,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted)"
                                + " VALUES (:id,'USER',:principal_name,:display_name,'LOCAL',:enable_flag,0,NULL,"
                                + dialect.jsonWriteExpression(":principal_ext")
                                + ",:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", principalId)
                .bind("principal_name", principalName)
                .bind("display_name", displayName)
                .bind("enable_flag", flag(user.getEnableFlag()))
                .bind("principal_ext", "{}")
                .bind("remark", text(user.getRemark()))
                .bind("creator_id", value(operatorId))
                .bind("creator_name", text(operatorName))
                .bind("create_time", now)
                .bind("operator_id", value(operatorId))
                .bind("operator_name", text(operatorName))
                .bind("operate_time", now)
                .fetch()
                .rowsUpdated()
                .flatMap(rows ->
                        rows == 1 ? Mono.empty() : Mono.error(new IllegalStateException("principal insert failed")));
        Mono<Void> account = databaseClient
                .sql(
                        "INSERT INTO dc3_auth.dc3_user (id,principal_id,user_name,nick_name,phone,email,social_ext,identity_ext,enable_flag,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted)"
                                + " VALUES (:id,:principal_id,:user_name,:nick_name,:phone,:email,"
                                + dialect.jsonWriteExpression(":social_ext") + ","
                                + dialect.jsonWriteExpression(":identity_ext")
                                + ",:enable_flag,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", userId)
                .bind("principal_id", principalId)
                .bind("user_name", principalName)
                .bind("nick_name", text(user.getNickName()))
                .bind("phone", text(user.getPhone()))
                .bind("email", text(user.getEmail()))
                .bind("social_ext", json(row.getSocialExt()))
                .bind("identity_ext", json(row.getIdentityExt()))
                .bind("enable_flag", flag(user.getEnableFlag()))
                .bind("remark", text(user.getRemark()))
                .bind("creator_id", value(operatorId))
                .bind("creator_name", text(operatorName))
                .bind("create_time", now)
                .bind("operator_id", value(operatorId))
                .bind("operator_name", text(operatorName))
                .bind("operate_time", now)
                .fetch()
                .rowsUpdated()
                .flatMap(
                        rows -> rows == 1 ? Mono.empty() : Mono.error(new IllegalStateException("user insert failed")));
        Mono<Void> membership = databaseClient
                .sql(
                        "INSERT INTO dc3_auth.dc3_tenant_membership (id,tenant_id,principal_id,principal_type,membership_status,joined_time,membership_ext,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted)"
                                + " VALUES (:id,:tenant_id,:principal_id,'USER','ACTIVE',:joined_time,"
                                + dialect.jsonWriteExpression(":membership_ext")
                                + ",:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", id())
                .bind("tenant_id", tenantId)
                .bind("principal_id", principalId)
                .bind("joined_time", now)
                .bind("membership_ext", "{}")
                .bind("remark", text(user.getRemark()))
                .bind("creator_id", value(operatorId))
                .bind("creator_name", text(operatorName))
                .bind("create_time", now)
                .bind("operator_id", value(operatorId))
                .bind("operator_name", text(operatorName))
                .bind("operate_time", now)
                .fetch()
                .rowsUpdated()
                .flatMap(rows ->
                        rows == 1 ? Mono.empty() : Mono.error(new IllegalStateException("membership insert failed")));
        return transactionalOperator
                .transactional(principal.then(account).then(membership))
                .then(reactiveUserService.getById(tenantId, userId))
                .onErrorMap(
                        DataIntegrityViolationException.class,
                        error -> new DuplicateException("User has been duplicated"));
    }

    @Override
    public Mono<UserBO> update(Long tenantId, UserBO user, Long operatorId, String operatorName) {
        if (tenantId == null || tenantId <= 0 || user == null || user.getId() == null || user.getId() <= 0) {
            return Mono.error(new RequestException("User update is invalid"));
        }
        UserDO row = userBuilder.buildDOByBO(user);
        return reactiveUserService
                .getById(tenantId, user.getId())
                .flatMap(current -> {
                    String principalName = blank(user.getUserName())
                            ? current.getUserName()
                            : user.getUserName().trim();
                    String displayName = user.getNickName() == null ? current.getNickName() : user.getNickName();
                    EnableFlagEnum targetFlag =
                            user.getEnableFlag() == null ? current.getEnableFlag() : user.getEnableFlag();
                    Mono<Void> principal = databaseClient
                            .sql(
                                    "UPDATE dc3_auth.dc3_principal SET principal_name=:principal_name,display_name=:display_name,enable_flag=:enable_flag,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE id=:principal_id AND deleted=0")
                            .bind("principal_name", text(principalName))
                            .bind("display_name", text(displayName))
                            .bind("enable_flag", flag(targetFlag))
                            .bind("operator_id", value(operatorId))
                            .bind("operator_name", text(operatorName))
                            .bind("operate_time", now())
                            .bind("principal_id", current.getPrincipalId())
                            .fetch()
                            .rowsUpdated()
                            .flatMap(rows -> rows == 1 ? Mono.empty() : Mono.error(new NotFoundException("Principal")));
                    Mono<Void> account = databaseClient
                            .sql(
                                    "UPDATE dc3_auth.dc3_user SET user_name=:user_name,nick_name=:nick_name,phone=:phone,email=:email,social_ext="
                                            + dialect.jsonWriteExpression(":social_ext") + ",identity_ext="
                                            + dialect.jsonWriteExpression(":identity_ext")
                                            + ",enable_flag=:enable_flag,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE id=:id AND principal_id=:principal_id AND deleted=0")
                            .bind("user_name", text(principalName))
                            .bind("nick_name", text(displayName))
                            .bind("phone", text(user.getPhone()))
                            .bind("email", text(user.getEmail()))
                            .bind("social_ext", json(row.getSocialExt()))
                            .bind("identity_ext", json(row.getIdentityExt()))
                            .bind("enable_flag", flag(targetFlag))
                            .bind("remark", text(user.getRemark()))
                            .bind("operator_id", value(operatorId))
                            .bind("operator_name", text(operatorName))
                            .bind("operate_time", now())
                            .bind("id", user.getId())
                            .bind("principal_id", current.getPrincipalId())
                            .fetch()
                            .rowsUpdated()
                            .flatMap(rows -> rows == 1 ? Mono.empty() : Mono.error(new NotFoundException("User")));
                    return transactionalOperator
                            .transactional(principal.then(account))
                            .then(reactiveUserService.getById(tenantId, user.getId()));
                })
                .onErrorMap(
                        DataIntegrityViolationException.class,
                        error -> new DuplicateException("User has been duplicated"));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long userId, Long operatorId, String operatorName) {
        return reactiveUserService
                .getById(tenantId, userId)
                .flatMap(user -> transactionalOperator.transactional(databaseClient
                        .sql(
                                "UPDATE dc3_auth.dc3_tenant_membership SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND principal_id=:principal_id AND deleted=0")
                        .bind("tenant_id", tenantId)
                        .bind("principal_id", user.getPrincipalId())
                        .bind("operator_id", value(operatorId))
                        .bind("operator_name", text(operatorName))
                        .bind("operate_time", now())
                        .fetch()
                        .rowsUpdated()
                        .flatMap(rows ->
                                rows == 1 ? Mono.empty() : Mono.error(new NotFoundException("Tenant membership")))
                        .then(databaseClient
                                .sql(
                                        "UPDATE dc3_auth.dc3_local_credential SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE principal_id=:principal_id AND deleted=0 AND NOT EXISTS (SELECT 1 FROM dc3_auth.dc3_tenant_membership m WHERE m.principal_id=:principal_id AND m.membership_status='ACTIVE' AND m.deleted=0)")
                                .bind("principal_id", user.getPrincipalId())
                                .bind("operator_id", value(operatorId))
                                .bind("operator_name", text(operatorName))
                                .bind("operate_time", now())
                                .fetch()
                                .rowsUpdated())
                        .then(databaseClient
                                .sql(
                                        "UPDATE dc3_auth.dc3_user SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE id=:id AND deleted=0 AND NOT EXISTS (SELECT 1 FROM dc3_auth.dc3_tenant_membership m WHERE m.principal_id=:principal_id AND m.membership_status='ACTIVE' AND m.deleted=0)")
                                .bind("id", userId)
                                .bind("principal_id", user.getPrincipalId())
                                .bind("operator_id", value(operatorId))
                                .bind("operator_name", text(operatorName))
                                .bind("operate_time", now())
                                .fetch()
                                .rowsUpdated())
                        .then(databaseClient
                                .sql(
                                        "UPDATE dc3_auth.dc3_principal SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE id=:principal_id AND deleted=0 AND NOT EXISTS (SELECT 1 FROM dc3_auth.dc3_tenant_membership m WHERE m.principal_id=dc3_principal.id AND m.membership_status='ACTIVE' AND m.deleted=0)")
                                .bind("principal_id", user.getPrincipalId())
                                .bind("operator_id", value(operatorId))
                                .bind("operator_name", text(operatorName))
                                .bind("operate_time", now())
                                .fetch()
                                .rowsUpdated())
                        .thenReturn(Boolean.TRUE)));
    }

    private long id() {
        return UuidV7.nextLong();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private byte flag(EnableFlagEnum value) {
        return value == null ? EnableFlagEnum.ENABLE.getIndex() : value.getIndex();
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String json(Object value) {
        return value == null ? "{}" : JsonUtil.toJsonString(value);
    }
}
