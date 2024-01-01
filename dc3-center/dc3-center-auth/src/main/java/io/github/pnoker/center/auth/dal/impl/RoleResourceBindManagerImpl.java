package io.github.pnoker.center.auth.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.dal.RoleResourceBindManager;
import io.github.pnoker.center.auth.entity.model.RoleResourceBindDO;
import io.github.pnoker.center.auth.mapper.RoleResourceBindMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 角色-权限资源关联表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class RoleResourceBindManagerImpl extends ServiceImpl<RoleResourceBindMapper, RoleResourceBindDO> implements RoleResourceBindManager {

}
