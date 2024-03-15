package io.github.pnoker.center.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.data.entity.model.DriverRunDO;
import io.github.pnoker.center.data.entity.model.DriverRunHistoryDO;
import io.github.pnoker.center.data.mapper.DriverRunHistoryMapper;
import io.github.pnoker.center.data.service.DriverRunHistoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class DriverRunHistoryServiceImpl extends ServiceImpl<DriverRunHistoryMapper, DriverRunHistoryDO>  implements DriverRunHistoryService {
    @Resource
    private DriverRunHistoryMapper driverRunHistoryMapper;

    @Override
    public DriverRunDO getDurationDay(Long id, String code, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return driverRunHistoryMapper.getDurationDay(id,code,startOfDay,endOfDay);
    }
}
