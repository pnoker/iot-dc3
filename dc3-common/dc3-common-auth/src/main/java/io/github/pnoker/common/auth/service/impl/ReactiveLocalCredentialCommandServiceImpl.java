package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.builder.LocalCredentialBuilder;
import io.github.pnoker.common.auth.entity.model.LocalCredentialDO;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialCommandService;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialService;
import io.github.pnoker.common.auth.support.ReactiveAuthScheduler;
import io.github.pnoker.common.enums.CredentialTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.RequirePasswordChangeFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.PasswordUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Atomic, tenant-scoped local credential commands. */
@Service
@RequiredArgsConstructor
public class ReactiveLocalCredentialCommandServiceImpl implements ReactiveLocalCredentialCommandService {

    private static final String TABLE = "dc3_auth.dc3_local_credential";
    private static final String MEMBERSHIP = "dc3_auth.dc3_tenant_membership";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final R2dbcDialect dialect;
    private final ReactiveLocalCredentialService reactiveLocalCredentialService;
    private final LocalCredentialBuilder builder;

    @Value("${dc3.auth.password.expire-days:0}")
    private long passwordExpireDays;

    @Override
    public Mono<LocalCredentialBO> add(Long tenantId, LocalCredentialBO credential, Long operatorId, String operatorName) {
        if (!validTenant(tenantId) || credential == null || credential.getPrincipalId() == null
                || credential.getPrincipalId() <= 0 || blank(credential.getLoginName()) || blank(credential.getRawPassword())) {
            return Mono.error(new EmptyException("Principal, login name and password are required"));
        }
        return requireMember(tenantId, credential.getPrincipalId())
                .then(prepareForCreate(credential))
                .flatMap(prepared -> {
                    long id = UuidV7.nextLong();
                    LocalDateTime now = now();
                    LocalCredentialDO row = builder.buildDOByBO(prepared);
                    DatabaseClient.GenericExecuteSpec insert = databaseClient.sql("INSERT INTO " + TABLE
                                    + " (id,principal_id,login_name,login_name_normalized,credential_type,password_hash,password_algorithm,password_params,password_updated_time,password_expire_time,failed_attempts,locked_until,require_password_change,enable_flag,credential_ext,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted)"
                                    + " VALUES (:id,:principal_id,:login_name,:login_name_normalized,:credential_type,:password_hash,:password_algorithm," + dialect.jsonWriteExpression(":password_params") + ",:password_updated_time,:password_expire_time,:failed_attempts,NULL,:require_password_change,:enable_flag," + dialect.jsonWriteExpression(":credential_ext") + ",:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                            .bind("id", id).bind("principal_id", prepared.getPrincipalId()).bind("login_name", text(prepared.getLoginName()))
                            .bind("login_name_normalized", normalize(prepared.getLoginName())).bind("credential_type", CredentialTypeEnum.PASSWORD.getValue())
                            .bind("password_hash", prepared.getPasswordHash()).bind("password_algorithm", prepared.getPasswordAlgorithm().getValue())
                            .bind("password_params", json(row.getPasswordParams())).bind("password_updated_time", now)
                            .bind("failed_attempts", 0).bind("require_password_change", flag(prepared.getRequirePasswordChange()))
                            .bind("enable_flag", flag(prepared.getEnableFlag())).bind("credential_ext", json(row.getCredentialExt()))
                            .bind("remark", text(prepared.getRemark())).bind("creator_id", value(operatorId)).bind("creator_name", text(operatorName))
                            .bind("create_time", now).bind("operator_id", value(operatorId)).bind("operator_name", text(operatorName)).bind("operate_time", now);
                    insert = bindNullable(insert, "password_expire_time", prepared.getPasswordExpireTime(), LocalDateTime.class);
                    return transactionalOperator.transactional(insert.fetch().rowsUpdated()
                                    .flatMap(rows -> rows == 1 ? Mono.empty() : Mono.error(new IllegalStateException("credential insert failed"))))
                            .then(reactiveLocalCredentialService.getById(tenantId, id));
                })
                .onErrorMap(DataIntegrityViolationException.class, error -> new DuplicateException("Local credential has been duplicated"));
    }

    @Override
    public Mono<LocalCredentialBO> update(Long tenantId, LocalCredentialBO credential, Long operatorId, String operatorName) {
        if (!validTenant(tenantId) || credential == null || credential.getId() == null || credential.getId() <= 0) {
            return Mono.error(new EmptyException("Credential id is required"));
        }
        return reactiveLocalCredentialService.getById(tenantId, credential.getId())
                .switchIfEmpty(Mono.error(new NotFoundException("Local credential does not exist")))
                .flatMap(current -> prepareForUpdate(credential, current))
                .flatMap(prepared -> updateRow(tenantId, prepared, operatorId, operatorName)
                        .then(reactiveLocalCredentialService.getById(tenantId, prepared.getId())))
                .onErrorMap(DataIntegrityViolationException.class, error -> new DuplicateException("Local credential has been duplicated"));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (!validTenant(tenantId) || id == null || id <= 0) return Mono.error(new EmptyException("Credential id is required"));
        return databaseClient.sql("UPDATE " + TABLE + " c SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                        + " WHERE c.id=:id AND c.deleted=0 AND EXISTS (SELECT 1 FROM " + MEMBERSHIP + " m WHERE m.tenant_id=:tenant_id"
                        + " AND m.principal_id=c.principal_id AND m.membership_status='ACTIVE' AND m.deleted=0)")
                .bind("id", id).bind("tenant_id", tenantId).bind("operator_id", value(operatorId)).bind("operator_name", text(operatorName)).bind("operate_time", now())
                .fetch().rowsUpdated().flatMap(rows -> rows == 1 ? Mono.just(Boolean.TRUE) : Mono.error(new NotFoundException("Local credential does not exist")))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<LocalCredentialBO> resetPassword(Long tenantId, Long id, String rawPassword, Long operatorId, String operatorName) {
        if (blank(rawPassword)) return Mono.error(new EmptyException("The password is empty"));
        return reactiveLocalCredentialService.getById(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Local credential does not exist")))
                .flatMap(current -> encode(rawPassword).map(hash -> {
                    current.setRawPassword(rawPassword);
                    current.setPasswordHash(hash);
                    current.setPasswordAlgorithm(PasswordUtil.algorithmOfHash(hash));
                    current.setPasswordUpdatedTime(now());
                    current.setPasswordExpireTime(expireAt());
                    current.setRequirePasswordChange(RequirePasswordChangeFlagEnum.REQUIRED);
                    current.setFailedAttempts(0);
                    current.setLockedUntil(null);
                    return current;
                }))
                .flatMap(prepared -> updateRow(tenantId, prepared, operatorId, operatorName)
                        .then(reactiveLocalCredentialService.getById(tenantId, prepared.getId())));
    }

    @Override
    public Mono<LocalCredentialBO> changePassword(Long tenantId, String loginName, String currentPassword,
                                                   String newPassword, Long operatorId, String operatorName) {
        if (blank(newPassword)) return Mono.error(new EmptyException("The new password is empty"));
        return reactiveLocalCredentialService.getByLoginName(tenantId, loginName)
                .switchIfEmpty(Mono.error(new UnAuthorizedException("The current password does not match")))
                .flatMap(current -> reactiveLocalCredentialService.verifyPassword(current, currentPassword)
                        .flatMap(valid -> valid ? encode(newPassword).map(hash -> {
                            current.setRawPassword(newPassword);
                            current.setPasswordHash(hash);
                            current.setPasswordAlgorithm(PasswordUtil.algorithmOfHash(hash));
                            current.setPasswordUpdatedTime(now());
                            current.setPasswordExpireTime(expireAt());
                            current.setRequirePasswordChange(RequirePasswordChangeFlagEnum.NOT_REQUIRED);
                            current.setFailedAttempts(0);
                            current.setLockedUntil(null);
                            return current;
                        }) : Mono.error(new UnAuthorizedException("The current password does not match"))))
                .flatMap(prepared -> updateRow(tenantId, prepared, operatorId, operatorName)
                        .then(reactiveLocalCredentialService.getById(tenantId, prepared.getId())));
    }

    @Override
    public Mono<Void> recordSuccessfulLogin(Long tenantId, Long id) {
        return databaseClient.sql("UPDATE " + TABLE + " c SET failed_attempts=0,locked_until=NULL"
                        + " WHERE c.id=:id AND c.deleted=0 AND EXISTS (SELECT 1 FROM " + MEMBERSHIP + " m WHERE m.tenant_id=:tenant_id"
                        + " AND m.principal_id=c.principal_id AND m.membership_status='ACTIVE' AND m.deleted=0)")
                .bind("id", id).bind("tenant_id", tenantId).fetch().rowsUpdated().then();
    }

    @Override
    public Mono<Void> recordFailedLogin(Long tenantId, Long id) {
        return databaseClient.sql("UPDATE " + TABLE + " c SET failed_attempts=c.failed_attempts+1,locked_until=CASE WHEN c.failed_attempts+1>=:max_attempts THEN CURRENT_TIMESTAMP + (:lock_minutes * INTERVAL '1 minute') ELSE c.locked_until END"
                        + " WHERE c.id=:id AND c.deleted=0 AND EXISTS (SELECT 1 FROM " + MEMBERSHIP + " m WHERE m.tenant_id=:tenant_id"
                        + " AND m.principal_id=c.principal_id AND m.membership_status='ACTIVE' AND m.deleted=0)")
                .bind("max_attempts", MAX_FAILED_ATTEMPTS).bind("lock_minutes", LOCK_MINUTES).bind("id", id).bind("tenant_id", tenantId)
                .fetch().rowsUpdated().then();
    }

    private Mono<Void> requireMember(Long tenantId, Long principalId) {
        return databaseClient.sql("SELECT 1 FROM " + MEMBERSHIP + " WHERE tenant_id=:tenant_id AND principal_id=:principal_id"
                        + " AND membership_status='ACTIVE' AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId).bind("principal_id", principalId).map((row, metadata) -> Boolean.TRUE).one()
                .switchIfEmpty(Mono.error(new NotFoundException("Tenant membership does not exist"))).then();
    }

    private Mono<Void> updateRow(Long tenantId, LocalCredentialBO credential, Long operatorId, String operatorName) {
        LocalCredentialDO row = builder.buildDOByBO(credential);
        DatabaseClient.GenericExecuteSpec update = databaseClient.sql("UPDATE " + TABLE + " c SET login_name=:login_name,login_name_normalized=:login_name_normalized,password_hash=:password_hash,password_algorithm=:password_algorithm,password_params=" + dialect.jsonWriteExpression(":password_params") + ",password_updated_time=:password_updated_time,password_expire_time=:password_expire_time,failed_attempts=:failed_attempts,locked_until=:locked_until,require_password_change=:require_password_change,enable_flag=:enable_flag,credential_ext=" + dialect.jsonWriteExpression(":credential_ext") + ",remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE c.id=:id AND c.deleted=0 AND EXISTS (SELECT 1 FROM " + MEMBERSHIP + " m WHERE m.tenant_id=:tenant_id AND m.principal_id=c.principal_id AND m.membership_status='ACTIVE' AND m.deleted=0)")
                .bind("login_name", text(credential.getLoginName())).bind("login_name_normalized", normalize(credential.getLoginName()))
                .bind("password_hash", credential.getPasswordHash()).bind("password_algorithm", credential.getPasswordAlgorithm().getValue())
                .bind("password_params", json(row.getPasswordParams())).bind("password_updated_time", credential.getPasswordUpdatedTime())
                .bind("failed_attempts", credential.getFailedAttempts() == null ? 0 : credential.getFailedAttempts())
                .bind("require_password_change", flag(credential.getRequirePasswordChange())).bind("enable_flag", flag(credential.getEnableFlag()))
                .bind("credential_ext", json(row.getCredentialExt())).bind("remark", text(credential.getRemark()))
                .bind("operator_id", value(operatorId)).bind("operator_name", text(operatorName)).bind("operate_time", now())
                .bind("id", credential.getId()).bind("tenant_id", tenantId);
        update = bindNullable(update, "password_expire_time", credential.getPasswordExpireTime(), LocalDateTime.class);
        update = bindNullable(update, "locked_until", credential.getLockedUntil(), LocalDateTime.class);
        return update.fetch().rowsUpdated().flatMap(rows -> rows == 1 ? Mono.empty() : Mono.error(new UpdateException("The local credential update failed")));
    }

    private Mono<LocalCredentialBO> prepareForCreate(LocalCredentialBO credential) {
        return encode(credential.getRawPassword()).map(hash -> {
            credential.setCredentialType(CredentialTypeEnum.PASSWORD);
            credential.setLoginName(credential.getLoginName().trim());
            credential.setLoginNameNormalized(normalize(credential.getLoginName()));
            credential.setPasswordHash(hash);
            credential.setPasswordAlgorithm(PasswordUtil.algorithmOfHash(hash));
            credential.setPasswordUpdatedTime(now());
            credential.setPasswordExpireTime(expireAt());
            credential.setFailedAttempts(0);
            credential.setRequirePasswordChange(credential.getRequirePasswordChange() == null ? RequirePasswordChangeFlagEnum.REQUIRED : credential.getRequirePasswordChange());
            credential.setEnableFlag(credential.getEnableFlag() == null ? EnableFlagEnum.ENABLE : credential.getEnableFlag());
            return credential;
        });
    }

    private Mono<LocalCredentialBO> prepareForUpdate(LocalCredentialBO credential, LocalCredentialBO current) {
        credential.setId(current.getId());
        credential.setPrincipalId(current.getPrincipalId());
        credential.setCredentialType(CredentialTypeEnum.PASSWORD);
        credential.setLoginName(blank(credential.getLoginName()) ? current.getLoginName() : credential.getLoginName().trim());
        credential.setLoginNameNormalized(normalize(credential.getLoginName()));
        credential.setPasswordParams(current.getPasswordParams());
        credential.setCredentialExt(credential.getCredentialExt() == null ? current.getCredentialExt() : credential.getCredentialExt());
        credential.setPasswordHash(current.getPasswordHash());
        credential.setPasswordAlgorithm(current.getPasswordAlgorithm());
        credential.setPasswordUpdatedTime(current.getPasswordUpdatedTime());
        credential.setPasswordExpireTime(current.getPasswordExpireTime());
        credential.setFailedAttempts(credential.getFailedAttempts() == null ? current.getFailedAttempts() : credential.getFailedAttempts());
        credential.setLockedUntil(current.getLockedUntil());
        credential.setRequirePasswordChange(credential.getRequirePasswordChange() == null ? current.getRequirePasswordChange() : credential.getRequirePasswordChange());
        credential.setEnableFlag(credential.getEnableFlag() == null ? current.getEnableFlag() : credential.getEnableFlag());
        if (!blank(credential.getRawPassword())) {
            return encode(credential.getRawPassword()).map(hash -> {
                credential.setPasswordHash(hash);
                credential.setPasswordAlgorithm(PasswordUtil.algorithmOfHash(hash));
                credential.setPasswordUpdatedTime(now());
                credential.setPasswordExpireTime(expireAt());
                return credential;
            });
        }
        return Mono.just(credential);
    }

    private Mono<String> encode(String rawPassword) {
        return Mono.fromCallable(() -> PasswordUtil.encode(rawPassword)).subscribeOn(ReactiveAuthScheduler.CRYPTO);
    }

    private LocalDateTime expireAt() { return passwordExpireDays > 0 ? now().plusDays(passwordExpireDays) : null; }
    private LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private boolean validTenant(Long tenantId) { return tenantId != null && tenantId > 0; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String normalize(String value) { return value.trim().toLowerCase(java.util.Locale.ROOT); }
    private String text(String value) { return value == null ? "" : value; }
    private long value(Long value) { return value == null ? 0L : value; }
    private byte flag(EnableFlagEnum value) { return value == null ? EnableFlagEnum.ENABLE.getIndex() : value.getIndex(); }
    private byte flag(RequirePasswordChangeFlagEnum value) { return value == null ? RequirePasswordChangeFlagEnum.REQUIRED.getIndex() : value.getIndex(); }
    private String json(Object value) { return value == null ? "{}" : JsonUtil.toJsonString(value); }
    private <T> DatabaseClient.GenericExecuteSpec bindNullable(DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }
}
