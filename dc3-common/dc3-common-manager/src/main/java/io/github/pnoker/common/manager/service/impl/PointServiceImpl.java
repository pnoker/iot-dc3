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
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.manager.dal.PointAttributeConfigManager;
import io.github.pnoker.common.manager.dal.PointManager;
import io.github.pnoker.common.manager.entity.bo.DeviceByPointBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.PointConfigByDeviceBO;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.PointBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.model.PointAttributeConfigDO;
import io.github.pnoker.common.manager.entity.model.PointDO;
import io.github.pnoker.common.manager.entity.query.PointQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.mapper.PointMapper;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business service implementation for point operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final PointBuilder pointBuilder;

    private final PointManager pointManager;

    private final PointMapper pointMapper;

    private final MetadataEventPublisher metadataEventPublisher;

    private final ProfileService profileService;

    private final PointAttributeConfigManager pointAttributeConfigManager;

    private final DeviceMapper deviceMapper;

    private final DriverService driverService;

    @Override
    @Transactional
    public void add(PointBO entityBO) {
        validateTenantRelations(entityBO);
        entityBO.setPointCode(null);
        checkDuplicate(entityBO, false, true);

        PointDO entityDO = pointBuilder.buildDOByBO(entityBO);
        if (!pointManager.save(entityDO)) {
            throw new AddException("Failed to create point");
        }

        //
        List<Long> deviceIds = listDeviceIdsByProfileId(entityDO.getProfileId());
        metadataEventPublisher.publishEvent(
                new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.POINT, MetadataOperateTypeEnum.ADD,
                        driverServiceNamesByDeviceIds(deviceIds)));
        publishDeviceUpdateEvents(deviceIds);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PointDO entityDO = getDOById(id, true);
        List<Long> deviceIds = listDeviceIdsByProfileId(entityDO.getProfileId());
        Set<String> targetServices = driverServiceNamesByDeviceIds(deviceIds);

        if (!pointManager.removeById(id)) {
            throw new DeleteException("Failed to remove ");
        }

        //
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.POINT,
                MetadataOperateTypeEnum.DELETE, targetServices);
        metadataEventPublisher.publishEvent(metadataEvent);
        publishDeviceUpdateEvents(deviceIds);
    }

    @Override
    @Transactional
    public void update(PointBO entityBO) {
        PointDO current = getDOById(entityBO.getId(), true);
        List<Long> oldDeviceIds = listDeviceIdsByProfileId(current.getProfileId());
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        entityBO.setPointCode(current.getPointCode());
        validateTenantRelations(entityBO);

        checkDuplicate(entityBO, true, true);

        PointDO entityDO = pointBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!pointManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update point");
        }

        //
        if (Objects.equals(current.getProfileId(), entityDO.getProfileId())) {
            MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.POINT,
                    MetadataOperateTypeEnum.UPDATE, driverServiceNamesByDeviceIds(oldDeviceIds));
            metadataEventPublisher.publishEvent(metadataEvent);
        } else {
            List<Long> newDeviceIds = listDeviceIdsByProfileId(entityDO.getProfileId());
            metadataEventPublisher.publishEvent(new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.POINT,
                    MetadataOperateTypeEnum.DELETE, driverServiceNamesByDeviceIds(oldDeviceIds)));
            metadataEventPublisher.publishEvent(new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.POINT,
                    MetadataOperateTypeEnum.ADD, driverServiceNamesByDeviceIds(newDeviceIds)));
            publishDeviceUpdateEvents(oldDeviceIds);
            publishDeviceUpdateEvents(newDeviceIds);
        }
    }

    @Override
    public PointBO getById(Long id) {
        PointDO entityDO = getDOById(id, true);
        return pointBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<PointBO> listByIds(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<PointDO> entityDOList = pointManager.listByIds(ids);
        return pointBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<PointBO> listByDeviceId(Long deviceId, Long tenantId) {
        DeviceDO deviceDO = deviceMapper.selectById(deviceId);
        if (Objects.isNull(deviceDO) || Objects.isNull(deviceDO.getProfileId())) {
            return Collections.emptyList();
        }
        return listByProfileId(deviceDO.getProfileId(), deviceDO.getTenantId())
                .stream()
                .filter(point -> Objects.equals(deviceDO.getTenantId(), point.getTenantId()))
                .toList();
    }

    @Override
    public List<PointBO> listByProfileId(Long profileId, Long tenantId) {
        LambdaQueryChainWrapper<PointDO> wrapper = pointManager.lambdaQuery().eq(PointDO::getProfileId, profileId);
        if (Objects.nonNull(tenantId)) {
            wrapper.eq(PointDO::getTenantId, tenantId);
        }
        List<PointDO> entityDOList = wrapper.list();
        return pointBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<PointBO> selectByProfileIds(List<Long> profileIds) {
        if (CollectionUtils.isEmpty(profileIds)) {
            return Collections.emptyList();
        }
        LambdaQueryChainWrapper<PointDO> wrapper = pointManager.lambdaQuery().in(PointDO::getProfileId, profileIds);
        List<PointDO> entityDOList = wrapper.list();
        return pointBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public Page<PointBO> list(PointQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<PointDO> entityPageDO = pointMapper.selectPageWithDevice(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery), entityQuery.getDeviceId());
        return pointBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public Map<Long, String> unit(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        List<PointDO> pointDOList = pointManager.listByIds(ids);
        return pointDOList.stream().collect(Collectors.toMap(PointDO::getId, PointDO::getUnit));
    }

    @Override
    public DeviceByPointBO getPointStatisticsWithDevice(Long pointId) {
        PointBO pointBO = getById(pointId);
        Set<Long> deviceIds = new HashSet<>();

        listDeviceIdsByProfileId(pointBO.getProfileId()).forEach(deviceId -> {
            List<PointAttributeConfigDO> dos = listByDeviceIdAndPointId(deviceId, pointId);
            if (!dos.isEmpty()) {
                deviceIds.add(deviceId);
            }
        });
        DeviceByPointBO deviceByPointBO = new DeviceByPointBO();
        if (CollectionUtils.isNotEmpty(deviceIds)) {
            List<DeviceDO> deviceDOList = deviceMapper
                    .selectList(new LambdaQueryWrapper<DeviceDO>().in(DeviceDO::getId, deviceIds));
            deviceByPointBO.setDevices(deviceDOList);
            deviceByPointBO.setCount((long) deviceDOList.size());
        } else {
            deviceByPointBO.setDevices(Collections.emptyList());
            deviceByPointBO.setCount(0L);
        }
        return deviceByPointBO;
    }

    @Override
    public Long getPointByDeviceId(Long deviceId) {
        DeviceDO deviceDO = deviceMapper.selectById(deviceId);
        if (Objects.isNull(deviceDO) || Objects.isNull(deviceDO.getProfileId())) {
            return 0L;
        }
        return pointManager.count(new LambdaQueryWrapper<PointDO>()
                .eq(PointDO::getProfileId, deviceDO.getProfileId()));
    }

    @Override
    public PointConfigByDeviceBO getPointConfigByDeviceId(Long deviceId) {
        PointConfigByDeviceBO pointConfigByDeviceBO = new PointConfigByDeviceBO();
        pointConfigByDeviceBO.setConfigCount(0L);
        DeviceDO deviceDO = deviceMapper.selectById(deviceId);
        if (Objects.isNull(deviceDO) || Objects.isNull(deviceDO.getProfileId())) {
            pointConfigByDeviceBO.setUnConfigCount(0L);
            return pointConfigByDeviceBO;
        }
        List<PointDO> allPoints = pointManager
                .list(new LambdaQueryWrapper<PointDO>().eq(PointDO::getProfileId, deviceDO.getProfileId()));
        if (CollectionUtils.isEmpty(allPoints)) {
            pointConfigByDeviceBO.setUnConfigCount(0L);
            return pointConfigByDeviceBO;
        }
        List<Long> pointIdList = allPoints.stream().map(PointDO::getId).toList();
        List<PointAttributeConfigDO> configList = pointAttributeConfigManager
                .list(new LambdaQueryWrapper<PointAttributeConfigDO>()
                        .eq(PointAttributeConfigDO::getDeviceId, deviceId));
        Set<Long> configuredPointIds;
        if (CollectionUtils.isNotEmpty(configList)) {
            configuredPointIds = configList.stream()
                    .map(PointAttributeConfigDO::getPointId)
                    .filter(pointIdList::contains)
                    .collect(Collectors.toSet());
        } else {
            configuredPointIds = new HashSet<>();
        }
        long configCount = configuredPointIds.size();
        pointConfigByDeviceBO.setConfigCount(configCount);
        pointConfigByDeviceBO.setUnConfigCount(pointIdList.size() - configCount);
        if (!configuredPointIds.isEmpty()) {
            List<PointDO> configuredPoints = allPoints.stream()
                    .filter(p -> configuredPointIds.contains(p.getId()))
                    .toList();
            pointConfigByDeviceBO.setPoints(configuredPoints);
        }
        return pointConfigByDeviceBO;
    }

    /**
     * @param entityQuery {@link PointQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<PointDO> fuzzyQuery(PointQuery entityQuery) {
        QueryWrapper<PointDO> wrapper = Wrappers.query();
        wrapper.eq("dp.deleted", 0);
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getPointName()), "dp.point_name", entityQuery.getPointName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getPointCode()), "dp.point_code", entityQuery.getPointCode());
        wrapper.eq(Objects.nonNull(entityQuery.getPointTypeFlag()), "dp.point_type_flag",
                Objects.isNull(entityQuery.getPointTypeFlag()) ? null : entityQuery.getPointTypeFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getRwFlag()), "dp.rw_flag",
                Objects.isNull(entityQuery.getRwFlag()) ? null : entityQuery.getRwFlag().getIndex());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getProfileId()), "dp.profile_id", entityQuery.getProfileId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), "dp.enable_flag",
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), "dp.tenant_id", entityQuery.getTenantId());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), "dp.version", entityQuery.getVersion());
        wrapper.exists(FieldUtil.isValidIdField(entityQuery.getGroupId()),
                "select 1 from dc3_group_bind dgb where dgb.deleted = 0 "
                        + "and dgb.tenant_id = dp.tenant_id "
                        + "and dgb.entity_type_flag = {0} "
                        + "and dgb.entity_id = dp.id "
                        + "and dgb.group_id = {1}",
                EntityTypeFlagEnum.POINT.getIndex(), entityQuery.getGroupId());
        wrapper.exists(FieldUtil.isValidIdField(entityQuery.getLabelId()),
                "select 1 from dc3_label_bind dlb where dlb.deleted = 0 "
                        + "and dlb.tenant_id = dp.tenant_id "
                        + "and dlb.entity_type_flag = {0} "
                        + "and dlb.entity_id = dp.id "
                        + "and dlb.label_id = {1}",
                EntityTypeFlagEnum.POINT.getIndex(), entityQuery.getLabelId());
        return wrapper.lambda();
    }

    /**
     * @param entityBO       {@link PointBO}
     * @param isUpdate
     * @param throwException
     * @return
     */
    private boolean checkDuplicate(PointBO entityBO, boolean isUpdate, boolean throwException) {
        boolean hasName = StringUtils.isNotEmpty(entityBO.getPointName());
        boolean hasCode = StringUtils.isNotEmpty(entityBO.getPointCode());
        if (!hasName && !hasCode) {
            return false;
        }
        LambdaQueryWrapper<PointDO> wrapper = Wrappers.<PointDO>query().lambda();
        wrapper.eq(PointDO::getProfileId, entityBO.getProfileId());
        wrapper.eq(PointDO::getTenantId, entityBO.getTenantId());
        wrapper.ne(isUpdate && Objects.nonNull(entityBO.getId()), PointDO::getId, entityBO.getId());
        wrapper.and(query -> {
            if (hasName) {
                query.eq(PointDO::getPointName, entityBO.getPointName());
            }
            if (hasCode) {
                if (hasName) {
                    query.or();
                }
                query.eq(PointDO::getPointCode, entityBO.getPointCode());
            }
        });
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        PointDO one = pointManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Point has been duplicated");
        }
        return duplicate;
    }

    private void validateTenantRelations(PointBO entityBO) {
        ProfileBO profileBO = profileService.getById(entityBO.getProfileId());
        if (Objects.isNull(profileBO) || !Objects.equals(entityBO.getTenantId(), profileBO.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
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

        DriverBO driverBO = driverService.listByDeviceId(deviceId, null);
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

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link PointDO}
     */
    private PointDO getDOById(Long id, boolean throwException) {
        PointDO entityDO = pointManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Point does not exist");
        }
        return entityDO;
    }

    /**
     * Device ID Point ID
     *
     * @param deviceId Device ID
     * @param pointId  Point ID
     * @return PointConfig Array
     */
    private List<PointAttributeConfigDO> listByDeviceIdAndPointId(Long deviceId, Long pointId) {
        LambdaQueryChainWrapper<PointAttributeConfigDO> wrapper = pointAttributeConfigManager.lambdaQuery()
                .eq(PointAttributeConfigDO::getDeviceId, deviceId)
                .eq(PointAttributeConfigDO::getPointId, pointId);
        List<PointAttributeConfigDO> entityDO = wrapper.list();
        return entityDO;
    }

}
