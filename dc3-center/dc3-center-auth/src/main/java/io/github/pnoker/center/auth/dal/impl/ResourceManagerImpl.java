package io.github.pnoker.center.auth.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.dal.ResourceManager;
import io.github.pnoker.center.auth.entity.model.ResourceDO;
import io.github.pnoker.center.auth.mapper.ResourceMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 权限资源表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class ResourceManagerImpl extends ServiceImpl<ResourceMapper, ResourceDO> implements ResourceManager {

}
