package io.github.pnoker.center.data.dal.impl;

import io.github.pnoker.center.data.entity.model.DeviceStatusHistoryDO;
import io.github.pnoker.center.data.mapper.DeviceStatusHistoryMapper;
import io.github.pnoker.center.data.dal.DeviceStatusHistoryManager;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 设备状态历史表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
@Service
public class DeviceStatusHistoryManagerImpl extends ServiceImpl<DeviceStatusHistoryMapper, DeviceStatusHistoryDO> implements DeviceStatusHistoryManager {

}
