package io.github.pnoker.center.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.data.entity.model.DeviceRunDO;
import io.github.pnoker.center.data.entity.model.DeviceRunHistoryDO;
import io.github.pnoker.center.data.entity.model.DriverRunDO;
import io.github.pnoker.center.data.entity.model.DriverRunHistoryDO;
import io.github.pnoker.center.data.mapper.DeviceRunHistoryMapper;
import io.github.pnoker.center.data.mapper.DriverRunHistoryMapper;
import io.github.pnoker.center.data.service.DeviceRunHistoryService;
import io.github.pnoker.center.data.service.DriverRunHistoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class DeviceRunHistoryServiceImpl extends ServiceImpl<DeviceRunHistoryMapper, DeviceRunHistoryDO>  implements DeviceRunHistoryService {
    @Resource
    private DeviceRunHistoryMapper deviceRunHistoryMapper;

    @Override
    public DeviceRunDO getDurationDay(Long id, String code, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return deviceRunHistoryMapper.getDurationDay(id,code,startOfDay,endOfDay);
    }
}
