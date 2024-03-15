package io.github.pnoker.center.data.service.impl;

import io.github.pnoker.center.data.entity.model.DriverStatusHistoryDO;
import io.github.pnoker.center.data.mapper.DriverStatusHistoryMapper;
import io.github.pnoker.center.data.service.DriverStatusHistoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DriverStatusHistoryServiceImpl implements DriverStatusHistoryService {
    @Resource
    private DriverStatusHistoryMapper driverStatusHistoryMapper;

    @Override
    public List<DriverStatusHistoryDO> selectRecently2Data(long id) {
        return driverStatusHistoryMapper.selectRecently2Data(id);
    }
}
