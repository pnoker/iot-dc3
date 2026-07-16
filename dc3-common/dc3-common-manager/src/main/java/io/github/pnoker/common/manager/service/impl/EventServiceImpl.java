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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import io.github.pnoker.common.manager.dal.EventManager;
import io.github.pnoker.common.manager.dal.EventParamManager;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.EventBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.model.EventDO;
import io.github.pnoker.common.manager.entity.model.EventParamDO;
import io.github.pnoker.common.manager.entity.query.EventQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.mapper.EventMapper;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.EventService;
import io.github.pnoker.common.manager.service.ProfileService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business service implementation for event operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventBuilder eventBuilder;

    private final EventManager eventManager;

    private final EventMapper eventMapper;

    private final EventParamManager eventParamManager;

    private final MetadataEventPublisher metadataEventPublisher;

    private final ProfileService profileService;

    private final DeviceMapper deviceMapper;

    private final DriverService driverService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(EventBO entityBO) {
        validateTenantRelations(entityBO);
        entityBO.setEventCode(null);
        checkDuplicate(entityBO, false, true);

        EventDO entityDO = eventBuilder.buildDOByBO(entityBO);
        if (!eventManager.save(entityDO)) {
            throw new AddException("Failed to create event");
        }
        entityBO.setId(entityDO.getId());

        List<Long> deviceIds = listDeviceIdsByProfileId(entityDO.getProfileId());
        metadataEventPublisher.publishEvent(
                new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.EVENT, MetadataOperateTypeEnum.ADD,
                        driverServiceNamesByDeviceIds(deviceIds)));
        publishDeviceUpdateEvents(deviceIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        EventDO entityDO = getDOById(id, true);
        List<Long> deviceIds = listDeviceIdsByProfileId(entityDO.getProfileId());
        Set<String> targetServices = driverServiceNamesByDeviceIds(deviceIds);

        cascadeDeleteParams(id);

        if (!eventManager.removeById(id)) {
            throw new DeleteException("Failed to remove event");
        }

        metadataEventPublisher.publishEvent(
                new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.EVENT,
                        MetadataOperateTypeEnum.DELETE, targetServices));
        publishDeviceUpdateEvents(deviceIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(EventBO entityBO) {
        EventDO current = getDOById(entityBO.getId(), true);
        List<Long> oldDeviceIds = listDeviceIdsByProfileId(current.getProfileId());
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        entityBO.setEventCode(current.getEventCode());
        validateTenantRelations(entityBO);

        checkDuplicate(entityBO, true, true);

        EventDO entityDO = eventBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!eventManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update event");
        }

        if (Objects.equals(current.getProfileId(), entityDO.getProfileId())) {
            metadataEventPublisher.publishEvent(
                    new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.EVENT,
                            MetadataOperateTypeEnum.UPDATE, driverServiceNamesByDeviceIds(oldDeviceIds)));
        } else {
            List<Long> newDeviceIds = listDeviceIdsByProfileId(entityDO.getProfileId());
            metadataEventPublisher.publishEvent(
                    new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.EVENT,
                            MetadataOperateTypeEnum.DELETE, driverServiceNamesByDeviceIds(oldDeviceIds)));
            metadataEventPublisher.publishEvent(
                    new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.EVENT,
                            MetadataOperateTypeEnum.ADD, driverServiceNamesByDeviceIds(newDeviceIds)));
            publishDeviceUpdateEvents(oldDeviceIds);
            publishDeviceUpdateEvents(newDeviceIds);
        }
    }

    @Override
    public EventBO getById(Long id) {
        EventDO entityDO = getDOById(id, true);
        return eventBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<EventBO> listByIds(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<EventDO> entityDOList = eventManager.listByIds(ids);
        return eventBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<EventBO> listByDeviceId(Long deviceId, Long tenantId) {
        DeviceDO deviceDO = deviceMapper.selectById(deviceId);
        if (Objects.isNull(deviceDO) || Objects.isNull(deviceDO.getProfileId())) {
            return Collections.emptyList();
        }
        return listByProfileId(deviceDO.getProfileId(), deviceDO.getTenantId());
    }

    @Override
    public List<EventBO> listByProfileId(Long profileId, Long tenantId) {
        LambdaQueryChainWrapper<EventDO> wrapper = eventManager.lambdaQuery().eq(EventDO::getProfileId, profileId);
        List<EventDO> entityDOList = wrapper.list();
        return eventBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<EventBO> listByProfileIds(List<Long> profileIds) {
        if (CollectionUtils.isEmpty(profileIds)) {
            return Collections.emptyList();
        }
        LambdaQueryChainWrapper<EventDO> wrapper = eventManager.lambdaQuery().in(EventDO::getProfileId, profileIds);
        List<EventDO> entityDOList = wrapper.list();
        return eventBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public Page<EventBO> list(EventQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<EventDO> entityPageDO = eventMapper.selectPageWithDevice(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery), entityQuery.getDeviceId());
        return eventBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for event search.
     *
     * @param entityQuery {@link EventQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link EventDO}
     */
    private LambdaQueryWrapper<EventDO> fuzzyQuery(EventQuery entityQuery) {
        QueryWrapper<EventDO> wrapper = Wrappers.query();
        wrapper.eq("de.deleted", 0);
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getEventName()), "de.event_name", entityQuery.getEventName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getEventCode()), "de.event_code", entityQuery.getEventCode());
        wrapper.eq(Objects.nonNull(entityQuery.getEventType()), "de.event_type_flag",
                Objects.isNull(entityQuery.getEventType()) ? null : entityQuery.getEventType().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getEventLevel()), "de.event_level_flag",
                Objects.isNull(entityQuery.getEventLevel()) ? null : entityQuery.getEventLevel().getIndex());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getProfileId()), "de.profile_id", entityQuery.getProfileId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), "de.enable_flag",
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), "de.version", entityQuery.getVersion());
        return wrapper.lambda();
    }

    /**
     * Check whether an event is duplicated within its profile by event name or code.
     * Returns {@code false} when neither name nor code is supplied.
     *
     * @param entityBO       {@link EventBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(EventBO entityBO, boolean isUpdate, boolean throwException) {
        boolean hasName = StringUtils.isNotEmpty(entityBO.getEventName());
        boolean hasCode = StringUtils.isNotEmpty(entityBO.getEventCode());
        if (!hasName && !hasCode) {
            return false;
        }
        LambdaQueryWrapper<EventDO> wrapper = Wrappers.<EventDO>query().lambda();
        wrapper.eq(EventDO::getProfileId, entityBO.getProfileId());
        wrapper.ne(isUpdate && Objects.nonNull(entityBO.getId()), EventDO::getId, entityBO.getId());
        wrapper.and(query -> {
            if (hasName) {
                query.eq(EventDO::getEventName, entityBO.getEventName());
            }
            if (hasCode) {
                if (hasName) {
                    query.or();
                }
                query.eq(EventDO::getEventCode, entityBO.getEventCode());
            }
        });
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        EventDO one = eventManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Event has been duplicated");
        }
        return duplicate;
    }

    /**
     * Validate that the event's profile belongs to the same tenant.
     *
     * @param entityBO the event to validate
     */
    private void validateTenantRelations(EventBO entityBO) {
        ProfileBO profileBO = profileService.getById(entityBO.getProfileId());
        if (Objects.isNull(profileBO) || !Objects.equals(entityBO.getTenantId(), profileBO.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    /**
     * Delete all params belonging to an event (cascade on event delete).
     *
     * @param eventId the event whose params to delete
     */
    private void cascadeDeleteParams(Long eventId) {
        LambdaQueryChainWrapper<EventParamDO> wrapper = eventParamManager.lambdaQuery()
                .eq(EventParamDO::getEventId, eventId);
        List<EventParamDO> params = wrapper.list();
        if (CollectionUtils.isNotEmpty(params)) {
            eventParamManager.removeByIds(params.stream().map(EventParamDO::getId).toList());
        }
    }

    /**
     * Publish a device-update metadata event to each affected device's driver.
     *
     * @param deviceIds the devices that changed
     */
    private void publishDeviceUpdateEvents(Collection<Long> deviceIds) {
        if (CollectionUtils.isEmpty(deviceIds)) {
            return;
        }
        deviceIds.forEach(deviceId -> metadataEventPublisher.publishEvent(
                new MetadataEvent(this, deviceId, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE,
                        driverServiceNamesByDeviceId(deviceId))));
    }

    /**
     * Resolve the union of driver service names serving the given devices.
     *
     * @param deviceIds the devices to look up drivers for
     * @return the set of driver service names
     */
    private Set<String> driverServiceNamesByDeviceIds(Collection<Long> deviceIds) {
        if (CollectionUtils.isEmpty(deviceIds)) {
            return Collections.emptySet();
        }
        return deviceIds.stream()
                .map(this::driverServiceNamesByDeviceId)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Resolve the driver service name serving a single device.
     *
     * @param deviceId the device to look up
     * @return the driver service name, or empty when none
     */
    private Set<String> driverServiceNamesByDeviceId(Long deviceId) {
        if (Objects.isNull(deviceId)) {
            return Collections.emptySet();
        }
        DriverBO driverBO = driverService.getByDeviceId(deviceId, null);
        if (Objects.isNull(driverBO) || StringUtils.isBlank(driverBO.getServiceName())) {
            return Collections.emptySet();
        }
        return Set.of(driverBO.getServiceName());
    }

    /**
     * List the device ids sharing a profile.
     *
     * @param profileId the profile id
     * @return the device ids
     */
    private List<Long> listDeviceIdsByProfileId(Long profileId) {
        if (Objects.isNull(profileId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getProfileId, profileId);
        return deviceMapper.selectList(wrapper).stream().map(DeviceDO::getId).toList();
    }

    /**
     * Get event data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link EventDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private EventDO getDOById(Long id, boolean throwException) {
        EventDO entityDO = eventManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Event does not exist");
        }
        return entityDO;
    }

}
