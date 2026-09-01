package io.github.pnoker.common.data.service.impl;

import io.github.pnoker.common.data.biz.alarm.NotifyConfigCache;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBuilder;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.query.NotifyChannelQuery;
import io.github.pnoker.common.data.repository.ReactiveNotifyAdminStore;
import io.github.pnoker.common.data.service.NotifyChannelService;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class NotifyChannelServiceImpl implements NotifyChannelService {
    private final NotifyChannelBuilder builder;
    private final ReactiveNotifyAdminStore store;
    private final NotifyConfigCache cache;
    @Override public Mono<NotifyChannelBO> add(NotifyChannelBO value) { return Mono.defer(() -> { validate(value); NotifyChannelDO data = builder.buildDOByBO(value); return store.existsChannelCode(value(value.getTenantId()), data.getChannelCode(), null).flatMap(exists -> exists ? Mono.<NotifyChannelBO>error(new DuplicateException("Notify channel has been duplicated")) : store.insertChannel(data).map(builder::buildBOByDO).switchIfEmpty(Mono.error(new AddException("Failed to create notify channel")))).doOnSuccess(result -> { if (result != null) cache.invalidateChannel(result.getId()); }).onErrorMap(DataIntegrityViolationException.class, error -> new DuplicateException("Notify channel has been duplicated")); }); }
    @Override public Mono<Boolean> delete(Long tenantId, Long id) { return store.getChannel(value(tenantId), value(id)).switchIfEmpty(Mono.error(new NotFoundException("Notify channel does not exist"))).flatMap(existing -> store.hasChannelBindings(value(tenantId), value(id)).flatMap(has -> has ? Mono.<Boolean>error(new AssociatedException("Failed to remove notify channel: bindings exist")) : store.deleteChannel(value(tenantId), value(id)))).flatMap(ok -> ok ? Mono.just(true) : Mono.error(new DeleteException("Failed to remove notify channel"))).doOnSuccess(ok -> cache.invalidateChannel(id)); }
    @Override public Mono<NotifyChannelBO> update(NotifyChannelBO value) { return Mono.defer(() -> { validate(value); NotifyChannelDO data = builder.buildDOByBO(value); return store.getChannel(value(value.getTenantId()), value(value.getId())).switchIfEmpty(Mono.error(new NotFoundException("Notify channel does not exist"))).then(store.existsChannelCode(value(value.getTenantId()), data.getChannelCode(), value.getId())).flatMap(exists -> exists ? Mono.<NotifyChannelBO>error(new DuplicateException("Notify channel has been duplicated")) : store.updateChannel(data).map(builder::buildBOByDO).switchIfEmpty(Mono.error(new UpdateException("Failed to update notify channel")))).doOnSuccess(result -> { if (result != null) cache.invalidateChannel(result.getId()); }).onErrorMap(DataIntegrityViolationException.class, error -> new DuplicateException("Notify channel has been duplicated")); }); }
    @Override public Mono<NotifyChannelBO> getById(Long tenantId, Long id) { return store.getChannel(value(tenantId), value(id)).switchIfEmpty(Mono.error(new NotFoundException("Notify channel does not exist"))).map(builder::buildBOByDO); }
    @Override public Mono<OffsetPage<NotifyChannelBO>> list(Long tenantId, NotifyChannelQuery query) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            NotifyChannelQuery request = query == null ? new NotifyChannelQuery() : query;
            PageRequest page = new PageRequest(request.getOffset(), request.getLimit(), request.getSort());
            return store.listChannel(tenantId, request.getChannelName(), request.getChannelCode(), request.getChannelTypeFlag(), request.getEnableFlag(), page)
                    .map(result -> OffsetPage.of(result.items().stream().map(builder::buildBOByDO).toList(), result.offset(), result.limit(), result.total()));
        });
    }
    private void validate(NotifyChannelBO value) { if (value == null || value.getTenantId() == null || value.getTenantId() <= 0) throw new IllegalArgumentException("tenantId is required"); }
    private long value(Long value) { return value == null ? 0 : value; }
    private void requireTenant(Long tenantId) { if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId is required"); }
}
