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

package io.github.pnoker.common.manager.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
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
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.manager.dal.PointAttributeConfigManager;
import io.github.pnoker.common.manager.dal.PointDataVolumeRunManager;
import io.github.pnoker.common.manager.dal.PointManager;
import io.github.pnoker.common.manager.dal.ProfileBindManager;
import io.github.pnoker.common.manager.entity.bo.*;
import io.github.pnoker.common.manager.entity.builder.PointBuilder;
import io.github.pnoker.common.manager.entity.model.*;
import io.github.pnoker.common.manager.entity.query.PointQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.mapper.DriverMapper;
import io.github.pnoker.common.manager.mapper.PointMapper;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.manager.service.ProfileBindService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PointService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointServiceImpl implements PointService {

    @Resource
    private PointBuilder pointBuilder;

    @Resource
    private PointManager pointManager;

    @Resource
    private ProfileBindManager profileBindManager;

    @Resource
    private PointMapper pointMapper;

    @Resource
    private MetadataEventPublisher metadataEventPublisher;

    @Resource
    private PointDataVolumeRunManager pointDataVolumeRunManager;

    @Resource
    private ProfileBindService profileBindService;

    @Resource
    private PointAttributeConfigManager pointAttributeConfigManager;

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private DriverMapper driverMapper;

    @Override
    public void save(PointBO entityBO) {
        checkDuplicate(entityBO, false, true);

        PointDO entityDO = pointBuilder.buildDOByBO(entityBO);
        if (!pointManager.save(entityDO)) {
            throw new AddException("Failed to create 位号");
        }

        // 通知驱动
        metadataEventPublisher.publishEvent(new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.POINT, MetadataOperateTypeEnum.ADD));
        List<Long> deviceIds = profileBindService.selectDeviceIdsByProfileId(entityDO.getProfileId());
        deviceIds.forEach(entityId -> metadataEventPublisher.publishEvent(new MetadataEvent(this, entityId, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE)));
    }

    @Override
    public void remove(Long id) {
        PointDO entityDO = getDOById(id, true);

        if (!pointManager.removeById(id)) {
            throw new DeleteException("Failed to remove 位号");
        }

        // 通知驱动
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.POINT, MetadataOperateTypeEnum.DELETE);
        metadataEventPublisher.publishEvent(metadataEvent);
        List<Long> deviceIds = profileBindService.selectDeviceIdsByProfileId(entityDO.getProfileId());
        deviceIds.forEach(entityId -> metadataEventPublisher.publishEvent(new MetadataEvent(this, entityId, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE)));
    }

    @Override
    public void update(PointBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        PointDO entityDO = pointBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!pointManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update point");
        }

        // 通知驱动
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public PointBO selectById(Long id) {
        PointDO entityDO = getDOById(id, true);
        return pointBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<PointBO> selectByIds(Set<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<PointDO> entityDOList = pointManager.listByIds(ids);
        return pointBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<PointBO> selectByDeviceId(Long deviceId) {
        LambdaQueryChainWrapper<ProfileBindDO> wrapper = profileBindManager.lambdaQuery().eq(ProfileBindDO::getDeviceId, deviceId);
        List<ProfileBindDO> entityDOList = wrapper.list();
        List<Long> profileIds = entityDOList.stream().map(ProfileBindDO::getProfileId).toList();
        return selectByProfileIds(profileIds);
    }

    @Override
    public List<PointBO> selectByProfileId(Long profileId) {
        LambdaQueryChainWrapper<PointDO> wrapper = pointManager.lambdaQuery().eq(PointDO::getProfileId, profileId);
        List<PointDO> entityDOList = wrapper.list();
        return pointBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<PointBO> selectByProfileIds(List<Long> profileIds) {
        if (CollUtil.isEmpty(profileIds)) {
            return Collections.emptyList();
        }
        LambdaQueryChainWrapper<PointDO> wrapper = pointManager.lambdaQuery().in(PointDO::getProfileId, profileIds);
        List<PointDO> entityDOList = wrapper.list();
        return pointBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public Page<PointBO> selectByPage(PointQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<PointDO> entityPageDO = pointMapper.selectPageWithDevice(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery), entityQuery.getDeviceId());
        return pointBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public Map<Long, String> unit(Set<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        List<PointDO> pointDOList = pointManager.listByIds(ids);
        return pointDOList.stream().collect(Collectors.toMap(PointDO::getId, PointDO::getUnit));
    }

    @Override
    public DeviceByPointBO selectPointStatisticsWithDevice(Long pointId) {
        PointBO pointBO = selectById(pointId);
        Set<Long> deviceIds = new HashSet<>();

        profileBindService.selectDeviceIdsByProfileId(pointBO.getProfileId()).forEach(deviceId -> {
            List<PointAttributeConfigDO> dos = selectByDeviceIdAndPointId(deviceId, pointId);
            if (!dos.isEmpty()) {
                deviceIds.add(deviceId);
            }
        });
        DeviceByPointBO deviceByPointBO = new DeviceByPointBO();
        if (Objects.nonNull(deviceIds)) {
            List<DeviceDO> deviceDOList = deviceMapper.selectList(new LambdaQueryWrapper<DeviceDO>().in(DeviceDO::getId, deviceIds));
            deviceByPointBO.setDevices(deviceDOList);
            deviceByPointBO.setCount(deviceDOList.stream().count());
        } else {
            deviceByPointBO.setCount(0L);
        }
        return deviceByPointBO;
    }

    @Override
    public List<PointDataVolumeRunBO> selectPointStatisticsByDeviceId(Long pointId, Set<Long> deviceIds) {
        List<PointDataVolumeRunBO> list = new ArrayList<>();
        LocalDateTime sevenDaysAgo = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIN).minusDays(7);
        if (Objects.isNull(deviceIds)) {
            return list;
        }
        List<DeviceDO> deviceDOList = deviceMapper.selectList(new LambdaQueryWrapper<DeviceDO>().in(DeviceDO::getId, deviceIds));
        List<Long> zero = Collections.nCopies(7, 0L);
        ArrayList<Long> zeroList = new ArrayList<>(zero);
        deviceDOList.forEach(deviceDO -> {
            LambdaQueryWrapper<PointDataVolumeRunDO> wrapper = Wrappers.<PointDataVolumeRunDO>query().lambda();
            wrapper.eq(PointDataVolumeRunDO::getPointId, pointId).eq(PointDataVolumeRunDO::getDeviceId, deviceDO.getId()).ge(PointDataVolumeRunDO::getCreateTime, sevenDaysAgo);
            PointDataVolumeRunBO pointDataVolumeRunBO = new PointDataVolumeRunBO();
            pointDataVolumeRunBO.setDeviceName(deviceDO.getDeviceName());
            List<PointDataVolumeRunDO> pointDataVolumeRunDOList = pointDataVolumeRunManager.list(wrapper);
            if (Objects.nonNull(pointDataVolumeRunDOList)) {
                for (int i = 0; i < 7; i++) {
                    zeroList.set(i, pointDataVolumeRunDOList.get(i).getTotal());
                }
            }
            pointDataVolumeRunBO.setTotal(zeroList);
            list.add(pointDataVolumeRunBO);
        });
        return list;
    }

    @Override
    public PointDataVolumeRunDO selectPointStatisticsByPointId(Long pointId) {
        QueryWrapper<PointDataVolumeRunDO> wrapper = new QueryWrapper<>();
        wrapper.select("sum(total) as total");
        wrapper.lambda().eq(PointDataVolumeRunDO::getPointId, pointId);
        return pointDataVolumeRunManager.getOne(wrapper);
    }

    @Override
    public Long selectPointByDeviceId(Long deviceId) {
        ProfileBindDO bindDO = profileBindManager.getOne(new LambdaQueryWrapper<ProfileBindDO>().eq(ProfileBindDO::getDeviceId, deviceId));
        Long count = 0L;
        if (Objects.nonNull(bindDO)) {
            List<PointDO> list = pointManager.list(new LambdaQueryWrapper<PointDO>().eq(PointDO::getProfileId, bindDO.getProfileId()));
            count = list.stream().count();
        }
        return count;
    }

    @Override
    public PointConfigByDeviceBO selectPointConfigByDeviceId(Long deviceId) {
        PointConfigByDeviceBO pointConfigByDeviceBO = new PointConfigByDeviceBO();
        ProfileBindDO bindDO = profileBindManager.getOne(new LambdaQueryWrapper<ProfileBindDO>().eq(ProfileBindDO::getDeviceId, deviceId));
        List<Long> configCount = new ArrayList<>();
        List<Long> unConfigCount = new ArrayList<>();
        if (Objects.nonNull(bindDO)) {
            List<PointDO> list = pointManager.list(new LambdaQueryWrapper<PointDO>().eq(PointDO::getProfileId, bindDO.getProfileId()));
            if (Objects.nonNull(list)) {
                List<Long> profileList = list.stream().map(PointDO::getId).toList();
                List<PointAttributeConfigDO> list2 = pointAttributeConfigManager.list(new LambdaQueryWrapper<PointAttributeConfigDO>().eq(PointAttributeConfigDO::getDeviceId, deviceId));
                if (Objects.nonNull(list2)) {
                    List<Long> attrList = new ArrayList<>(list2.stream().map(PointAttributeConfigDO::getPointId).toList());
                    //取交集
                    attrList.retainAll(profileList);
                    configCount.addAll(attrList);
                    unConfigCount.addAll(profileList);
                } else {
                    unConfigCount.addAll(profileList);
                }
            }
        }
        pointConfigByDeviceBO.setConfigCount(0L);
        if (Objects.nonNull(configCount) && Objects.nonNull(unConfigCount)) {
            List<PointDO> list = pointManager.list(new LambdaQueryWrapper<PointDO>().in(PointDO::getId, configCount));
            pointConfigByDeviceBO.setPoints(list);
            pointConfigByDeviceBO.setConfigCount(list.stream().count());
        }
        pointConfigByDeviceBO.setUnConfigCount(unConfigCount.stream().count() - pointConfigByDeviceBO.getConfigCount());
        return pointConfigByDeviceBO;
    }

    @Override
    public List<DeviceDataVolumeRunBO> selectDeviceStatisticsByPointId(Long deviceId, Set<Long> pointIds) {
        List<DeviceDataVolumeRunBO> list = new ArrayList<>();
        LocalDateTime sevenDaysAgo = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIN).minusDays(7);
        if (Objects.isNull(pointIds)) {
            return list;
        }
        List<PointDO> pointDOList = pointManager.list(new LambdaQueryWrapper<PointDO>().in(PointDO::getId, pointIds));
        List<Long> zero = Collections.nCopies(7, 0L);
        ArrayList<Long> zeroList = new ArrayList<>(zero);
        pointDOList.forEach(pointDO -> {
            LambdaQueryWrapper<PointDataVolumeRunDO> wrapper = Wrappers.<PointDataVolumeRunDO>query().lambda();
            wrapper.eq(PointDataVolumeRunDO::getPointId, pointDO.getId()).eq(PointDataVolumeRunDO::getDeviceId, deviceId).ge(PointDataVolumeRunDO::getCreateTime, sevenDaysAgo);
            DeviceDataVolumeRunBO deviceDataVolumeRunBO = new DeviceDataVolumeRunBO();
            deviceDataVolumeRunBO.setPointName(pointDO.getPointName());
            List<PointDataVolumeRunDO> pointDataVolumeRunDOList = pointDataVolumeRunManager.list(wrapper);
            if (Objects.nonNull(pointDataVolumeRunDOList)) {
                for (int i = 0; i < pointDataVolumeRunDOList.size(); i++) {
                    zeroList.set(i, pointDataVolumeRunDOList.get(i).getTotal());
                }
            }
            deviceDataVolumeRunBO.setTotal(zeroList);
            list.add(deviceDataVolumeRunBO);
        });
        return list;
    }

    @Override
    public PointDataVolumeRunDO selectPointDataByDriverId(Long driverId) {
        QueryWrapper<PointDataVolumeRunDO> wrapper = new QueryWrapper<>();
        wrapper.select("sum(total) as total");
        wrapper.lambda().eq(PointDataVolumeRunDO::getDriverId, driverId);
        PointDataVolumeRunDO one = pointDataVolumeRunManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return new PointDataVolumeRunDO();
        }
        return one;
    }

    @Override
    public Long selectPointByDriverId(Long driverId) {
        LambdaQueryWrapper<PointDataVolumeRunDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(PointDataVolumeRunDO::getPointId);
        queryWrapper.eq(PointDataVolumeRunDO::getDriverId, driverId)
                .groupBy(PointDataVolumeRunDO::getPointId);
        List<PointDataVolumeRunDO> pointDataVolumeRunDOList = pointDataVolumeRunManager.list(queryWrapper);
        if (Objects.nonNull(pointDataVolumeRunDOList)) {
            return pointDataVolumeRunDOList.stream().count();
        } else {
            return 0L;
        }
    }

    @Override
    public PointDataStatisticsByDriverIdBO selectPointDataStatisticsByDriverId(Long driverId) {
        PointDataStatisticsByDriverIdBO result = new PointDataStatisticsByDriverIdBO();
        DriverDO driverDO = driverMapper.selectById(driverId);
        if (Objects.isNull(driverDO)) {
            throw new NotFoundException("Driver does not exist");
        }
        result.setDriverName(driverDO.getDriverName());
        List<Long> zero = Collections.nCopies(7, 0L);
        ArrayList<Long> zeroList = new ArrayList<>(zero);
        LocalDateTime sevenDaysAgo = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIN).minusDays(6);
        QueryWrapper<PointDataVolumeRunDO> wrapper = new QueryWrapper<>();
        wrapper.select("sum(total) as total");
        List<PointDataVolumeRunDO> pointDataVolumeRunDOList = pointDataVolumeRunManager.list(wrapper.lambda()
                .eq(PointDataVolumeRunDO::getDriverId, driverId)
                .ge(PointDataVolumeRunDO::getCreateTime, sevenDaysAgo)
                .groupBy(PointDataVolumeRunDO::getCreateTime)
                .orderByDesc(PointDataVolumeRunDO::getCreateTime));
        if (Objects.isNull(pointDataVolumeRunDOList)) {
            result.setTotal(zeroList);
            return result;
        }
        for (int i = 0; i < pointDataVolumeRunDOList.size(); i++) {
            zeroList.set(i, pointDataVolumeRunDOList.get(i).getTotal());
        }
        result.setTotal(zeroList);
        return result;
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link PointQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<PointDO> fuzzyQuery(PointQuery entityQuery) {
        QueryWrapper<PointDO> wrapper = Wrappers.query();
        wrapper.eq("dp.deleted", 0);
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getPointName()), "dp.point_name", entityQuery.getPointName());
        wrapper.eq(Objects.nonNull(entityQuery.getPointCode()), "dp.point_code", entityQuery.getPointCode());
        wrapper.eq(Objects.nonNull(entityQuery.getPointTypeFlag()), "dp.point_type_flag", entityQuery.getPointTypeFlag());
        wrapper.eq(Objects.nonNull(entityQuery.getRwFlag()), "dp.rw_flag", entityQuery.getRwFlag());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getProfileId()), "dp.profile_id", entityQuery.getProfileId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), "dp.enable_flag", entityQuery.getEnableFlag());
        wrapper.eq("dp.tenant_id", entityQuery.getTenantId());
        return wrapper.lambda();
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link PointBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(PointBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<PointDO> wrapper = Wrappers.<PointDO>query().lambda();
        wrapper.eq(PointDO::getPointName, entityBO.getPointName());
        wrapper.eq(PointDO::getPointCode, entityBO.getPointCode());
        wrapper.eq(PointDO::getProfileId, entityBO.getProfileId());
        wrapper.eq(PointDO::getTenantId, entityBO.getTenantId());
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

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
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
     * 根据 设备ID 和 位号ID 查询位号是否已配置
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     * @return PointConfig Array
     */
    private List<PointAttributeConfigDO> selectByDeviceIdAndPointId(Long deviceId, Long pointId) {
        LambdaQueryChainWrapper<PointAttributeConfigDO> wrapper = pointAttributeConfigManager.lambdaQuery()
                .eq(PointAttributeConfigDO::getDeviceId, deviceId)
                .eq(PointAttributeConfigDO::getPointId, pointId);
        List<PointAttributeConfigDO> entityDO = wrapper.list();
        return entityDO;
    }

}
