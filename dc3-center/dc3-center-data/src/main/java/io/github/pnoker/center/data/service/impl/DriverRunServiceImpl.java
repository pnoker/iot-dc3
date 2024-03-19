package io.github.pnoker.center.data.service.impl;

import io.github.pnoker.center.data.dal.DriverRunManager;
import io.github.pnoker.center.data.entity.model.DriverRunDO;
import io.github.pnoker.center.data.mapper.DriverRunMapper;
import io.github.pnoker.center.data.service.DriverRunService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DriverRunServiceImpl implements DriverRunService {
    @Resource
    private DriverRunMapper driverRunMapper;
    @Override
    public List<DriverRunDO> get7daysDuration(Long driverId, String code) {
        return driverRunMapper.get7daysDuration(driverId,code);
    }
}
