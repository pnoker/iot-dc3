/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 */
package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.repository.ProfileFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive application service for profile metadata. */
public interface ReactiveProfileService {
    Mono<ProfileBO> getById(Long tenantId, Long id);
    Mono<ProfileBO> add(ProfileBO value);
    Mono<ProfileBO> update(ProfileBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<ProfileBO> getByNameAndType(Long tenantId, String name, ProfileTypeEnum type);
    Flux<ProfileBO> listByIds(Long tenantId, List<Long> ids);
    Flux<ProfileBO> listByDeviceId(Long tenantId, Long deviceId);
    Mono<OffsetPage<ProfileBO>> list(ProfileFilter filter);
}
