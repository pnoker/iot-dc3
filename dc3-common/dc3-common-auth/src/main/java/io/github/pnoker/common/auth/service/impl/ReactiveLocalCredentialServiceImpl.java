package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.builder.LocalCredentialBuilder;
import io.github.pnoker.common.auth.repository.LocalCredentialFilter;
import io.github.pnoker.common.auth.repository.ReactiveLocalCredentialStore;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialService;
import io.github.pnoker.common.auth.support.ReactiveAuthScheduler;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default non-blocking local credential query implementation. */
@Service
@RequiredArgsConstructor
public class ReactiveLocalCredentialServiceImpl implements ReactiveLocalCredentialService {

    private final ReactiveLocalCredentialStore store;
    private final LocalCredentialBuilder builder;

    @Override
    public Mono<LocalCredentialBO> getById(Long tenantId, Long id) {
        return store.getById(tenantId, id).map(builder::buildBOByDO);
    }

    @Override
    public Mono<LocalCredentialBO> getByLoginName(Long tenantId, String loginName) {
        if (loginName == null || loginName.isBlank()) return Mono.error(new EmptyException("The login name is empty"));
        return store.getByLoginName(tenantId, normalize(loginName)).map(builder::buildBOByDO);
    }

    @Override
    public Mono<io.github.pnoker.db.r2dbc.core.page.OffsetPage<LocalCredentialBO>> list(LocalCredentialFilter filter) {
        return store.list(filter).map(page -> io.github.pnoker.db.r2dbc.core.page.OffsetPage.of(
                page.items().stream().map(builder::buildBOByDO).toList(), page.offset(), page.limit(), page.total()));
    }

    @Override
    public Mono<Boolean> isLoginNameAvailable(Long tenantId, String loginName) {
        if (loginName == null || loginName.isBlank()) return Mono.just(false);
        return store.existsByLoginName(tenantId, normalize(loginName)).map(exists -> !exists);
    }

    @Override
    public Mono<Boolean> verifyPassword(LocalCredentialBO credential, String rawPassword) {
        if (credential == null || rawPassword == null || rawPassword.isBlank()) return Mono.just(false);
        if (credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(java.time.LocalDateTime.now(java.time.Clock.systemUTC()))) {
            return Mono.just(false);
        }
        return Mono.fromCallable(() -> PasswordUtil.verify(rawPassword, credential.getPasswordHash()))
                .subscribeOn(ReactiveAuthScheduler.CRYPTO);
    }

    private String normalize(String value) { return value.trim().toLowerCase(java.util.Locale.ROOT); }
}
