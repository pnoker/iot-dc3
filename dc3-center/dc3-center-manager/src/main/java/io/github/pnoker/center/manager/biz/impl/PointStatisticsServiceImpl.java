package io.github.pnoker.center.manager.biz.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.pnoker.center.manager.biz.PointStatisticsService;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
import io.github.pnoker.center.manager.entity.model.DriverDO;
import io.github.pnoker.center.manager.entity.model.PointDataVolumeHistoryDO;
import io.github.pnoker.center.manager.entity.model.PointDataVolumeRunDO;
import io.github.pnoker.center.manager.mapper.DeviceMapper;
import io.github.pnoker.center.manager.mapper.DriverMapper;
import io.github.pnoker.center.manager.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 点位统计
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
    private DriverMapper driverMapper;
    @Resource
    private DeviceMapper deviceMapper;
    @Resource
    private PointService pointService;
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private PointDataVolumeHistoryService pointDataVolumeHistoryService;
    @Resource
    private PointDataVolumeRunService pointDataVolumeRunService;

    private LocalDateTime datetime;


    /**
     * 统计点历史
     *
     * @param datetime 日期时间
     */
    @Override
    public void statisticsPointHistory(LocalDateTime datetime) {
        this.datetime = datetime;
        List<DriverDO> driverList = driverMapper.selectList(new QueryWrapper<>());
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
        QueryWrapper<DeviceDO> wrapper = new QueryWrapper<>();
        wrapper.eq("driver_id", driverId);
        List<DeviceDO> deviceList = deviceMapper.selectList(wrapper);
        deviceList.forEach(item -> {
            getPointSaveMongo(driverId, item.getId());
        });
    }

    /**
     * 获得分数保存mongo
     *
     * @param deviceId 设备id
     */
    private void getPointSaveMongo(long driverId, long deviceId) {
        List<PointBO> pointList = pointService.selectByDeviceId(deviceId);
        pointList.forEach(pointItem -> {
            Long pointId = pointItem.getId();
            QueryWrapper<PointDataVolumeHistoryDO> wrapper = new QueryWrapper<>();
            wrapper.orderByDesc("create_time").eq("point_id", pointId).last("LIMIT 1");
            PointDataVolumeHistoryDO pointDataVolumeHistoryDO = pointDataVolumeHistoryService.getOne(wrapper);
            long count;
            if (ObjectUtil.isEmpty(pointDataVolumeHistoryDO)) {
                count = getPointCount(deviceId, pointId,LocalDateTime.MIN, this.datetime);
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
            pointDataVolumeHistoryService.save(historyDO);
            log.info("save point data history table success");
            /**更新或插入动态表*/
            savePointDataVolumeRun(historyDO);
        });
    }

    /**
     * 获取点数
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
     * 保存点数据卷运行
     *
     * @param historyDO 历史确实
     */
    private void savePointDataVolumeRun(PointDataVolumeHistoryDO historyDO) {
        PointDataVolumeRunDO runDO = new PointDataVolumeRunDO();
        LocalDateTime minDay = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIN);
        BeanUtils.copyProperties(historyDO, runDO, "id");
        runDO.setCreateTime(minDay);
        QueryWrapper<PointDataVolumeHistoryDO> wrapperHistory = new QueryWrapper<>();
        wrapperHistory.gt("create_time", minDay);
        wrapperHistory.select("sum(total) as total");
        PointDataVolumeHistoryDO sumTotal = pointDataVolumeHistoryService.getOne(wrapperHistory);
        runDO.setTotal(sumTotal != null ? sumTotal.getTotal() : 0);
        QueryWrapper<PointDataVolumeRunDO> wrapper = new QueryWrapper<>();
        wrapper.eq("point_id", runDO.getPointId()).eq("device_id", runDO.getDeviceId()).eq("create_time", minDay);
        pointDataVolumeRunService.saveOrUpdate(runDO, wrapper);
        log.info("save point data run table success");
    }
}
