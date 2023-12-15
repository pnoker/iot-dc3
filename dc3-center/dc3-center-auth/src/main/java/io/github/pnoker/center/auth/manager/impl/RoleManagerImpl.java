package io.github.pnoker.center.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.entity.model.RoleDO;
import io.github.pnoker.center.auth.manager.RoleManager;
import io.github.pnoker.center.auth.mapper.RoleMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class RoleManagerImpl extends ServiceImpl<RoleMapper, RoleDO> implements RoleManager {

}
