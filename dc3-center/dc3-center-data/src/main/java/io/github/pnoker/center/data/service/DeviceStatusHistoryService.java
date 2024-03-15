package io.github.pnoker.center.data.service;

import io.github.pnoker.center.data.entity.model.DeviceStatusHistoryDO;
import io.github.pnoker.center.data.entity.model.DriverStatusHistoryDO;

import java.util.List;

public interface DeviceStatusHistoryService {

    List<DeviceStatusHistoryDO> selectRecently2Data(long id);
}
