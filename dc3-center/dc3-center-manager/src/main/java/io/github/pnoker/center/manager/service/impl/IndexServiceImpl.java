package io.github.pnoker.center.manager.service.impl;

import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.dto.DataStatisticsDTO;
import io.github.pnoker.common.utils.RedisUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class IndexServiceImpl implements IndexService {

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private DriverService driverService;
    @Resource
    private DeviceService deviceService;
    @Resource
    private PointService pointService;
    @Resource
    private ProfileService profileService;


    /**
     * 每小时更新一次
     *
     * @return
     */
    @Override
    public DataStatisticsDTO dataStatistics() {
        DataStatisticsDTO dataStatisticsDTO = redisUtil.getKey(PrefixConstant.DATA_STATISTICS);
        if (null == dataStatisticsDTO) {
            dataStatisticsDTO = new DataStatisticsDTO();
            dataStatisticsDTO.setDriverCount(driverService.count());
            dataStatisticsDTO.setDeviceCount(deviceService.count());
            dataStatisticsDTO.setPointCount(pointService.count());
            dataStatisticsDTO.setProfileCount(profileService.count());
            dataStatisticsDTO.setDataCount(deviceService.dataCount());
            redisUtil.setKey(PrefixConstant.DATA_STATISTICS, dataStatisticsDTO, 60, TimeUnit.MINUTES);
        }
        return dataStatisticsDTO;
    }
}
