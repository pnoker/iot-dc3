package io.github.pnoker.center.data.dal.impl;

import io.github.pnoker.center.data.entity.model.DeviceRunDO;
import io.github.pnoker.center.data.mapper.DeviceRunMapper;
import io.github.pnoker.center.data.dal.DeviceRunManager;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 设备运行时长历史表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
@Service
public class DeviceRunManagerImpl extends ServiceImpl<DeviceRunMapper, DeviceRunDO> implements DeviceRunManager {

}
