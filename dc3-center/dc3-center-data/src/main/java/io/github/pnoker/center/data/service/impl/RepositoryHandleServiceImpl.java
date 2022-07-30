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

package io.github.pnoker.center.data.service.impl;

import io.github.pnoker.center.data.service.RepositoryHandleService;
import io.github.pnoker.center.data.service.RepositoryService;
import io.github.pnoker.center.data.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.constant.CommonConstant;
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
 */
@Slf4j
@Service
public class RepositoryHandleServiceImpl implements RepositoryHandleService {

    @Value("${data.point.sava.influxdb.enable}")
    private Boolean enableInfluxdb;
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
        // 保存单个数据到 Redis
        threadPoolExecutor.execute(() -> {
            try {
                redisRepositoryService.savePointValue(pointValue);
            } catch (Exception e) {
                log.error("Save point value to redis error {}", e.getMessage());
            }
        });

        // 保存单个数据到 Mongo
        threadPoolExecutor.execute(() -> {
            try {
                mongoRepositoryService.savePointValue(pointValue);
            } catch (Exception e) {
                log.error("Save point value to mongo error {}", e.getMessage());
            }
        });

        // 保存单个数据到其他 Repository
        threadPoolExecutor.execute(() -> {
            try {
                if (enableInfluxdb) {
                    // 保存单个数据到 Influxdb
                }

                if (enableOpentsdb) {
                    // 保存单个数据到 Opentsdb
                    RepositoryService repositoryService = RepositoryStrategyFactory.get(CommonConstant.RepositoryStrategy.REPOSITORY_STRATEGY_OPENTSDB);
                    repositoryService.savePointValue(pointValue);
                }

                if (enableElasticsearch) {
                    // 保存单个数据到 Elasticsearch
                    RepositoryService repositoryService = RepositoryStrategyFactory.get(CommonConstant.RepositoryStrategy.REPOSITORY_STRATEGY_ELASTICSEARCH);
                    repositoryService.savePointValue(pointValue);
                }
            } catch (Exception e) {
                log.error("Save point value to repository error {}", e.getMessage());
            }
        });
    }

    @Override
    public void save(List<PointValue> pointValues) {
        final Map<String, List<PointValue>> group = pointValues.stream().collect(Collectors.groupingBy(PointValue::getDeviceId));

        group.forEach((deviceId, values) -> {
            // 保存批量数据到 Redis
            threadPoolExecutor.execute(() -> {
                try {
                    redisRepositoryService.savePointValues(deviceId, values);
                } catch (Exception e) {
                    log.error("Save point values to redis error {}", e.getMessage());
                }
            });

            // 保存批量数据到 Mongo
            threadPoolExecutor.execute(() -> {
                try {
                    mongoRepositoryService.savePointValues(deviceId, values);
                } catch (Exception e) {
                    log.error("Save point values to mongo error {}", e.getMessage());
                }
            });

            // 保存批量数据到其他 Repository
            threadPoolExecutor.execute(() -> {
                try {
                    if (enableInfluxdb) {
                        // 保存批量数据到 Influxdb
                    }

                    if (enableOpentsdb) {
                        // 保存批量数据到 Opentsdb
                        RepositoryService repositoryService = RepositoryStrategyFactory.get(CommonConstant.RepositoryStrategy.REPOSITORY_STRATEGY_OPENTSDB);
                        repositoryService.savePointValues(deviceId, values);
                    }

                    if (enableElasticsearch) {
                        // 保存批量数据到 Elasticsearch
                        RepositoryService repositoryService = RepositoryStrategyFactory.get(CommonConstant.RepositoryStrategy.REPOSITORY_STRATEGY_ELASTICSEARCH);
                        repositoryService.savePointValues(deviceId, values);
                    }
                } catch (Exception e) {
                    log.error("Save point values to repository error {}", e.getMessage());
                }
            });
        });

    }

}
