package io.github.pnoker.center.manager.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.manager.entity.model.PointAttributeDO;
import io.github.pnoker.center.manager.manager.PointAttributeManager;
import io.github.pnoker.center.manager.mapper.PointAttributeMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 模板配置信息表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class PointAttributeManagerImpl extends ServiceImpl<PointAttributeMapper, PointAttributeDO> implements PointAttributeManager {

}
