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

package com.dc3.center.data.service.impl;

import com.dc3.center.data.strategy.factory.SaveStrategyFactory;
import com.dc3.center.data.strategy.service.SaveStrategyService;
import com.dc3.center.data.service.PointValueHandleService;
import com.dc3.common.bean.point.PointValue;
import com.dc3.common.constant.CommonConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class PointValueSaveServiceImpl implements PointValueHandleService {

    @Value("${data.point.sava.opentsdb.enable}")
    private Boolean enableOpentsdb;
    @Value("${data.point.sava.elasticsearch.enable}")
    private Boolean enableElasticsearch;

    @Override
    public void postHandle(PointValue pointValue) {
        if (enableOpentsdb) {
            SaveStrategyService saveStrategyService = SaveStrategyFactory.get(CommonConstant.StrategyService.POINT_VALUE_SAVE_STRATEGY_OPENTSDB);
            saveStrategyService.savePointValue(pointValue);
        }

        if (enableElasticsearch) {
            SaveStrategyService saveStrategyService = SaveStrategyFactory.get(CommonConstant.StrategyService.POINT_VALUE_SAVE_STRATEGY_ELASTICSEARCH);
            saveStrategyService.savePointValue(pointValue);
        }
    }

    @Override
    public void postHandle(List<PointValue> pointValues) {
        if (enableOpentsdb) {
            SaveStrategyService saveStrategyService = SaveStrategyFactory.get(CommonConstant.StrategyService.POINT_VALUE_SAVE_STRATEGY_OPENTSDB);
            saveStrategyService.savePointValues(pointValues);
        }

        if (enableElasticsearch) {
            SaveStrategyService saveStrategyService = SaveStrategyFactory.get(CommonConstant.StrategyService.POINT_VALUE_SAVE_STRATEGY_ELASTICSEARCH);
            saveStrategyService.savePointValues(pointValues);
        }
    }

}
