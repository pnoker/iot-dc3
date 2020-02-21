package com.pnoker.common.sdk.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.manager.feign.DriverInfoClient;
import com.pnoker.api.center.manager.feign.PointInfoClient;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.bean.R;
import com.pnoker.common.dto.DriverInfoDto;
import com.pnoker.common.dto.PointInfoDto;
import com.pnoker.common.model.*;
import com.pnoker.common.sdk.init.DeviceDriver;
import com.pnoker.common.sdk.service.CustomizersService;
import com.pnoker.common.sdk.service.DriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {
    @Resource
    private PointInfoClient pointInfoClient;
    @Resource
    private DriverInfoClient driverInfoClient;
    @Resource
    private DeviceDriver deviceDriver;
    @Resource
    private CustomizersService customizersService;

    @Override
    public void read(Long deviceId, Long pointId) {
        Device device = deviceDriver.getDeviceMap().get(deviceId);
        Profile profile = deviceDriver.getProfileMap().get(device.getProfileId());
        Point point = deviceDriver.getPointMap().get(profile.getId()).get(pointId);

        DriverInfoDto driverInfoDto = new DriverInfoDto();
        driverInfoDto.setProfileId(profile.getId());
        driverInfoDto.setPage(new Pages().setSize(-1L));
        Map<String, String> dd = new HashMap<>(16);
        R<Page<DriverInfo>> rd = driverInfoClient.list(driverInfoDto);
        if (rd.isOk()) {
            List<DriverInfo> driverInfos = rd.getData().getRecords();
            for (DriverInfo driverInfo : driverInfos) {
                dd.put(deviceDriver.getDriverAttributeMap().get(driverInfo.getDriverAttributeId()).getName(), driverInfo.getValue());
            }
        }

        PointInfoDto pointInfoDto = new PointInfoDto();
        pointInfoDto.setDeviceId(deviceId).setPointId(pointId);
        pointInfoDto.setPage(new Pages().setSize(-1L));
        Map<String, String> pp = new HashMap<>(16);
        R<Page<PointInfo>> rp = pointInfoClient.list(pointInfoDto);
        if (rp.isOk()) {
            List<PointInfo> pointInfos = rp.getData().getRecords();
            for (PointInfo pointInfo : pointInfos) {
                pp.put(deviceDriver.getPointAttributeMap().get(pointInfo.getPointAttributeId()).getName(), pointInfo.getValue());
            }
        }


        customizersService.read(dd, pp);
    }
}
