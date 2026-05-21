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

package io.github.pnoker.common.data.biz.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.cache.PointValueLocalCacheService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RepositoryException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import io.github.pnoker.common.repository.RepositoryService;
import io.github.pnoker.common.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business service implementation for point value operations.
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointValueServiceImpl implements PointValueService {

    private final PointFacade pointFacade;

    private final DeviceFacade deviceFacade;

    private final PointValueLocalCacheService pointValueLocalCacheService;

    private final AlarmRuleTriggerService alarmRuleTriggerService;

    @Override
    public void save(PointValueBO pointValueBO) {
        if (Objects.isNull(pointValueBO)) {
            return;
        }

        // create_time carries the driver's acquisition timestamp; operate_time
        // is stamped at persistence. Keeping them distinct lets the dashboard
        // measure the collect→store pipeline latency.
        if (Objects.isNull(pointValueBO.getCreateTime())) {
            pointValueBO.setCreateTime(LocalDateTimeUtil.now());
        }
        pointValueBO.setOperateTime(LocalDateTimeUtil.now());
        savePointValueToRepository(pointValueBO);
        alarmRuleTriggerService.processPointValue(pointValueBO);
    }

    @Override
    public void save(List<PointValueBO> pointValueBOList) {
        if (CollectionUtils.isEmpty(pointValueBOList)) {
            return;
        }

        final Map<Long, List<PointValueBO>> group = pointValueBOList.stream().map(pointValue -> {
            if (Objects.isNull(pointValue.getCreateTime())) {
                pointValue.setCreateTime(LocalDateTimeUtil.now());
            }
            // See single-row save() — operate_time is the persistence
            // timestamp, not a mirror of create_time.
            pointValue.setOperateTime(LocalDateTimeUtil.now());
            return pointValue;
        }).collect(Collectors.groupingBy(PointValueBO::getDeviceId));

        group.forEach(this::savePointValuesToRepository);
        pointValueBOList.forEach(alarmRuleTriggerService::processPointValue);
    }

    @Override
    public List<String> history(Long tenantId, Long deviceId, Long pointId, int count) {
        if (Objects.isNull(tenantId) || Objects.isNull(deviceId) || Objects.isNull(pointId)) {
            return Collections.emptyList();
        }
        validateMetadataScope(tenantId, deviceId, pointId);
        if (count < 1) {
            count = 100;
        }
        if (count > 500) {
            count = 500;
        }

        RepositoryService repositoryService = getFirstRepositoryService();
        return repositoryService.listHistoryPointValue(tenantId, deviceId, pointId, count);
    }

    @Override
    public Page<PointValueBO> latest(PointValueQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        validateMetadataScope(entityQuery.getTenantId(), entityQuery.getDeviceId(), entityQuery.getPointId());

        Page<PointValueBO> entityPageBO = new Page<>();
        entityPageBO.setCurrent(entityQuery.getPage().getCurrent()).setSize(entityQuery.getPage().getSize());

        FacadePointQuery facadeQuery = FacadePointQuery.builder()
                .page(entityQuery.getPage())
                .pointName(entityQuery.getPointName())
                .tenantId(entityQuery.getTenantId())
                .deviceId(entityQuery.getDeviceId())
                .enableFlag(entityQuery.getEnableFlag())
                .build();

        FacadePage<FacadePointBO> page = pointFacade.listByPage(facadeQuery);
        List<Long> pointIds = page.getRecords().stream().map(FacadePointBO::getId).toList();

        if (pointIds.isEmpty()) {
            return entityPageBO;
        }

        Long tenantId = entityQuery.getTenantId();
        Map<Long, PointValueBO> pointValueBOMap = pointValueLocalCacheService.selectLatestPointValue(tenantId,
                entityQuery.getDeviceId(), pointIds);
        RepositoryService repositoryService = getFirstRepositoryService();
        List<PointValueBO> pointValueBOList = pointIds.stream().map(id -> {
            PointValueBO value = pointValueBOMap.get(id);
            if (Objects.isNull(value)) {
                value = repositoryService.selectLatestPointValue(tenantId, entityQuery.getDeviceId(), id);
            }
            return Objects.isNull(value) ? noLatestPointValue(tenantId, entityQuery.getDeviceId(), id)
                    : latestPointValue(value);
        }).toList();

        entityPageBO.setCurrent(page.getCurrent())
                .setSize(page.getSize())
                .setTotal(page.getTotal())
                .setRecords(pointValueBOList);

        return entityPageBO;
    }

    @Override
    @SneakyThrows
    public Page<PointValueBO> page(PointValueQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        validateMetadataScope(entityQuery.getTenantId(), entityQuery.getDeviceId(), entityQuery.getPointId());
        if (Objects.isNull(entityQuery.getCreateTimeFrom())) {
            java.time.LocalDateTime from = io.github.pnoker.common.utils.TimeRangeUtil
                    .resolveFrom(entityQuery.getRangeKey(), entityQuery.getRangeHours());
            if (Objects.nonNull(from)) {
                entityQuery.setCreateTimeFrom(from);
            }
        }

        RepositoryService repositoryService = getFirstRepositoryService();
        return repositoryService.listPagePointValue(entityQuery);
    }

    /**
     * Save PointValue to the specified storage service
     *
     * @param pointValueBO PointValue
     */
    private void savePointValueToRepository(PointValueBO pointValueBO) {
        try {
            // local hot cache
            pointValueLocalCacheService.savePointValue(pointValueBO);

            // other repository
            RepositoryService repositoryService = getFirstRepositoryService();
            repositoryService.savePointValue(pointValueBO);
        } catch (Exception e) {
            log.error("Save point value failed, tenantId={}, deviceId={}, pointId={}", pointValueBO.getTenantId(),
                    pointValueBO.getDeviceId(), pointValueBO.getPointId(), e);
        }
    }

    /**
     * Save PointValues to the specified storage service
     *
     * @param deviceId         Device ID
     * @param pointValueBOList Array
     */
    private void savePointValuesToRepository(Long deviceId, List<PointValueBO> pointValueBOList) {
        try {
            // local hot cache
            pointValueLocalCacheService.savePointValue(deviceId, pointValueBOList);

            // other repository
            RepositoryService repositoryService = getFirstRepositoryService();
            List<List<PointValueBO>> splitPointValueBOList = ListUtils.partition(pointValueBOList, 100);
            for (List<PointValueBO> splitPointValueBO : splitPointValueBOList) {
                repositoryService.savePointValues(splitPointValueBO);
            }
        } catch (Exception e) {
            log.error("Save point values failed, deviceId={}, size={}", deviceId, pointValueBOList.size(), e);
        }
    }

    private void validateMetadataScope(Long tenantId, Long deviceId, Long pointId) {
        if (Objects.isNull(tenantId)) {
            return;
        }

        FacadeDeviceBO device = null;
        if (isValidId(deviceId)) {
            device = deviceFacade.getById(tenantId, deviceId);
            if (Objects.isNull(device)) {
                throw new NotFoundException("Device does not exist");
            }
        }

        FacadePointBO point = null;
        if (isValidId(pointId)) {
            point = pointFacade.getById(tenantId, pointId);
            if (Objects.isNull(point)) {
                throw new NotFoundException("Point does not exist");
            }
        }

        if (Objects.nonNull(device) && Objects.nonNull(point)
                && (Objects.isNull(device.getProfileIds()) || !device.getProfileIds().contains(point.getProfileId()))) {
            throw new NotFoundException("Point does not exist");
        }
    }

    private boolean isValidId(Long id) {
        return Objects.nonNull(id) && id > 0;
    }

    private PointValueBO noLatestPointValue(Long tenantId, Long deviceId, Long pointId) {
        return PointValueBO.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .pointId(pointId)
                .rawValue(DataConstant.PointValue.NO_LATEST_VALUE)
                .calValue(DataConstant.PointValue.NO_LATEST_VALUE)
                .hasLatestValue(false)
                .build();
    }

    private PointValueBO latestPointValue(PointValueBO pointValueBO) {
        pointValueBO.setHasLatestValue(true);
        return pointValueBO;
    }

    /**
     * Get data storage service
     *
     * @return RepositoryService
     */
    private RepositoryService getFirstRepositoryService() {
        List<RepositoryService> repositoryServices = RepositoryStrategyFactory.get();
        if (!repositoryServices.isEmpty() && repositoryServices.size() > 1) {
            throw new RepositoryException(
                    "Save point values to repository error: There are multiple repository, only one is supported.");
        }

        Optional<RepositoryService> first = repositoryServices.stream().findFirst();
        if (first.isEmpty()) {
            throw new RepositoryException(
                    "Save point values to repository error: Please configure at least one repository.");
        }

        return first.get();
    }

}
