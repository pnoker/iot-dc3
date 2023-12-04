package io.github.pnoker.center.manager.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.manager.entity.model.PointDO;
import io.github.pnoker.center.manager.manager.PointManager;
import io.github.pnoker.center.manager.mapper.PointMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 设备位号表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class PointManagerImpl extends ServiceImpl<PointMapper, PointDO> implements PointManager {

}
