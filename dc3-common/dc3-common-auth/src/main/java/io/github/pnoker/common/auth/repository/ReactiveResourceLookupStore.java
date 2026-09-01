package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.ResourceDO;
import reactor.core.publisher.Flux;

import java.util.Collection;

/** Non-blocking read port for globally registered resources. */
public interface ReactiveResourceLookupStore {
    Flux<ResourceDO> listEnabledByIds(Collection<Long> ids);
}
