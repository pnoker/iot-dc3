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

package io.github.pnoker.common.data.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.GrpcPagePointQuery;
import io.github.pnoker.api.center.manager.GrpcRPagePointDTO;
import io.github.pnoker.api.center.manager.PointApiGrpc;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.exception.RepositoryException;
import io.github.pnoker.common.redis.service.RedisRepositoryService;
import io.github.pnoker.common.repository.RepositoryService;
import io.github.pnoker.common.strategy.RepositoryStrategyFactory;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointValueServiceImpl implements PointValueService {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private PointApiGrpc.PointApiBlockingStub pointApiBlockingStub;

    @Resource
    private RedisRepositoryService redisRepositoryService;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void save(PointValueBO pointValueBO) {
        if (Objects.isNull(pointValueBO)) {
            return;
        }

        pointValueBO.setCreateTime(LocalDateTime.now());
        savePointValueToRepository(pointValueBO);
    }

    @Override
    public void save(List<PointValueBO> pointValueBOList) {
        if (CollUtil.isEmpty(pointValueBOList)) {
            return;
        }

        final Map<Long, List<PointValueBO>> group = pointValueBOList.stream()
                .map(pointValue -> {
                    pointValue.setCreateTime(LocalDateTime.now());
                    return pointValue;
                })
                .collect(Collectors.groupingBy(PointValueBO::getDeviceId));

        group.forEach(this::savePointValuesToRepository);
    }

    @Override
    public List<String> history(Long deviceId, Long pointId, int count) {
        if (Objects.isNull(deviceId) || Objects.isNull(pointId)) {
            return Collections.emptyList();
        }
        if (count < 1) {
            count = 100;
        }
        if (count > 500) {
            count = 500;
        }

        RepositoryService repositoryService = getFirstRepositoryService();
        return repositoryService.selectHistoryPointValue(deviceId, pointId, count);
    }

    @Override
    public Page<PointValueBO> latest(PointValueQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }

        Page<PointValueBO> entityPageBO = new Page<>();
        entityPageBO.setCurrent(entityQuery.getPage().getCurrent()).setSize(entityQuery.getPage().getSize());

        GrpcPage.Builder entityPageGrpcDTO = GrpcPage.newBuilder()
                .setSize(entityQuery.getPage().getSize())
                .setCurrent(entityQuery.getPage().getCurrent());
        GrpcPagePointQuery.Builder query = GrpcPagePointQuery.newBuilder()
                .setPage(entityPageGrpcDTO);
        if (CharSequenceUtil.isNotEmpty(entityQuery.getPointName())) {
            query.setPointName(entityQuery.getPointName());
        }
        query.setPointTypeFlag(DefaultConstant.NULL_INT);
        query.setRwFlag(DefaultConstant.NULL_INT);
        query.setProfileId(DefaultConstant.NULL_INT);
        query.setTenantId(entityQuery.getTenantId());
        if (Objects.nonNull(entityQuery.getDeviceId())) {
            query.setDeviceId(entityQuery.getDeviceId());
        }
        Optional.ofNullable(entityQuery.getEnableFlag()).ifPresentOrElse(value -> query.setEnableFlag(value.getIndex()), () -> query.setEnableFlag(DefaultConstant.NULL_INT));
        GrpcRPagePointDTO rPagePointDTO = pointApiBlockingStub.selectByPage(query.build());
        if (!rPagePointDTO.getResult().getOk()) {
            return entityPageBO;
        }

        List<GrpcPointDTO> points = rPagePointDTO.getData().getDataList();
        List<Long> pointIds = points.stream().map(p -> p.getBase().getId()).toList();

        Map<Long, PointValueBO> pointValueBOMap = redisRepositoryService.selectLatestPointValue(entityQuery.getDeviceId(), pointIds);
        RepositoryService repositoryService = getFirstRepositoryService();
        List<PointValueBO> pointValueBOList = pointIds.stream().map(id -> {
            PointValueBO value = pointValueBOMap.get(id);
            return Objects.isNull(value) ? repositoryService.selectLatestPointValue(entityQuery.getDeviceId(), id) : value;
        }).filter(Objects::nonNull).toList();

        entityPageBO.setCurrent(rPagePointDTO.getData().getPage().getCurrent())
                .setSize(rPagePointDTO.getData().getPage().getSize())
                .setTotal(rPagePointDTO.getData().getPage().getTotal())
                .setRecords(pointValueBOList);

        return entityPageBO;
    }

    @Override
    @SneakyThrows
    public Page<PointValueBO> page(PointValueQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }

        RepositoryService repositoryService = getFirstRepositoryService();
        return repositoryService.selectPagePointValue(entityQuery);
    }

    /**
     * 保存 PointValue 到指定存储服务
     *
     * @param pointValueBO PointValue
     */
    private void savePointValueToRepository(PointValueBO pointValueBO) {
        try {
            // redis repository
            redisRepositoryService.savePointValue(pointValueBO);

            // other repository
            RepositoryService repositoryService = getFirstRepositoryService();
            repositoryService.savePointValue(pointValueBO);
        } catch (Exception e) {
            log.error("Save point value to error {}", e.getMessage());
        }
    }

    /**
     * 保存 PointValues 到指定存储服务
     *
     * @param deviceId                   设备ID
     * @param pointValueBOListPointValue Array
     */
    private void savePointValuesToRepository(Long deviceId, List<PointValueBO> pointValueBOList) {
        try {
            // redis repository
            redisRepositoryService.savePointValue(deviceId, pointValueBOList);

            // other repository
            RepositoryService repositoryService = getFirstRepositoryService();
            List<List<PointValueBO>> splitPointValueBOList = ListUtil.split(pointValueBOList, 100);
            for (List<PointValueBO> splitPointValueBO : splitPointValueBOList) {
                repositoryService.savePointValue(deviceId, splitPointValueBO);
            }
        } catch (Exception e) {
            log.error("Save point values to error {}", e.getMessage());
        }
    }

    /**
     * 获取数据存储服务
     *
     * @return RepositoryService
     */
    private RepositoryService getFirstRepositoryService() {
        List<RepositoryService> repositoryServices = RepositoryStrategyFactory.get();
        if (!repositoryServices.isEmpty() && repositoryServices.size() > 1) {
            throw new RepositoryException("Save point values to repository error: There are multiple repository, only one is supported.");
        }

        Optional<RepositoryService> first = repositoryServices.stream().findFirst();
        if (!first.isPresent()) {
            throw new RepositoryException("Save point values to repository error: Please configure at least one repository.");
        }

        return first.get();
    }

}
