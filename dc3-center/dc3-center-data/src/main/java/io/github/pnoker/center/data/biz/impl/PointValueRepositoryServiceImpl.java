/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.center.data.biz.impl;

import io.github.pnoker.center.data.biz.PointValueRepositoryService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.repository.RepositoryService;
import lombok.extern.slf4j.Slf4j;
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
public class PointValueRepositoryServiceImpl implements PointValueRepositoryService {

    @Resource(name = "redisServiceImpl")
    private RepositoryService redisRepositoryService;
    @Resource(name = "mongoServiceImpl")
    private RepositoryService mongoRepositoryService;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void save(PointValueBO pointValueBO) {
        // 保存单个数据到 Redis & Mongo
        savePointValueToRepository(pointValueBO, redisRepositoryService, mongoRepositoryService);
    }

    @Override
    public void save(List<PointValueBO> pointValueBOS) {
        final Map<Long, List<PointValueBO>> group = pointValueBOS.stream().collect(Collectors.groupingBy(PointValueBO::getDeviceId));

        group.forEach((deviceId, values) -> {
            // 保存批量数据到 Redis & Mongo
            savePointValuesToRepository(deviceId, values, redisRepositoryService, mongoRepositoryService);
        });
    }

    /**
     * 保存 PointValue 到指定存储服务
     *
     * @param pointValueBO       PointValue
     * @param repositoryServices RepositoryService Array
     */
    private void savePointValueToRepository(PointValueBO pointValueBO, RepositoryService... repositoryServices) {
        for (RepositoryService repositoryService : repositoryServices) {
            threadPoolExecutor.execute(() -> {
                try {
                    repositoryService.savePointValue(pointValueBO);
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
     * @param pointValueBOS      PointValue Array
     * @param repositoryServices RepositoryService Array
     */
    private void savePointValuesToRepository(Long deviceId, List<PointValueBO> pointValueBOS, RepositoryService... repositoryServices) {
        for (RepositoryService repositoryService : repositoryServices) {
            threadPoolExecutor.execute(() -> {
                try {
                    repositoryService.savePointValues(deviceId, pointValueBOS);
                } catch (Exception e) {
                    log.error("Save point values to {} error {}", repositoryService.getRepositoryName(), e.getMessage());
                }
            });
        }
    }
}
