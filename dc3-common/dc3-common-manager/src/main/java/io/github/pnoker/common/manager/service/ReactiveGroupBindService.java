package io.github.pnoker.common.manager.service;
import io.github.pnoker.common.manager.entity.bo.GroupBindBO;import io.github.pnoker.common.manager.repository.BindingFilter;import io.github.pnoker.db.r2dbc.core.page.OffsetPage;import reactor.core.publisher.Mono;
public interface ReactiveGroupBindService{Mono<GroupBindBO> add(GroupBindBO value);Mono<GroupBindBO> update(GroupBindBO value);Mono<Boolean> delete(Long tenantId,Long id,Long operatorId,String operatorName);Mono<GroupBindBO> getById(Long tenantId,Long id);Mono<OffsetPage<GroupBindBO>> list(BindingFilter filter);}
