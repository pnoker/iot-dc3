package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.ApiBO;
import io.github.pnoker.common.auth.entity.builder.ApiBuilder;
import io.github.pnoker.common.auth.repository.ApiFilter;
import io.github.pnoker.common.auth.repository.ReactiveApiStore;
import io.github.pnoker.common.auth.security.PermissionCacheInvalidator;
import io.github.pnoker.common.auth.service.ReactiveApiService;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReactiveApiServiceImpl implements ReactiveApiService {

    private final ReactiveApiStore store;
    private final ApiBuilder builder;
    private PermissionCacheInvalidator permissionCacheInvalidator;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPermissionCacheInvalidator(PermissionCacheInvalidator invalidator) { this.permissionCacheInvalidator = invalidator; }

    @Override public Mono<ApiBO> getById(Long id) {
        if (!valid(id)) return Mono.error(new RequestException("API ID is required"));
        return Mono.defer(() -> store.getById(id)).map(builder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("API")));
    }

    @Override public Mono<OffsetPage<ApiBO>> list(ApiFilter filter) {
        return Mono.defer(() -> store.list(filter)).map(page -> OffsetPage.of(
                page.items().stream().map(builder::buildBOByDO).toList(), page.offset(), page.limit(), page.total()));
    }

    @Override public Mono<ApiBO> add(ApiBO api) {
        return Mono.defer(() -> {
            validate(api, false);
            if (api.getApiCode() == null || api.getApiCode().isBlank()) api.setApiCode(CodeUtil.getCode());
            else api.setApiCode(api.getApiCode().trim());
            return Mono.defer(() -> store.existsDuplicate(api)).flatMap(duplicate -> duplicate
                    ? Mono.<ApiBO>error(new DuplicateException("API has been duplicated"))
                    : Mono.defer(() -> store.insert(api))
                            .switchIfEmpty(Mono.error(new ServiceException("API insert returned no row")))
                            .doOnSuccess(saved -> invalidateAll()).map(builder::buildBOByDO))
                    .onErrorMap(DuplicateKeyException.class, error -> new DuplicateException("API code is already in use"));
        });
    }

    @Override public Mono<ApiBO> update(ApiBO api) {
        return Mono.defer(() -> {
            validate(api, true);
            if (api.getApiCode() == null || api.getApiCode().isBlank()) return Mono.error(new RequestException("API code is required"));
            api.setApiCode(api.getApiCode().trim());
            return Mono.defer(() -> store.existsDuplicate(api)).flatMap(duplicate -> duplicate
                    ? Mono.<ApiBO>error(new DuplicateException("API has been duplicated"))
                    : Mono.defer(() -> store.update(api)).doOnSuccess(saved -> invalidateAll()).map(builder::buildBOByDO))
                    .switchIfEmpty(Mono.error(new NotFoundException("API")))
                    .onErrorMap(DuplicateKeyException.class, error -> new DuplicateException("API code is already in use"));
        });
    }

    @Override public Mono<Void> delete(Long id, Long operatorId, String operatorName) {
        return getById(id).then(Mono.defer(() -> store.delete(id, operatorId, operatorName)))
                .defaultIfEmpty(false)
                .doOnSuccess(deleted -> invalidateAll())
                .flatMap(deleted -> Boolean.TRUE.equals(deleted) ? Mono.<Void>empty() : Mono.error(new NotFoundException("API")));
    }

    private void validate(ApiBO api, boolean update) {
        if (api == null || (update && !valid(api.getId()))) throw new RequestException("API is invalid");
        if (api.getApiName() == null || api.getApiName().isBlank() || api.getApiTypeFlag() == null) {
            throw new RequestException("API name and type are required");
        }
        api.setApiName(api.getApiName().trim());
        api.setServiceName(api.getServiceName() == null ? "" : api.getServiceName().trim());
        api.setApiGroup(api.getApiGroup() == null ? "" : api.getApiGroup().trim());
    }

    private void invalidateAll() { if (permissionCacheInvalidator != null) permissionCacheInvalidator.invalidateAll(); }
    private boolean valid(Long id) { return id != null && id > 0; }
}
