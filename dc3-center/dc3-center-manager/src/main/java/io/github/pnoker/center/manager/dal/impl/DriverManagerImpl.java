package io.github.pnoker.center.manager.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.manager.dal.DriverManager;
import io.github.pnoker.center.manager.entity.model.DriverDO;
import io.github.pnoker.center.manager.mapper.DriverMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 协议驱动表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class DriverManagerImpl extends ServiceImpl<DriverMapper, DriverDO> implements DriverManager {

}
