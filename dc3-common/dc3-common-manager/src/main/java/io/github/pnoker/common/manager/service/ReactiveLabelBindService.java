package io.github.pnoker.common.manager.service;
import io.github.pnoker.common.manager.entity.bo.LabelBindBO;import io.github.pnoker.common.manager.repository.BindingFilter;import io.github.pnoker.db.r2dbc.core.page.OffsetPage;import reactor.core.publisher.Mono;
public interface ReactiveLabelBindService{Mono<LabelBindBO> add(LabelBindBO value);Mono<LabelBindBO> update(LabelBindBO value);Mono<Boolean> delete(Long tenantId,Long id,Long operatorId,String operatorName);Mono<LabelBindBO> getById(Long tenantId,Long id);Mono<OffsetPage<LabelBindBO>> list(BindingFilter filter);}
