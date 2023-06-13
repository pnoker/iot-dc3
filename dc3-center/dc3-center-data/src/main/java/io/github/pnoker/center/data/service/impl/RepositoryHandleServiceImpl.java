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

package io.github.pnoker.center.data.service.impl;

import io.github.pnoker.center.data.service.RepositoryHandleService;
import io.github.pnoker.center.data.service.RepositoryService;
import io.github.pnoker.center.data.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import io.github.pnoker.common.entity.point.PointValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class RepositoryHandleServiceImpl implements RepositoryHandleService {

    @Value("${data.point.sava.influxdb.enable}")
    private Boolean enableInfluxdb;
    @Value("${data.point.sava.tdengine.enable}")
    private Boolean enableTDengine;
    @Value("${data.point.sava.opentsdb.enable}")
    private Boolean enableOpentsdb;
    @Value("${data.point.sava.elasticsearch.enable}")
    private Boolean enableElasticsearch;

    @Resource(name = "redisServiceImpl")
    private RepositoryService redisRepositoryService;
    @Resource(name = "mongoServiceImpl")
    private RepositoryService mongoRepositoryService;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void save(PointValue pointValue) {
        // 保存单个数据到 Redis & Mongo
        savePointValueToRepository(pointValue, redisRepositoryService, mongoRepositoryService);

        // 保存单个数据到 Influxdb
        if (Boolean.TRUE.equals(enableInfluxdb)) {
            RepositoryService repositoryService = RepositoryStrategyFactory.get(StrategyConstant.Storage.INFLUXDB);
            savePointValueToRepository(pointValue, repositoryService);
        }

        // 保存单个数据到 TDengine
        if (Boolean.TRUE.equals(enableTDengine)) {
            RepositoryService repositoryService = RepositoryStrategyFactory.get(StrategyConstant.Storage.TDENGINE);
            savePointValueToRepository(pointValue, repositoryService);
        }

        // 保存单个数据到 Opentsdb
        if (Boolean.TRUE.equals(enableOpentsdb)) {
            RepositoryService repositoryService = RepositoryStrategyFactory.get(StrategyConstant.Storage.STRATEGY_OPENTSDB);
            savePointValueToRepository(pointValue, repositoryService);
        }

        // 保存单个数据到 Elasticsearch
        if (Boolean.TRUE.equals(enableElasticsearch)) {
            RepositoryService repositoryService = RepositoryStrategyFactory.get(StrategyConstant.Storage.STRATEGY_ELASTICSEARCH);
            savePointValueToRepository(pointValue, repositoryService);
        }
    }

    @Override
    public void save(List<PointValue> pointValues) {
        final Map<String, List<PointValue>> group = pointValues.stream().collect(Collectors.groupingBy(PointValue::getDeviceId));

        group.forEach((deviceId, values) -> {
            // 保存批量数据到 Redis & Mongo
            savePointValuesToRepository(deviceId, values, redisRepositoryService, mongoRepositoryService);

            // 保存批量数据到 Influxdb
            if (Boolean.TRUE.equals(enableInfluxdb)) {
                RepositoryService repositoryService = RepositoryStrategyFactory.get(StrategyConstant.Storage.INFLUXDB);
                savePointValuesToRepository(deviceId, values, repositoryService);
            }

            // 保存批量数据到 tdengine
            if (Boolean.TRUE.equals(enableTDengine)) {
                RepositoryService repositoryService = RepositoryStrategyFactory.get(StrategyConstant.Storage.INFLUXDB);
                savePointValuesToRepository(deviceId, values, repositoryService);
            }

            // 保存批量数据到 Opentsdb
            if (Boolean.TRUE.equals(enableOpentsdb)) {
                RepositoryService repositoryService = RepositoryStrategyFactory.get(StrategyConstant.Storage.STRATEGY_OPENTSDB);
                savePointValuesToRepository(deviceId, values, repositoryService);
            }

            // 保存批量数据到 Elasticsearch
            if (Boolean.TRUE.equals(enableElasticsearch)) {
                RepositoryService repositoryService = RepositoryStrategyFactory.get(StrategyConstant.Storage.STRATEGY_ELASTICSEARCH);
                savePointValuesToRepository(deviceId, values, repositoryService);
            }
        });
    }

    /**
     * 保存 PointValue 到指定存储服务
     *
     * @param pointValue         PointValue
     * @param repositoryServices RepositoryService Array
     */
    private void savePointValueToRepository(PointValue pointValue, RepositoryService... repositoryServices) {
        for (RepositoryService repositoryService : repositoryServices) {
            threadPoolExecutor.execute(() -> {
                try {
                    repositoryService.savePointValue(pointValue);
                } catch (Exception e) {
                    log.error("Save point value to {} error {}", repositoryService.getRepositoryName(), e.getMessage());
                }
            });
        }

    }

    /**
     * 保存 PointValues 到指定存储服务
     *
     * @param deviceId           设备ID
     * @param pointValues        PointValue Array
     * @param repositoryServices RepositoryService Array
     */
    private void savePointValuesToRepository(String deviceId, List<PointValue> pointValues, RepositoryService... repositoryServices) {
        for (RepositoryService repositoryService : repositoryServices) {
            threadPoolExecutor.execute(() -> {
                try {
                    repositoryService.savePointValues(deviceId, pointValues);
                } catch (Exception e) {
                    log.error("Save point values to {} error {}", repositoryService.getRepositoryName(), e.getMessage());
                }
            });
        }
    }
}
