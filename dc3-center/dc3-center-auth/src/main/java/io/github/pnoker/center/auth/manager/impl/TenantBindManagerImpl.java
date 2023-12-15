package io.github.pnoker.center.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.entity.model.TenantBindDO;
import io.github.pnoker.center.auth.manager.TenantBindManager;
import io.github.pnoker.center.auth.mapper.TenantBindMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户关联表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class TenantBindManagerImpl extends ServiceImpl<TenantBindMapper, TenantBindDO> implements TenantBindManager {

}
