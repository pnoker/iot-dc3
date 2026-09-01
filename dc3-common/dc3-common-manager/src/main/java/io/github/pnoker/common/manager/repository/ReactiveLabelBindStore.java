package io.github.pnoker.common.manager.repository;
import io.github.pnoker.common.manager.entity.bo.LabelBindBO;import io.github.pnoker.db.r2dbc.core.page.OffsetPage;import reactor.core.publisher.Mono;
public interface ReactiveLabelBindStore{Mono<OffsetPage<LabelBindBO>> list(BindingFilter filter);Mono<LabelBindBO> get(Long tenantId,Long id);Mono<LabelBindBO> getByEntity(Long tenantId,byte type,Long labelId,Long entityId);Mono<LabelBindBO> insert(LabelBindBO value);Mono<LabelBindBO> update(LabelBindBO value);Mono<Boolean> delete(Long tenantId,Long id,Long operatorId,String operatorName);}
