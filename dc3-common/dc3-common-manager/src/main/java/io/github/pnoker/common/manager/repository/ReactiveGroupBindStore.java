package io.github.pnoker.common.manager.repository;
import io.github.pnoker.common.manager.entity.bo.GroupBindBO;import io.github.pnoker.db.r2dbc.core.page.OffsetPage;import reactor.core.publisher.Mono;
public interface ReactiveGroupBindStore{Mono<OffsetPage<GroupBindBO>> list(BindingFilter filter);Mono<GroupBindBO> get(Long tenantId,Long id);Mono<GroupBindBO> getByEntity(Long tenantId,byte type,Long entityId);Mono<GroupBindBO> insert(GroupBindBO value);Mono<GroupBindBO> update(GroupBindBO value);Mono<Boolean> delete(Long tenantId,Long id,Long operatorId,String operatorName);}
