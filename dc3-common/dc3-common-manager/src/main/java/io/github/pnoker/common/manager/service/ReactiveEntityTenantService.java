package io.github.pnoker.common.manager.service;
import io.github.pnoker.common.enums.EntityTypeEnum;import reactor.core.publisher.Mono;
/** Reactive tenant guard for polymorphic manager entity bindings. */
public interface ReactiveEntityTenantService{Mono<Void> requireEntityTenant(Long tenantId,EntityTypeEnum entityType,Long entityId);}
