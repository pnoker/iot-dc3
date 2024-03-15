package io.github.pnoker.center.data.dal.impl;

import io.github.pnoker.center.data.entity.model.DriverRunDO;
import io.github.pnoker.center.data.mapper.DriverRunMapper;
import io.github.pnoker.center.data.dal.DriverRunManager;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 驱动运行时长历史表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
@Service
public class DriverRunManagerImpl extends ServiceImpl<DriverRunMapper, DriverRunDO> implements DriverRunManager {

}
