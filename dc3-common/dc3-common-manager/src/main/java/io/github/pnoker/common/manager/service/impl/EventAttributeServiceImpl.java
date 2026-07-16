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
import io.github.pnoker.common.manager.dal.EventAttributeManager;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.builder.EventAttributeBuilder;
import io.github.pnoker.common.manager.entity.model.EventAttributeDO;
import io.github.pnoker.common.manager.entity.query.EventAttributeQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.EventAttributeService;
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
 * Business service implementation for event attribute operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventAttributeServiceImpl implements EventAttributeService {

    private final EventAttributeBuilder eventAttributeBuilder;

    private final EventAttributeManager eventAttributeManager;

    private final DriverService driverService;

    private final MetadataEventPublisher metadataEventPublisher;

    @Override
    public void add(EventAttributeBO entityBO) {
        validateTenantRelations(entityBO);
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create event attribute: event attribute has been duplicated");
        }

        EventAttributeDO entityDO = eventAttributeBuilder.buildDOByBO(entityBO);
        if (!eventAttributeManager.save(entityDO)) {
            throw new AddException("Failed to create event attribute");
        }
        publishDriverMetadataEvent(entityDO.getDriverId());
    }

    @Override
    public void delete(Long id) {
        EventAttributeDO entityDO = getDOById(id, true);

        if (!eventAttributeManager.removeById(id)) {
            throw new DeleteException("Failed to remove event attribute");
        }
        publishDriverMetadataEvent(entityDO.getDriverId());
    }

    @Override
    public void update(EventAttributeBO entityBO) {
        EventAttributeDO current = getDOById(entityBO.getId(), true);
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException("Failed to update event attribute: event attribute has been duplicated");
        }

        EventAttributeDO entityDO = eventAttributeBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!eventAttributeManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update event attribute");
        }
        publishDriverMetadataEvent(entityDO.getDriverId());
    }

    @Override
    public EventAttributeBO getById(Long id) {
        EventAttributeDO entityDO = getDOById(id, true);
        return eventAttributeBuilder.buildBOByDO(entityDO);
    }

    @Override
    public EventAttributeBO getByNameAndDriverId(String name, Long driverId) {
        LambdaQueryChainWrapper<EventAttributeDO> wrapper = eventAttributeManager.lambdaQuery()
                .eq(EventAttributeDO::getAttributeCode, name)
                .eq(EventAttributeDO::getDriverId, driverId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        EventAttributeDO entityDO = wrapper.one();
        return eventAttributeBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<EventAttributeBO> listByDriverId(Long driverId) {
        LambdaQueryChainWrapper<EventAttributeDO> wrapper = eventAttributeManager.lambdaQuery()
                .eq(EventAttributeDO::getDriverId, driverId);
        List<EventAttributeDO> entityDO = wrapper.list();
        return eventAttributeBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public void saveBatch(List<EventAttributeBO> entityBOList) {
        if (Objects.isNull(entityBOList) || entityBOList.isEmpty()) {
            return;
        }
        entityBOList.forEach(this::validateTenantRelations);
        List<EventAttributeDO> doList = entityBOList.stream().map(eventAttributeBuilder::buildDOByBO).toList();
        if (!eventAttributeManager.saveBatch(doList)) {
            throw new AddException("Failed to batch create event attributes");
        }
        entityBOList.stream().map(EventAttributeBO::getDriverId).distinct().forEach(this::publishDriverMetadataEvent);
    }

    @Override
    public void updateBatch(List<EventAttributeBO> entityBOList) {
        if (Objects.isNull(entityBOList) || entityBOList.isEmpty()) {
            return;
        }
        entityBOList.forEach(this::validateTenantRelations);
        List<EventAttributeDO> doList = entityBOList.stream().map(bo -> {
            EventAttributeDO entityDO = eventAttributeBuilder.buildDOByBO(bo);
            entityDO.setOperateTime(null);
            return entityDO;
        }).toList();
        if (!eventAttributeManager.updateBatchById(doList)) {
            throw new UpdateException("Failed to batch update event attributes");
        }
        entityBOList.stream().map(EventAttributeBO::getDriverId).distinct().forEach(this::publishDriverMetadataEvent);
    }

    @Override
    public void removeByIds(Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return;
        }
        List<EventAttributeDO> entityDOList = eventAttributeManager.listByIds(ids);
        if (!eventAttributeManager.removeByIds(ids)) {
            throw new DeleteException("Failed to batch remove event attributes");
        }
        entityDOList.stream().map(EventAttributeDO::getDriverId).distinct().forEach(this::publishDriverMetadataEvent);
    }

    @Override
    public Page<EventAttributeBO> list(EventAttributeQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<EventAttributeDO> entityPageDO = eventAttributeManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return eventAttributeBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for event attribute search.
     *
     * @param entityQuery {@link EventAttributeQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link EventAttributeDO}
     */
    private LambdaQueryWrapper<EventAttributeDO> fuzzyQuery(EventAttributeQuery entityQuery) {
        LambdaQueryWrapper<EventAttributeDO> wrapper = Wrappers.<EventAttributeDO>query().lambda();
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getAttributeCode()), EventAttributeDO::getAttributeCode,
                entityQuery.getAttributeCode());
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getAttributeName()), EventAttributeDO::getAttributeName,
                entityQuery.getAttributeName());
        wrapper.eq(Objects.nonNull(entityQuery.getAttributeTypeFlag()), EventAttributeDO::getAttributeTypeFlag,
                Objects.isNull(entityQuery.getAttributeTypeFlag()) ? null
                        : entityQuery.getAttributeTypeFlag().getIndex());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDriverId()), EventAttributeDO::getDriverId,
                entityQuery.getDriverId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), EventAttributeDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), EventAttributeDO::getVersion, entityQuery.getVersion());
        return wrapper;
    }

    /**
     * Check whether an event attribute is duplicated by attribute code and driver.
     * Unlike the throwing variant, this only reports the duplicate without raising
     * an exception.
     *
     * @param entityBO {@link EventAttributeBO} to be validated
     * @param isUpdate whether the operation is an update (true) or create (false)
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(EventAttributeBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<EventAttributeDO> wrapper = Wrappers.<EventAttributeDO>query().lambda();
        wrapper.eq(EventAttributeDO::getAttributeCode, entityBO.getAttributeCode());
        wrapper.eq(EventAttributeDO::getDriverId, entityBO.getDriverId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        EventAttributeDO one = eventAttributeManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    /**
     * Validate that the attribute's driver belongs to the same tenant.
     *
     * @param entityBO the attribute to validate
     */
    private void validateTenantRelations(EventAttributeBO entityBO) {
        DriverBO driverBO = driverService.getById(entityBO.getDriverId());
        if (Objects.isNull(driverBO) || !Objects.equals(entityBO.getTenantId(), driverBO.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    /**
     * Publish a driver-update metadata event for the given driver.
     *
     * @param driverId the driver that changed
     */
    private void publishDriverMetadataEvent(Long driverId) {
        DriverBO driverBO = driverService.getById(driverId);
        if (Objects.isNull(driverBO) || StringUtils.isBlank(driverBO.getServiceName())) {
            return;
        }
        metadataEventPublisher.publishEvent(new MetadataEvent(this, driverId, MetadataTypeEnum.DRIVER,
                MetadataOperateTypeEnum.UPDATE, Set.of(driverBO.getServiceName())));
    }

    /**
     * Get event attribute data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link EventAttributeDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private EventAttributeDO getDOById(Long id, boolean throwException) {
        EventAttributeDO entityDO = eventAttributeManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Event attribute does not exist");
        }
        return entityDO;
    }

}
