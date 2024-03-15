package io.github.pnoker.center.data.dal.impl;

import io.github.pnoker.center.data.entity.model.DriverStatusHistoryDO;
import io.github.pnoker.center.data.mapper.DriverStatusHistoryMapper;
import io.github.pnoker.center.data.dal.DriverStatusHistoryManager;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 驱动状态历史表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
@Service
public class DriverStatusHistoryManagerImpl extends ServiceImpl<DriverStatusHistoryMapper, DriverStatusHistoryDO> implements DriverStatusHistoryManager {

}
