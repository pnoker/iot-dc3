/*
 * Copyright 2016-present the original author or authors.
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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import io.github.pnoker.center.data.service.RepositoryService;
import io.github.pnoker.center.data.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.constant.driver.StorageConstant;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import io.github.pnoker.common.entity.point.EsPointValue;
import io.github.pnoker.common.entity.point.PointValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "data.point.sava.elasticsearch.enable", havingValue = "true")
public class ElasticsearchServiceImpl implements RepositoryService, InitializingBean {

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Override
    public String getRepositoryName() {
        return StrategyConstant.Storage.STRATEGY_ELASTICSEARCH;
    }

    @Override
    public void savePointValue(PointValue pointValue) throws IOException {
        if (!CharSequenceUtil.isAllNotEmpty(pointValue.getDeviceId(), pointValue.getPointId())) {
            return;
        }

        final String index = StorageConstant.POINT_VALUE_PREFIX + pointValue.getDeviceId();
        IndexRequest<EsPointValue> indexRequest = new IndexRequest.Builder<EsPointValue>()
                .index(index)
                .document(new EsPointValue(pointValue))
                .build();
        elasticsearchClient.index(indexRequest);
    }

    @Override
    public void savePointValues(String deviceId, List<PointValue> pointValues) throws IOException {
        if (CharSequenceUtil.isEmpty(deviceId)) {
            return;
        }

        final String index = StorageConstant.POINT_VALUE_PREFIX + deviceId;
        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
        pointValues.stream()
                .filter(pointValue -> CharSequenceUtil.isNotEmpty(pointValue.getPointId()))
                .forEach(pointValue -> bulkRequestBuilder.operations(operation -> operation
                        .index(builder -> builder
                                .index(index)
                                .document(new EsPointValue(pointValue))
                        )
                ));

        BulkResponse response = elasticsearchClient.bulk(bulkRequestBuilder.build());
        if (response.errors()) {
            for (BulkResponseItem item : response.items()) {
                if (ObjectUtil.isNotNull(item.error())) {
                    // todo 存在没有保存成功的数据怎么办
                    log.error("Send pointValues to elasticsearch error: {}", item.error().reason());
                }
            }
        }
    }

    @Override
    public void afterPropertiesSet() {
        RepositoryStrategyFactory.put(StrategyConstant.Storage.STRATEGY_ELASTICSEARCH, this);
    }

}