/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.data.save.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.dc3.center.data.strategy.factory.SaveStrategyFactory;
import com.dc3.center.data.strategy.service.SaveStrategyService;
import com.dc3.common.bean.point.EsPointValue;
import com.dc3.common.bean.point.PointValue;
import com.dc3.common.constant.CommonConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "data.point.sava.elasticsearch.enable", havingValue = "true")
public class ElasticsearchService implements SaveStrategyService, InitializingBean {

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Override
    public void savePointValue(PointValue pointValue) {
        IndexRequest<EsPointValue> indexRequest = new IndexRequest.Builder<EsPointValue>()
                .index(CommonConstant.Storage.POINT_VALUE_PREFIX + pointValue.getDeviceId())
                .document(new EsPointValue(pointValue.getDeviceId(), pointValue.getPointId(), pointValue.getRawValue(), pointValue.getValue()))
                .build();
        try {
            IndexResponse response = elasticsearchClient.index(indexRequest);
            log.info("Send pointValue to elasticsearch, Response: {}, Version: {}", response.result(), response.version());
        } catch (IOException e) {
            log.error("Send pointValue to elasticsearch error: {}", e.getMessage(), e);
        }
    }

    @Override
    public void savePointValues(List<PointValue> pointValues) {
        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
        for (PointValue pointValue : pointValues) {
            bulkRequestBuilder.operations(operation -> operation
                    .index(builder -> builder
                            .index(CommonConstant.Storage.POINT_VALUE_PREFIX + pointValue.getDeviceId())
                            .document(new EsPointValue(pointValue.getDeviceId(), pointValue.getPointId(), pointValue.getRawValue(), pointValue.getValue()))
                    )
            );
        }

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
    public void afterPropertiesSet() throws Exception {
        SaveStrategyFactory.put(CommonConstant.StrategyService.POINT_VALUE_SAVE_STRATEGY_ELASTICSEARCH, this);
    }

}