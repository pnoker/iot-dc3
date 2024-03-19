package io.github.pnoker.center.data.service;

import io.github.pnoker.center.data.entity.model.DriverRunDO;

import java.util.List;

public interface DriverRunService {
    List<DriverRunDO> get7daysDuration(Long driverId, String code);
}
