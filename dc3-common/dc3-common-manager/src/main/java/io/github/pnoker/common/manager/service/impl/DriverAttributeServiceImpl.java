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
import io.github.pnoker.common.manager.dal.DriverAttributeManager;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.builder.DriverAttributeBuilder;
import io.github.pnoker.common.manager.entity.model.DriverAttributeDO;
import io.github.pnoker.common.manager.entity.query.DriverAttributeQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.service.DriverAttributeService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Business service implementation for driver attribute operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverAttributeServiceImpl implements DriverAttributeService {

    private final DriverAttributeBuilder driverAttributeBuilder;

    private final DriverAttributeManager driverAttributeManager;

    private final DriverService driverService;

    private final MetadataEventPublisher metadataEventPublisher;

    @Override
    public void add(DriverAttributeBO entityBO) {
        validateTenantRelations(entityBO);
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create driver attribute: driver attribute has been duplicated");
        }

        DriverAttributeDO entityDO = driverAttributeBuilder.buildDOByBO(entityBO);
        if (!driverAttributeManager.save(entityDO)) {
            throw new AddException("Failed to create driver attribute");
        }
        publishDriverMetadataEvent(entityDO.getDriverId());
    }

    @Override
    public void delete(Long id) {
        DriverAttributeDO entityDO = getDOById(id, true);

        if (!driverAttributeManager.removeById(id)) {
            throw new DeleteException("Failed to remove driver attribute");
        }
        publishDriverMetadataEvent(entityDO.getDriverId());
    }

    @Override
    public void update(DriverAttributeBO entityBO) {
        DriverAttributeDO current = getDOById(entityBO.getId(), true);
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException("Failed to update driver attribute: driver attribute has been duplicated");
        }

        DriverAttributeDO entityDO = driverAttributeBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!driverAttributeManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update driver attribute");
        }
        publishDriverMetadataEvent(entityDO.getDriverId());
    }

    @Override
    public DriverAttributeBO getById(Long id) {
        DriverAttributeDO entityDO = getDOById(id, true);
        return driverAttributeBuilder.buildBOByDO(entityDO);
    }

    @Override
    public DriverAttributeBO getByNameAndDriverId(String name, Long driverId) {
        LambdaQueryChainWrapper<DriverAttributeDO> wrapper = driverAttributeManager.lambdaQuery()
                .eq(DriverAttributeDO::getAttributeCode, name)
                .eq(DriverAttributeDO::getDriverId, driverId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        DriverAttributeDO entityDO = wrapper.one();
        return driverAttributeBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<DriverAttributeBO> listByDriverId(Long driverId) {
        LambdaQueryChainWrapper<DriverAttributeDO> wrapper = driverAttributeManager.lambdaQuery()
                .eq(DriverAttributeDO::getDriverId, driverId);
        List<DriverAttributeDO> entityDO = wrapper.list();
        return driverAttributeBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public void saveBatch(List<DriverAttributeBO> entityBOList) {
        if (Objects.isNull(entityBOList) || entityBOList.isEmpty()) {
            return;
        }
        entityBOList.forEach(this::validateTenantRelations);
        List<DriverAttributeDO> doList = entityBOList.stream().map(driverAttributeBuilder::buildDOByBO).toList();
        if (!driverAttributeManager.saveBatch(doList)) {
            throw new AddException("Failed to batch create driver attributes");
        }
        entityBOList.stream().map(DriverAttributeBO::getDriverId).distinct().forEach(this::publishDriverMetadataEvent);
    }

    @Override
    public void updateBatch(List<DriverAttributeBO> entityBOList) {
        if (Objects.isNull(entityBOList) || entityBOList.isEmpty()) {
            return;
        }
        entityBOList.forEach(this::validateTenantRelations);
        List<DriverAttributeDO> doList = entityBOList.stream().map(bo -> {
            DriverAttributeDO entityDO = driverAttributeBuilder.buildDOByBO(bo);
            entityDO.setOperateTime(null);
            return entityDO;
        }).toList();
        if (!driverAttributeManager.updateBatchById(doList)) {
            throw new UpdateException("Failed to batch update driver attributes");
        }
        entityBOList.stream().map(DriverAttributeBO::getDriverId).distinct().forEach(this::publishDriverMetadataEvent);
    }

    @Override
    public void removeByIds(Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return;
        }
        List<DriverAttributeDO> entityDOList = driverAttributeManager.listByIds(ids);
        if (!driverAttributeManager.removeByIds(ids)) {
            throw new DeleteException("Failed to batch remove driver attributes");
        }
        entityDOList.stream().map(DriverAttributeDO::getDriverId).distinct().forEach(this::publishDriverMetadataEvent);
    }

    @Override
    public Page<DriverAttributeBO> list(DriverAttributeQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<DriverAttributeDO> entityPageDO = driverAttributeManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return driverAttributeBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * @param entityQuery {@link DriverAttributeQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<DriverAttributeDO> fuzzyQuery(DriverAttributeQuery entityQuery) {
        LambdaQueryWrapper<DriverAttributeDO> wrapper = Wrappers.<DriverAttributeDO>query().lambda();
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getAttributeCode()), DriverAttributeDO::getAttributeCode,
                entityQuery.getAttributeCode());
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getAttributeName()), DriverAttributeDO::getAttributeName,
                entityQuery.getAttributeName());
        wrapper.eq(Objects.nonNull(entityQuery.getAttributeTypeFlag()), DriverAttributeDO::getAttributeTypeFlag,
                Objects.isNull(entityQuery.getAttributeTypeFlag()) ? null
                        : entityQuery.getAttributeTypeFlag().getIndex());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDriverId()), DriverAttributeDO::getDriverId,
                entityQuery.getDriverId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), DriverAttributeDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), DriverAttributeDO::getTenantId,
                entityQuery.getTenantId());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), DriverAttributeDO::getVersion, entityQuery.getVersion());
        return wrapper;
    }

    /**
     * @param entityBO {@link DriverAttributeBO}
     * @param isUpdate
     * @return
     */
    private boolean checkDuplicate(DriverAttributeBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<DriverAttributeDO> wrapper = Wrappers.<DriverAttributeDO>query().lambda();
        wrapper.eq(DriverAttributeDO::getAttributeCode, entityBO.getAttributeCode());
        wrapper.eq(DriverAttributeDO::getDriverId, entityBO.getDriverId());
        wrapper.eq(DriverAttributeDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DriverAttributeDO one = driverAttributeManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    private void validateTenantRelations(DriverAttributeBO entityBO) {
        DriverBO driverBO = driverService.getById(entityBO.getDriverId());
        if (Objects.isNull(driverBO) || !Objects.equals(entityBO.getTenantId(), driverBO.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    private void publishDriverMetadataEvent(Long driverId) {
        DriverBO driverBO = driverService.getById(driverId);
        if (Objects.isNull(driverBO) || StringUtils.isBlank(driverBO.getServiceName())) {
            return;
        }
        metadataEventPublisher.publishEvent(new MetadataEvent(this, driverId, MetadataTypeEnum.DRIVER,
                MetadataOperateTypeEnum.UPDATE, Set.of(driverBO.getServiceName())));
    }

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link DriverAttributeDO}
     */
    private DriverAttributeDO getDOById(Long id, boolean throwException) {
        DriverAttributeDO entityDO = driverAttributeManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Driver attribute does not exist");
        }
        return entityDO;
    }

}
