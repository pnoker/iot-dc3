package io.github.pnoker.center.data.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.pnoker.center.data.entity.model.DeviceRunDO;
import io.github.pnoker.center.data.entity.model.DeviceRunHistoryDO;
import io.github.pnoker.center.data.entity.model.DriverRunDO;
import io.github.pnoker.center.data.entity.model.DriverRunHistoryDO;

import java.time.LocalDateTime;

public interface DeviceRunHistoryService extends IService<DeviceRunHistoryDO> {
    DeviceRunDO getDurationDay(Long id, String code, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
