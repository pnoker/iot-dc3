package io.github.pnoker.center.manager.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.manager.dal.DriverAttributeManager;
import io.github.pnoker.center.manager.entity.model.DriverAttributeDO;
import io.github.pnoker.center.manager.mapper.DriverAttributeMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 连接配置信息表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class DriverAttributeManagerImpl extends ServiceImpl<DriverAttributeMapper, DriverAttributeDO> implements DriverAttributeManager {

}
