package io.github.pnoker.common.manager.biz;

import io.github.pnoker.common.entity.option.DictionaryOption;
import io.github.pnoker.common.manager.entity.query.DictionaryListRequest;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Read-only option query service for manager entities. */
public interface DictionaryForManagerService {

    Mono<OffsetPage<DictionaryOption>> listDriverOptions(Long tenantId, DictionaryListRequest request);

    Mono<OffsetPage<DictionaryOption>> listProfileOptions(Long tenantId, DictionaryListRequest request);

    Mono<OffsetPage<DictionaryOption>> listProfilePointOptions(Long tenantId, DictionaryListRequest request);

    Mono<OffsetPage<DictionaryOption>> listDevicePointOptions(Long tenantId, DictionaryListRequest request);

    Mono<OffsetPage<DictionaryOption>> listDeviceOptions(Long tenantId, DictionaryListRequest request);

    Mono<OffsetPage<DictionaryOption>> listDriverDeviceOptions(Long tenantId, DictionaryListRequest request);

}
