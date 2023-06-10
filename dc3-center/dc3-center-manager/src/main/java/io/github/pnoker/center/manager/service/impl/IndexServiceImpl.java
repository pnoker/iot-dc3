package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.data.PointValueApiGrpc;
import io.github.pnoker.api.center.data.PointValueQuery;
import io.github.pnoker.api.center.data.RPointValueDTO;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.service.DataServiceConstant;
import io.github.pnoker.common.dto.DataStatisticsDTO;
import io.github.pnoker.common.dto.WeatherDeviceStatisticsDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.model.DriverDO;
import io.github.pnoker.common.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
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
    @Resource
    private DriverAttributeConfigService attributeConfigService;
    @GrpcClient(DataServiceConstant.SERVICE_NAME)
    private PointValueApiGrpc.PointValueApiBlockingStub pointValueApiStub;


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

    @Override
    public List<WeatherDeviceStatisticsDTO> weatherDeviceList() {
        DriverDO driverDO = driverService.selectByServiceName("default/dc3-driver-weather-amap", "1", false);
        return deviceService.selectAllByDriverId(driverDO.getId(), "1").stream().map(device -> {
            WeatherDeviceStatisticsDTO weatherDeviceStatisticsDTO = new WeatherDeviceStatisticsDTO();
            weatherDeviceStatisticsDTO.setDevice(device);
            weatherDeviceStatisticsDTO.setLocation(attributeConfigService.selectByDeviceId(device.getId()).get(0).getConfigValue());
            String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + device.getId();
            String status = redisUtil.getKey(key);
            status = ObjectUtil.isNotNull(status) ? status : DeviceStatusEnum.OFFLINE.getCode();
            weatherDeviceStatisticsDTO.setStatus(status);
            PointValueQuery.Builder request = PointValueQuery.newBuilder();
            request.setTenantId("1");
            request.setDeviceId(device.getId());
            request.setPointId(pointService.selectByDeviceId(device.getId()).get(0).getId());
            RPointValueDTO rPointValueDTO = pointValueApiStub.lastValue(request.build());
            if (rPointValueDTO.getResult().getOk()) {
                weatherDeviceStatisticsDTO.setLastValue(rPointValueDTO.getData().getRawValue());
            }
            return weatherDeviceStatisticsDTO;
        }).collect(Collectors.toList());
    }
}
