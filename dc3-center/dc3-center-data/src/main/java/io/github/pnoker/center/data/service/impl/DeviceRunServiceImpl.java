package io.github.pnoker.center.data.service.impl;

import io.github.pnoker.center.data.entity.model.DeviceRunDO;
import io.github.pnoker.center.data.mapper.DeviceRunMapper;
import io.github.pnoker.center.data.service.DeviceRunService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DeviceRunServiceImpl implements DeviceRunService {
    @Resource
    private DeviceRunMapper deviceRunMapper;
    @Override
    public List<DeviceRunDO> get7daysDuration(Long deviceId, String code) {
        return deviceRunMapper.get7daysDuration(deviceId,code);
    }
}
