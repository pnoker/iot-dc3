package io.github.pnoker.center.manager.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.manager.dal.DeviceManager;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
import io.github.pnoker.center.manager.mapper.DeviceMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 设备表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class DeviceManagerImpl extends ServiceImpl<DeviceMapper, DeviceDO> implements DeviceManager {

}
