package io.github.pnoker.common.auth.biz;

import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncCommand;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncResult;
import reactor.core.publisher.Mono;

public interface ReactiveResourceRegistrySyncService {

    Mono<ResourceRegistrySyncResult> sync(ResourceRegistrySyncCommand command);
}
