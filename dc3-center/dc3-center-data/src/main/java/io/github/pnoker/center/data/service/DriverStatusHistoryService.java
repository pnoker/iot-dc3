package io.github.pnoker.center.data.service;

import io.github.pnoker.center.data.entity.model.DriverStatusHistoryDO;

import java.util.List;

public interface DriverStatusHistoryService {

    List<DriverStatusHistoryDO> selectRecently2Data(long id);
}
