package io.github.pnoker.common.manager.service;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.repository.EventFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;import reactor.core.publisher.Mono;import java.util.List;
public interface ReactiveEventService { Mono<EventBO> getById(Long tenantId,Long id); Mono<EventBO> add(EventBO value); Mono<EventBO> update(EventBO value); Mono<Boolean> delete(Long tenantId,Long id,int expectedVersion,Long operatorId,String operatorName); Flux<EventBO> listByIds(Long tenantId,List<Long> ids); Flux<EventBO> listByProfileId(Long tenantId,Long profileId); Flux<EventBO> listByDeviceId(Long tenantId,Long deviceId); Mono<OffsetPage<EventBO>> list(EventFilter filter); }
