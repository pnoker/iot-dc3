package io.github.pnoker.center.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.entity.model.RoleUserBindDO;
import io.github.pnoker.center.auth.manager.RoleUserBindManager;
import io.github.pnoker.center.auth.mapper.RoleUserBindMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 角色-用户关联表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class RoleUserBindManagerImpl extends ServiceImpl<RoleUserBindMapper, RoleUserBindDO> implements RoleUserBindManager {

}
