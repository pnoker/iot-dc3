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
import io.github.pnoker.common.manager.dal.EventAttributeConfigManager;
import io.github.pnoker.common.manager.dal.EventAttributeManager;
import io.github.pnoker.common.manager.dal.EventManager;
import io.github.pnoker.common.manager.entity.bo.EventAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.builder.EventAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.model.EventAttributeConfigDO;
import io.github.pnoker.common.manager.entity.model.EventAttributeDO;
import io.github.pnoker.common.manager.entity.model.EventDO;
import io.github.pnoker.common.manager.entity.query.EventAttributeConfigQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.service.EventAttributeConfigService;
import io.github.pnoker.common.manager.service.EventService;
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
 * Business service implementation for event attribute configuration.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventAttributeConfigServiceImpl implements EventAttributeConfigService {

    private final EventAttributeConfigBuilder eventAttributeConfigBuilder;

    private final EventAttributeConfigManager eventAttributeConfigManager;

    private final MetadataEventPublisher metadataEventPublisher;

    private final EventService eventService;

    private final DeviceManager deviceManager;

    private final EventManager eventManager;

    private final EventAttributeManager eventAttributeManager;

    @Override
    public void add(EventAttributeConfigBO entityBO) {
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException(
                    "Failed to create event attribute config: event attribute config has been duplicated");
        }

        EventAttributeConfigDO entityDO = eventAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!eventAttributeConfigManager.save(entityDO)) {
            throw new AddException("Failed to create event attribute config");
        }

        //
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public EventAttributeConfigBO innerSave(EventAttributeConfigBO entityBO) {
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException(
                    "Failed to create event attribute config: event attribute config has been duplicated");
        }

        EventAttributeConfigDO entityDO = eventAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!eventAttributeConfigManager.save(entityDO)) {
            throw new AddException("Failed to create event attribute config");
        }

        return eventAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public void delete(Long id) {
        EventAttributeConfigDO entityDO = getDOById(id, true);

        if (!eventAttributeConfigManager.removeById(id)) {
            throw new DeleteException("Failed to remove event attribute config");
        }

        //
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public void update(EventAttributeConfigBO entityBO) {
        EventAttributeConfigDO current = getDOById(entityBO.getId(), true);
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException(
                    "Failed to update event attribute config: event attribute config has been duplicated");
        }

        EventAttributeConfigDO entityDO = eventAttributeConfigBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!eventAttributeConfigManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update event attribute config");
        }

        //
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public EventAttributeConfigBO getById(Long id) {
        EventAttributeConfigDO entityDO = getDOById(id, true);
        return eventAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public EventAttributeConfigBO getByAttributeIdAndDeviceIdAndEventId(Long attributeId, Long deviceId,
                                                                        Long eventId) {
        LambdaQueryChainWrapper<EventAttributeConfigDO> wrapper = eventAttributeConfigManager.lambdaQuery()
                .eq(EventAttributeConfigDO::getAttributeId, attributeId)
                .eq(EventAttributeConfigDO::getDeviceId, deviceId)
                .eq(EventAttributeConfigDO::getEventId, eventId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        EventAttributeConfigDO entityDO = wrapper.one();
        return eventAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<EventAttributeConfigBO> listByAttributeId(Long attributeId) {
        LambdaQueryChainWrapper<EventAttributeConfigDO> wrapper = eventAttributeConfigManager.lambdaQuery()
                .eq(EventAttributeConfigDO::getAttributeId, attributeId);
        List<EventAttributeConfigDO> entityDO = wrapper.list();
        return eventAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public List<EventAttributeConfigBO> listByDeviceId(Long deviceId) {
        List<EventBO> eventBOList = eventService.listByDeviceId(deviceId, null);
        Set<Long> eventIds = eventBOList.stream().map(EventBO::getId).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(eventIds)) {
            return Collections.emptyList();
        }

        LambdaQueryChainWrapper<EventAttributeConfigDO> wrapper = eventAttributeConfigManager.lambdaQuery()
                .eq(EventAttributeConfigDO::getDeviceId, deviceId)
                .in(EventAttributeConfigDO::getEventId, eventIds);
        List<EventAttributeConfigDO> entityDO = wrapper.list();
        return eventAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public List<EventAttributeConfigBO> listByDeviceIdAndEventId(Long deviceId, Long eventId) {
        LambdaQueryChainWrapper<EventAttributeConfigDO> wrapper = eventAttributeConfigManager.lambdaQuery()
                .eq(EventAttributeConfigDO::getDeviceId, deviceId)
                .eq(EventAttributeConfigDO::getEventId, eventId);
        List<EventAttributeConfigDO> entityDO = wrapper.list();
        return eventAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public Page<EventAttributeConfigBO> list(EventAttributeConfigQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<EventAttributeConfigDO> entityPageDO = eventAttributeConfigManager
                .page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return eventAttributeConfigBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * @param entityQuery {@link EventAttributeConfigQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<EventAttributeConfigDO> fuzzyQuery(EventAttributeConfigQuery entityQuery) {
        LambdaQueryWrapper<EventAttributeConfigDO> wrapper = Wrappers.<EventAttributeConfigDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getAttributeId()), EventAttributeConfigDO::getAttributeId,
                entityQuery.getAttributeId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDeviceId()), EventAttributeConfigDO::getDeviceId,
                entityQuery.getDeviceId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getEventId()), EventAttributeConfigDO::getEventId,
                entityQuery.getEventId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), EventAttributeConfigDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), EventAttributeConfigDO::getTenantId,
                entityQuery.getTenantId());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), EventAttributeConfigDO::getVersion,
                entityQuery.getVersion());
        return wrapper;
    }

    /**
     * @param entityBO {@link EventAttributeConfigBO}
     * @param isUpdate
     * @return
     */
    private boolean checkDuplicate(EventAttributeConfigBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<EventAttributeConfigDO> wrapper = Wrappers.<EventAttributeConfigDO>query().lambda();
        wrapper.eq(EventAttributeConfigDO::getAttributeId, entityBO.getAttributeId());
        wrapper.eq(EventAttributeConfigDO::getDeviceId, entityBO.getDeviceId());
        wrapper.eq(EventAttributeConfigDO::getEventId, entityBO.getEventId());
        wrapper.eq(EventAttributeConfigDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        EventAttributeConfigDO one = eventAttributeConfigManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    private void validateTenantRelations(EventAttributeConfigBO entityBO) {
        DeviceDO deviceDO = deviceManager.getById(entityBO.getDeviceId());
        EventDO eventDO = eventManager.getById(entityBO.getEventId());
        EventAttributeDO attributeDO = eventAttributeManager.getById(entityBO.getAttributeId());
        if (Objects.isNull(deviceDO) || Objects.isNull(eventDO) || Objects.isNull(attributeDO)
                || !Objects.equals(entityBO.getTenantId(), deviceDO.getTenantId())
                || !Objects.equals(entityBO.getTenantId(), eventDO.getTenantId())
                || !Objects.equals(entityBO.getTenantId(), attributeDO.getTenantId())
                || !Objects.equals(deviceDO.getDriverId(), attributeDO.getDriverId())
                || !Objects.equals(deviceDO.getProfileId(), eventDO.getProfileId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link EventAttributeConfigDO}
     */
    private EventAttributeConfigDO getDOById(Long id, boolean throwException) {
        EventAttributeConfigDO entityDO = eventAttributeConfigManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Event attribute config does not exist");
        }
        return entityDO;
    }

}
