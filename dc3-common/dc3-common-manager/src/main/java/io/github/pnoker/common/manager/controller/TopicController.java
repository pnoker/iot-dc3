/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.manager.entity.query.TopicQuery;
import io.github.pnoker.common.manager.entity.vo.TopicVO;
import io.github.pnoker.common.manager.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


import java.util.Objects;

/**
 * REST controller exposing topic management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */

@Slf4j
@RestController
@RequestMapping(ManagerConstant.TOPIC_URL_PREFIX)
@RequiredArgsConstructor
public class TopicController implements BaseController {

    private final TopicService topicService;

    @PostMapping("/list")
    public Mono<R<Page<TopicVO>>> query(@RequestBody(required = false) TopicQuery topicQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            TopicQuery query = Objects.isNull(topicQuery) ? new TopicQuery() : topicQuery;
            query.setTenantId(tenantId);
            Page<TopicVO> topicVOList = topicService.list(query);
            return R.ok(topicVOList);
        }));
    }

}
