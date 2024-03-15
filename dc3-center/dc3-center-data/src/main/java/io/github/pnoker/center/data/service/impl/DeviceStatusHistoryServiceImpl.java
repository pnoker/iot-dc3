package io.github.pnoker.center.data.service.impl;

import io.github.pnoker.center.data.entity.model.DeviceStatusHistoryDO;
import io.github.pnoker.center.data.entity.model.DriverStatusHistoryDO;
import io.github.pnoker.center.data.mapper.DeviceStatusHistoryMapper;
import io.github.pnoker.center.data.mapper.DriverStatusHistoryMapper;
import io.github.pnoker.center.data.service.DeviceStatusHistoryService;
import io.github.pnoker.center.data.service.DriverStatusHistoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DeviceStatusHistoryServiceImpl implements DeviceStatusHistoryService {
    @Resource
    private DeviceStatusHistoryMapper deviceStatusHistoryMapper;

    @Override
    public List<DeviceStatusHistoryDO> selectRecently2Data(long id) {
        return deviceStatusHistoryMapper.selectRecently2Data(id);
    }
}
