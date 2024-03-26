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

package io.github.pnoker.center.manager.biz.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.center.manager.biz.PointStatisticsService;
import io.github.pnoker.center.manager.dal.DeviceManager;
import io.github.pnoker.center.manager.dal.DriverManager;
import io.github.pnoker.center.manager.dal.PointDataVolumeHistoryManager;
import io.github.pnoker.center.manager.dal.PointDataVolumeRunManager;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
import io.github.pnoker.center.manager.entity.model.DriverDO;
import io.github.pnoker.center.manager.entity.model.PointDataVolumeHistoryDO;
import io.github.pnoker.center.manager.entity.model.PointDataVolumeRunDO;
import io.github.pnoker.center.manager.service.PointService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 点位数据量统计服务实现类
 *
 * @Author fukq
 * create by 2024/3/5 13:55
 * @Version 1.0
 * @date 2024/03/05
 */
@Slf4j
@Service
public class PointStatisticsServiceImpl implements PointStatisticsService {
    @Resource
    private DriverManager driverManager;
    @Resource
    private DeviceManager deviceManager;
    @Resource
    private PointService pointService;
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private PointDataVolumeHistoryManager pointDataVolumeHistoryManager;
    @Resource
    private PointDataVolumeRunManager pointDataVolumeRunManager;

    private LocalDateTime datetime;


    /**
     * 统计点位历史数据
     *
     * @param datetime 日期时间
     */
    @Override
    public void statisticsPointHistory(LocalDateTime datetime) {
        this.datetime = datetime;
        List<DriverDO> driverList = driverManager.list(new QueryWrapper<>());
        driverList.forEach(item -> {
            getDeviceByDriverId(item.getId());
        });

    }


    /**
     * 按驱动程序id获取设备
     *
     * @param driverId 驱动程序id
     */
    private void getDeviceByDriverId(Long driverId) {
        /**获取当前所有设备*/
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDriverId, driverId);
        List<DeviceDO> deviceList = deviceManager.list(wrapper);
        deviceList.forEach(item -> {
            getPointSaveByMongo(driverId, item.getId());
        });
    }

    /**
     * 从mongo获得点位数据保存
     *
     * @param deviceId 设备id
     */
    private void getPointSaveByMongo(long driverId, long deviceId) {
        List<PointBO> pointList = pointService.selectByDeviceId(deviceId);
        pointList.forEach(pointItem -> {
            Long pointId = pointItem.getId();
            LambdaQueryWrapper<PointDataVolumeHistoryDO> wrapper = Wrappers.<PointDataVolumeHistoryDO>query().lambda();
            wrapper.orderByDesc(PointDataVolumeHistoryDO::getCreateTime).eq(PointDataVolumeHistoryDO::getPointId, pointId).last("LIMIT 1");
            PointDataVolumeHistoryDO pointDataVolumeHistoryDO = pointDataVolumeHistoryManager.getOne(wrapper);
            long count;
            if (ObjectUtil.isEmpty(pointDataVolumeHistoryDO)) {
                count = getPointCount(deviceId, pointId, LocalDateTime.of(1970, 1, 1, 0, 0), this.datetime);
            } else {
                count = getPointCount(deviceId, pointId, pointDataVolumeHistoryDO.getCreateTime(), this.datetime);
            }
            PointDataVolumeHistoryDO historyDO = new PointDataVolumeHistoryDO();
            historyDO.setDriverId(driverId);
            historyDO.setDeviceId(deviceId);
            historyDO.setPointId(pointId);
            historyDO.setPointName(pointItem.getPointName());
            historyDO.setTotal(count);
            historyDO.setCreateTime(LocalDateTime.now());
            pointDataVolumeHistoryManager.save(historyDO);
            log.info("save point data history table success");
            /**更新或插入动态表*/
            savePointDataVolumeRun(historyDO);
        });
    }

    /**
     * 获取点位总数
     *
     * @param deviceId  设备id
     * @param pointId   点id
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return {@link Long}
     */
    private Long getPointCount(long deviceId, long pointId, LocalDateTime startTime, LocalDateTime endTime) {
        String collectionText = "point_value_";
        Query query = new Query(Criteria.where("deviceId").is(deviceId))
                .addCriteria(Criteria.where("pointId").is(pointId))
                .addCriteria(Criteria.where("createTime").gt(startTime).lt(endTime));
        return mongoTemplate.count(query, collectionText + deviceId);
    }

    /**
     * 保存点位数据量
     *
     * @param historyDO
     */
    private void savePointDataVolumeRun(PointDataVolumeHistoryDO historyDO) {
        PointDataVolumeRunDO runDO = new PointDataVolumeRunDO();
        LocalDateTime minDay = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIN);
        BeanUtils.copyProperties(historyDO, runDO, "id");
        runDO.setCreateTime(minDay);
        QueryWrapper<PointDataVolumeHistoryDO> wrapperHistory = new QueryWrapper<>();
        wrapperHistory.select("sum(total) as total").lambda().gt(PointDataVolumeHistoryDO::getCreateTime, minDay);
        PointDataVolumeHistoryDO sumTotal = pointDataVolumeHistoryManager.getOne(wrapperHistory);
        runDO.setTotal(sumTotal != null ? sumTotal.getTotal() : 0);
        LambdaQueryWrapper<PointDataVolumeRunDO> wrapper = Wrappers.<PointDataVolumeRunDO>query().lambda();
        wrapper.eq(PointDataVolumeRunDO::getPointId, runDO.getPointId()).eq(PointDataVolumeRunDO::getDeviceId, runDO.getDeviceId()).eq(PointDataVolumeRunDO::getCreateTime, minDay);
        pointDataVolumeRunManager.saveOrUpdate(runDO, wrapper);
        log.info("save point data run table success");
    }
}
