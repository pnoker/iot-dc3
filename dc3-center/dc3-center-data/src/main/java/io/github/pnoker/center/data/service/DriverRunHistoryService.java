package io.github.pnoker.center.data.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.pnoker.center.data.entity.model.DriverRunDO;
import io.github.pnoker.center.data.entity.model.DriverRunHistoryDO;

import java.time.LocalDateTime;

public interface DriverRunHistoryService extends IService<DriverRunHistoryDO> {
    DriverRunDO getDurationDay(Long id, String code, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
