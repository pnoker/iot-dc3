package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.query.TopicOffsetQuery;
import io.github.pnoker.common.manager.entity.vo.TopicVO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

public interface ReactiveTopicService {
    Mono<OffsetPage<TopicVO>> list(TopicOffsetQuery query);
}
