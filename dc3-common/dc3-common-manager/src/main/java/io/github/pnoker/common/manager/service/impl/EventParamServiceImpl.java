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
import io.github.pnoker.common.manager.dal.EventParamManager;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.bo.EventParamBO;
import io.github.pnoker.common.manager.entity.builder.EventParamBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.model.EventParamDO;
import io.github.pnoker.common.manager.entity.query.EventParamQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.EventParamService;
import io.github.pnoker.common.manager.service.EventService;
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
 * Business service implementation for event param operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventParamServiceImpl implements EventParamService {

    private final EventParamBuilder eventParamBuilder;

    private final EventParamManager eventParamManager;

    private final EventService eventService;

    private final MetadataEventPublisher metadataEventPublisher;

    private final DeviceMapper deviceMapper;

    private final DriverService driverService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(EventParamBO entityBO) {
        validateTenantRelations(entityBO);
        checkDuplicate(entityBO, false, true);

        EventParamDO entityDO = eventParamBuilder.buildDOByBO(entityBO);
        if (!eventParamManager.save(entityDO)) {
            throw new AddException("Failed to create event param");
        }
        publishEventUpdate(entityBO.getEventId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        EventParamDO entityDO = getDOById(id, true);
        if (!eventParamManager.removeById(id)) {
            throw new DeleteException("Failed to remove event param");
        }
        publishEventUpdate(entityDO.getEventId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(EventParamBO entityBO) {
        EventParamDO current = getDOById(entityBO.getId(), true);
        Long oldEventId = current.getEventId();
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        validateTenantRelations(entityBO);

        checkDuplicate(entityBO, true, true);

        EventParamDO entityDO = eventParamBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!eventParamManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update event param");
        }
        publishEventUpdate(oldEventId);
        if (!Objects.equals(oldEventId, entityBO.getEventId())) {
            publishEventUpdate(entityBO.getEventId());
        }
    }

    @Override
    public EventParamBO getById(Long id) {
        EventParamDO entityDO = getDOById(id, true);
        return eventParamBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<EventParamBO> listByIds(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<EventParamDO> entityDOList = eventParamManager.listByIds(ids);
        return eventParamBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<EventParamBO> listByEventId(Long eventId) {
        LambdaQueryChainWrapper<EventParamDO> wrapper = eventParamManager.lambdaQuery()
                .eq(EventParamDO::getEventId, eventId);
        List<EventParamDO> entityDOList = wrapper.list();
        return eventParamBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public Page<EventParamBO> list(EventParamQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<EventParamDO> entityPageDO = eventParamManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return eventParamBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<EventParamDO> fuzzyQuery(EventParamQuery entityQuery) {
        QueryWrapper<EventParamDO> wrapper = Wrappers.query();
        wrapper.eq("deleted", 0);
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getParamName()), "param_name", entityQuery.getParamName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getParamCode()), "param_code", entityQuery.getParamCode());
        wrapper.eq(Objects.nonNull(entityQuery.getParamTypeFlag()), "param_type_flag",
                Objects.isNull(entityQuery.getParamTypeFlag()) ? null : entityQuery.getParamTypeFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getEventId()), "event_id", entityQuery.getEventId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), "enable_flag",
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), "version", entityQuery.getVersion());
        return wrapper.lambda();
    }

    private boolean checkDuplicate(EventParamBO entityBO, boolean isUpdate, boolean throwException) {
        boolean hasName = StringUtils.isNotEmpty(entityBO.getParamName());
        boolean hasCode = StringUtils.isNotEmpty(entityBO.getParamCode());
        if (!hasName && !hasCode) {
            return false;
        }
        LambdaQueryWrapper<EventParamDO> wrapper = Wrappers.<EventParamDO>query().lambda();
        wrapper.eq(EventParamDO::getEventId, entityBO.getEventId());
        wrapper.ne(isUpdate && Objects.nonNull(entityBO.getId()), EventParamDO::getId, entityBO.getId());
        wrapper.and(query -> {
            if (hasName) {
                query.eq(EventParamDO::getParamName, entityBO.getParamName());
            }
            if (hasCode) {
                if (hasName) {
                    query.or();
                }
                query.eq(EventParamDO::getParamCode, entityBO.getParamCode());
            }
        });
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        EventParamDO one = eventParamManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Event param has been duplicated");
        }
        return duplicate;
    }

    private void validateTenantRelations(EventParamBO entityBO) {
        EventBO eventBO = eventService.getById(entityBO.getEventId());
        if (Objects.isNull(eventBO) || !Objects.equals(entityBO.getTenantId(), eventBO.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    private void publishEventUpdate(Long eventId) {
        if (Objects.isNull(eventId)) {
            return;
        }
        EventBO eventBO;
        try {
            eventBO = eventService.getById(eventId);
        } catch (NotFoundException ignored) {
            return;
        }
        if (Objects.isNull(eventBO)) {
            return;
        }

        List<Long> deviceIds = listDeviceIdsByProfileId(eventBO.getProfileId());
        metadataEventPublisher.publishEvent(
                new MetadataEvent(this, eventId, MetadataTypeEnum.EVENT, MetadataOperateTypeEnum.UPDATE,
                        driverServiceNamesByDeviceIds(deviceIds)));
        publishDeviceUpdateEvents(deviceIds);
    }

    private void publishDeviceUpdateEvents(Collection<Long> deviceIds) {
        if (CollectionUtils.isEmpty(deviceIds)) {
            return;
        }
        deviceIds.forEach(deviceId -> metadataEventPublisher.publishEvent(
                new MetadataEvent(this, deviceId, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE,
                        driverServiceNamesByDeviceId(deviceId))));
    }

    private Set<String> driverServiceNamesByDeviceIds(Collection<Long> deviceIds) {
        if (CollectionUtils.isEmpty(deviceIds)) {
            return Collections.emptySet();
        }
        return deviceIds.stream()
                .map(this::driverServiceNamesByDeviceId)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

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

    private List<Long> listDeviceIdsByProfileId(Long profileId) {
        if (Objects.isNull(profileId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getProfileId, profileId);
        return deviceMapper.selectList(wrapper).stream().map(DeviceDO::getId).toList();
    }

    private EventParamDO getDOById(Long id, boolean throwException) {
        EventParamDO entityDO = eventParamManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Event param does not exist");
        }
        return entityDO;
    }

}
