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
import io.github.pnoker.common.manager.dal.DriverAttributeConfigManager;
import io.github.pnoker.common.manager.dal.DriverAttributeManager;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.entity.builder.DriverAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.model.DriverAttributeConfigDO;
import io.github.pnoker.common.manager.entity.model.DriverAttributeDO;
import io.github.pnoker.common.manager.entity.query.DriverAttributeConfigQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.service.DriverAttributeConfigService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Business service implementation for driver attribute configuration.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverAttributeConfigServiceImpl implements DriverAttributeConfigService {

    private final DriverAttributeConfigBuilder driverAttributeConfigBuilder;

    private final DriverAttributeConfigManager driverAttributeConfigManager;

    private final MetadataEventPublisher metadataEventPublisher;

    private final DeviceManager deviceManager;

    private final DriverAttributeManager driverAttributeManager;

    @Override
    public void add(DriverAttributeConfigBO entityBO) {
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException(
                    "Failed to create driver attribute config: driver attribute config has been duplicated");
        }

        DriverAttributeConfigDO entityDO = driverAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!driverAttributeConfigManager.save(entityDO)) {
            throw new AddException("Failed to create driver attribute config");
        }

        // Notify subscribed drivers so they refresh the device metadata cache
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public DriverAttributeConfigBO innerSave(DriverAttributeConfigBO entityBO) {
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException(
                    "Failed to create driver attribute config: driver attribute config has been duplicated");
        }

        DriverAttributeConfigDO entityDO = driverAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!driverAttributeConfigManager.save(entityDO)) {
            throw new AddException("Failed to create driver attribute config");
        }

        return driverAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public void delete(Long id) {
        DriverAttributeConfigDO entityDO = getDOById(id, true);

        if (!driverAttributeConfigManager.removeById(id)) {
            throw new DeleteException("Failed to remove driver attribute config");
        }

        // Notify subscribed drivers so they refresh the device metadata cache
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public void update(DriverAttributeConfigBO entityBO) {
        DriverAttributeConfigDO current = getDOById(entityBO.getId(), true);
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException(
                    "Failed to update driver attribute config: driver attribute config has been duplicated");
        }

        DriverAttributeConfigDO entityDO = driverAttributeConfigBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!driverAttributeConfigManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update driver attribute config");
        }

        // Notify subscribed drivers so they refresh the device metadata cache
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public DriverAttributeConfigBO getById(Long id) {
        DriverAttributeConfigDO entityDO = getDOById(id, true);
        return driverAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public DriverAttributeConfigBO selectByAttributeIdAndDeviceId(Long deviceId, Long attributeId) {
        LambdaQueryChainWrapper<DriverAttributeConfigDO> wrapper = driverAttributeConfigManager.lambdaQuery()
                .eq(DriverAttributeConfigDO::getAttributeId, attributeId)
                .eq(DriverAttributeConfigDO::getDeviceId, deviceId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        DriverAttributeConfigDO entityDO = wrapper.one();
        return driverAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<DriverAttributeConfigBO> listByAttributeId(Long attributeId) {
        LambdaQueryChainWrapper<DriverAttributeConfigDO> wrapper = driverAttributeConfigManager.lambdaQuery()
                .eq(DriverAttributeConfigDO::getAttributeId, attributeId);
        List<DriverAttributeConfigDO> entityDO = wrapper.list();
        return driverAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public List<DriverAttributeConfigBO> listByDeviceId(Long deviceId) {
        LambdaQueryChainWrapper<DriverAttributeConfigDO> wrapper = driverAttributeConfigManager.lambdaQuery()
                .eq(DriverAttributeConfigDO::getDeviceId, deviceId);
        List<DriverAttributeConfigDO> entityDO = wrapper.list();
        return driverAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public Page<DriverAttributeConfigBO> list(DriverAttributeConfigQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<DriverAttributeConfigDO> entityPageDO = driverAttributeConfigManager
                .page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return driverAttributeConfigBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for driver attribute config search.
     *
     * @param entityQuery {@link DriverAttributeConfigQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link DriverAttributeConfigDO}
     */
    private LambdaQueryWrapper<DriverAttributeConfigDO> fuzzyQuery(DriverAttributeConfigQuery entityQuery) {
        LambdaQueryWrapper<DriverAttributeConfigDO> wrapper = Wrappers.<DriverAttributeConfigDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getAttributeId()), DriverAttributeConfigDO::getAttributeId,
                entityQuery.getAttributeId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDeviceId()), DriverAttributeConfigDO::getDeviceId,
                entityQuery.getDeviceId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), DriverAttributeConfigDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), DriverAttributeConfigDO::getVersion,
                entityQuery.getVersion());
        return wrapper;
    }

    /**
     * Check whether a driver attribute config is duplicated by attribute and device.
     * Unlike the throwing variant, this only reports the duplicate without raising
     * an exception.
     *
     * @param entityBO {@link DriverAttributeConfigBO} to be validated
     * @param isUpdate whether the operation is an update (true) or create (false)
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(DriverAttributeConfigBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<DriverAttributeConfigDO> wrapper = Wrappers.<DriverAttributeConfigDO>query().lambda();
        wrapper.eq(DriverAttributeConfigDO::getAttributeId, entityBO.getAttributeId());
        wrapper.eq(DriverAttributeConfigDO::getDeviceId, entityBO.getDeviceId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DriverAttributeConfigDO one = driverAttributeConfigManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    /**
     * Validate that the config's device and attribute belong to the same tenant and
     * share a driver.
     *
     * @param entityBO the config to validate
     */
    private void validateTenantRelations(DriverAttributeConfigBO entityBO) {
        DeviceDO deviceDO = deviceManager.getById(entityBO.getDeviceId());
        DriverAttributeDO attributeDO = driverAttributeManager.getById(entityBO.getAttributeId());
        if (Objects.isNull(deviceDO) || Objects.isNull(attributeDO)
                || !Objects.equals(entityBO.getTenantId(), deviceDO.getTenantId())
                || !Objects.equals(entityBO.getTenantId(), attributeDO.getTenantId())
                || !Objects.equals(deviceDO.getDriverId(), attributeDO.getDriverId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    /**
     * Get driver attribute config data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link DriverAttributeConfigDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private DriverAttributeConfigDO getDOById(Long id, boolean throwException) {
        DriverAttributeConfigDO entityDO = driverAttributeConfigManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Driver attribute does not exist");
        }
        return entityDO;
    }

}
