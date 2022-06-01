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
import com.dc3.center.data.save.strategy.SaveStrategyFactory;
import com.dc3.center.data.save.strategy.SaveStrategyService;
import com.dc3.common.bean.point.PointValue;
import com.dc3.common.constant.CommonConstant;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "data.point.sava.elasticsearch.enable", havingValue = "true")
public class ElasticsearchService implements SaveStrategyService, InitializingBean {

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Override
    public void savePointValue(PointValue pointValue) {
    }

    @Override
    public void savePointValues(List<PointValue> pointValues) {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        SaveStrategyFactory.put(CommonConstant.StrategyService.POINT_VALUE_SAVE_STRATEGY_ELASTICSEARCH, this);
    }
}