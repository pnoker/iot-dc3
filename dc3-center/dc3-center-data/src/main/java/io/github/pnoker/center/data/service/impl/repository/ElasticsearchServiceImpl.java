/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.data.service.impl.repository;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import io.github.pnoker.center.data.service.RepositoryService;
import io.github.pnoker.center.data.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.bean.point.EsPointValue;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.constant.CommonConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "data.point.sava.elasticsearch.enable", havingValue = "true")
public class ElasticsearchServiceImpl implements RepositoryService, InitializingBean {

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Override
    public void savePointValue(PointValue pointValue) {
        if (!StrUtil.isAllNotEmpty(pointValue.getDeviceId(), pointValue.getPointId())) {
            return;
        }

        final String index = CommonConstant.Storage.POINT_VALUE_PREFIX + pointValue.getDeviceId();
        IndexRequest<EsPointValue> indexRequest = new IndexRequest.Builder<EsPointValue>()
                .index(index)
                .document(new EsPointValue(pointValue))
                .build();
        try {
            IndexResponse response = elasticsearchClient.index(indexRequest);
            log.info("Send pointValue to elasticsearch, Response: {}, Version: {}", response.result(), response.version());
        } catch (IOException e) {
            log.error("Send pointValue to elasticsearch error: {}", e.getMessage(), e);
        }
    }

    @Override
    public void savePointValues(String deviceId, List<PointValue> pointValues) {
        if (StrUtil.isEmpty(deviceId)) {
            return;
        }

        final String index = CommonConstant.Storage.POINT_VALUE_PREFIX + deviceId;
        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
        pointValues.stream()
                .filter(pointValue -> StrUtil.isNotEmpty(pointValue.getPointId()))
                .forEach(pointValue -> bulkRequestBuilder.operations(operation -> operation
                        .index(builder -> builder
                                .index(index)
                                .document(new EsPointValue(pointValue))
                        )
                ));

        try {
            BulkResponse response = elasticsearchClient.bulk(bulkRequestBuilder.build());
            if (response.errors()) {
                for (BulkResponseItem item : response.items()) {
                    if (null != item.error()) {
                        log.error("Send pointValue to elasticsearch error: {}", item.error().reason());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Send pointValue to elasticsearch error: {}", e.getMessage(), e);
        }

    }

    @Override
    public void afterPropertiesSet() {
        RepositoryStrategyFactory.put(CommonConstant.RepositoryStrategy.REPOSITORY_STRATEGY_ELASTICSEARCH, this);
    }

}