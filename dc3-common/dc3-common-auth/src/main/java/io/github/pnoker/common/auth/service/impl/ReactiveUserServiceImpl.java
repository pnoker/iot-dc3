package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.repository.ReactiveUserStore;
import io.github.pnoker.common.auth.repository.UserFilter;
import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default non-blocking tenant-scoped user read service. */
@Service
@RequiredArgsConstructor
public class ReactiveUserServiceImpl implements ReactiveUserService {
    private final ReactiveUserStore userStore;
    private final UserBuilder userBuilder;

    @Override public Mono<UserBO> getById(Long tenantId, Long id) { return userStore.getById(tenantId, id).map(userBuilder::buildBOByDO).switchIfEmpty(Mono.error(new NotFoundException("User"))); }
    @Override public Mono<UserBO> getByUserName(Long tenantId, String userName) { if (userName == null || userName.isBlank()) return Mono.error(new RequestException("User name is required")); return userStore.getByUserName(tenantId, userName).map(userBuilder::buildBOByDO).switchIfEmpty(Mono.error(new NotFoundException("User"))); }
    @Override public Mono<UserBO> getByPrincipalId(Long tenantId, Long principalId) { return userStore.getByPrincipalId(tenantId, principalId).map(userBuilder::buildBOByDO).switchIfEmpty(Mono.error(new NotFoundException("User"))); }
    @Override public Mono<OffsetPage<UserBO>> list(UserFilter filter) { return userStore.list(filter).map(page -> OffsetPage.of(page.items().stream().map(userBuilder::buildBOByDO).toList(), page.offset(), page.limit(), page.total())); }
}
