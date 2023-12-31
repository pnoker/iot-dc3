package io.github.pnoker.center.auth.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.entity.model.TenantDO;
import io.github.pnoker.center.auth.dal.TenantManager;
import io.github.pnoker.center.auth.mapper.TenantMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class TenantManagerImpl extends ServiceImpl<TenantMapper, TenantDO> implements TenantManager {

}
