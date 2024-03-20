package io.github.pnoker.center.data.service;

import io.github.pnoker.center.data.entity.model.DeviceRunDO;

import java.util.List;

public interface DeviceRunService {
    List<DeviceRunDO> get7daysDuration(Long deviceId, String code);
}
