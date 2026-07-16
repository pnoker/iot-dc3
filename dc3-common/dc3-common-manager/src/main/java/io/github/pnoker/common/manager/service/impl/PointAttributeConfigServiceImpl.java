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

package io.github.pnoker.common.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.manager.dal.DeviceManager;
import io.github.pnoker.common.manager.dal.PointAttributeConfigManager;
import io.github.pnoker.common.manager.dal.PointAttributeManager;
import io.github.pnoker.common.manager.dal.PointManager;
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.builder.PointAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.model.PointAttributeConfigDO;
import io.github.pnoker.common.manager.entity.model.PointAttributeDO;
import io.github.pnoker.common.manager.entity.model.PointDO;
import io.github.pnoker.common.manager.entity.query.PointAttributeConfigQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.service.PointAttributeConfigService;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business service implementation for point attribute configuration.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointAttributeConfigServiceImpl implements PointAttributeConfigService {

    private final PointAttributeConfigBuilder pointAttributeConfigBuilder;

    private final PointAttributeConfigManager pointAttributeConfigManager;

    private final MetadataEventPublisher metadataEventPublisher;

    private final PointService pointService;

    private final DeviceManager deviceManager;

    private final PointManager pointManager;

    private final PointAttributeManager pointAttributeManager;

    @Override
    public void add(PointAttributeConfigBO entityBO) {
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException(
                    "Failed to create point attribute config: point attribute config has been duplicated");
        }

        PointAttributeConfigDO entityDO = pointAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!pointAttributeConfigManager.save(entityDO)) {
            throw new AddException("Failed to create point attribute config");
        }

        // Notify subscribed drivers so they refresh the device metadata cache
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public PointAttributeConfigBO innerSave(PointAttributeConfigBO entityBO) {
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException(
                    "Failed to create point attribute config: point attribute config has been duplicated");
        }

        PointAttributeConfigDO entityDO = pointAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!pointAttributeConfigManager.save(entityDO)) {
            throw new AddException("Failed to create point attribute config");
        }

        return pointAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public void delete(Long id) {
        PointAttributeConfigDO entityDO = getDOById(id, true);

        if (!pointAttributeConfigManager.removeById(id)) {
            throw new DeleteException("Failed to remove point attribute config");
        }

        // Notify subscribed drivers so they refresh the device metadata cache
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public void update(PointAttributeConfigBO entityBO) {
        PointAttributeConfigDO current = getDOById(entityBO.getId(), true);
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException(
                    "Failed to update point attribute config: point attribute config has been duplicated");
        }

        PointAttributeConfigDO entityDO = pointAttributeConfigBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!pointAttributeConfigManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update point attribute config");
        }

        // Notify subscribed drivers so they refresh the device metadata cache
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public PointAttributeConfigBO getById(Long id) {
        PointAttributeConfigDO entityDO = getDOById(id, true);
        return pointAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public PointAttributeConfigBO getByAttributeIdAndDeviceIdAndPointId(Long attributeId, Long deviceId,
                                                                        Long pointId) {
        LambdaQueryChainWrapper<PointAttributeConfigDO> wrapper = pointAttributeConfigManager.lambdaQuery()
                .eq(PointAttributeConfigDO::getAttributeId, attributeId)
                .eq(PointAttributeConfigDO::getDeviceId, deviceId)
                .eq(PointAttributeConfigDO::getPointId, pointId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        PointAttributeConfigDO entityDO = wrapper.one();
        return pointAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<PointAttributeConfigBO> listByAttributeId(Long attributeId) {
        LambdaQueryChainWrapper<PointAttributeConfigDO> wrapper = pointAttributeConfigManager.lambdaQuery()
                .eq(PointAttributeConfigDO::getAttributeId, attributeId);
        List<PointAttributeConfigDO> entityDO = wrapper.list();
        return pointAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public List<PointAttributeConfigBO> listByDeviceId(Long deviceId) {
        List<PointBO> pointBOList = pointService.listByDeviceId(deviceId, null);
        Set<Long> pointIds = pointBOList.stream().map(PointBO::getId).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(pointIds)) {
            return Collections.emptyList();
        }

        LambdaQueryChainWrapper<PointAttributeConfigDO> wrapper = pointAttributeConfigManager.lambdaQuery()
                .eq(PointAttributeConfigDO::getDeviceId, deviceId)
                .in(PointAttributeConfigDO::getPointId, pointIds);
        List<PointAttributeConfigDO> entityDO = wrapper.list();
        return pointAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public List<PointAttributeConfigBO> listByDeviceIdAndPointId(Long deviceId, Long pointId) {
        LambdaQueryChainWrapper<PointAttributeConfigDO> wrapper = pointAttributeConfigManager.lambdaQuery()
                .eq(PointAttributeConfigDO::getDeviceId, deviceId)
                .eq(PointAttributeConfigDO::getPointId, pointId);
        List<PointAttributeConfigDO> entityDO = wrapper.list();
        return pointAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public Page<PointAttributeConfigBO> list(PointAttributeConfigQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<PointAttributeConfigDO> entityPageDO = pointAttributeConfigManager
                .page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return pointAttributeConfigBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for point attribute config search.
     *
     * @param entityQuery {@link PointAttributeConfigQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link PointAttributeConfigDO}
     */
    private LambdaQueryWrapper<PointAttributeConfigDO> fuzzyQuery(PointAttributeConfigQuery entityQuery) {
        LambdaQueryWrapper<PointAttributeConfigDO> wrapper = Wrappers.<PointAttributeConfigDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getAttributeId()), PointAttributeConfigDO::getAttributeId,
                entityQuery.getAttributeId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDeviceId()), PointAttributeConfigDO::getDeviceId,
                entityQuery.getDeviceId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getPointId()), PointAttributeConfigDO::getPointId,
                entityQuery.getPointId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), PointAttributeConfigDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), PointAttributeConfigDO::getVersion,
                entityQuery.getVersion());
        return wrapper;
    }

    /**
     * Check whether a point attribute config is duplicated by attribute, device, and
     * point. Unlike the throwing variant, this only reports the duplicate without
     * raising an exception.
     *
     * @param entityBO {@link PointAttributeConfigBO} to be validated
     * @param isUpdate whether the operation is an update (true) or create (false)
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(PointAttributeConfigBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<PointAttributeConfigDO> wrapper = Wrappers.<PointAttributeConfigDO>query().lambda();
        wrapper.eq(PointAttributeConfigDO::getAttributeId, entityBO.getAttributeId());
        wrapper.eq(PointAttributeConfigDO::getDeviceId, entityBO.getDeviceId());
        wrapper.eq(PointAttributeConfigDO::getPointId, entityBO.getPointId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        PointAttributeConfigDO one = pointAttributeConfigManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    /**
     * Validate that the config's device, point, and attribute all belong to the same
     * tenant, share a driver, and that the device and point share a profile.
     *
     * @param entityBO the config to validate
     */
    private void validateTenantRelations(PointAttributeConfigBO entityBO) {
        DeviceDO deviceDO = deviceManager.getById(entityBO.getDeviceId());
        PointDO pointDO = pointManager.getById(entityBO.getPointId());
        PointAttributeDO attributeDO = pointAttributeManager.getById(entityBO.getAttributeId());
        if (Objects.isNull(deviceDO) || Objects.isNull(pointDO) || Objects.isNull(attributeDO)
                || !Objects.equals(entityBO.getTenantId(), deviceDO.getTenantId())
                || !Objects.equals(entityBO.getTenantId(), pointDO.getTenantId())
                || !Objects.equals(entityBO.getTenantId(), attributeDO.getTenantId())
                || !Objects.equals(deviceDO.getDriverId(), attributeDO.getDriverId())
                || !Objects.equals(deviceDO.getProfileId(), pointDO.getProfileId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    /**
     * Get point attribute config data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link PointAttributeConfigDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private PointAttributeConfigDO getDOById(Long id, boolean throwException) {
        PointAttributeConfigDO entityDO = pointAttributeConfigManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Point attribute config does not exist");
        }
        return entityDO;
    }

}
