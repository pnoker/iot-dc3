package io.github.pnoker.center.auth.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.dal.DriverTokenManager;
import io.github.pnoker.center.auth.entity.model.DriverTokenDO;
import io.github.pnoker.center.auth.mapper.DriverTokenMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 驱动令牌表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class DriverTokenManagerImpl extends ServiceImpl<DriverTokenMapper, DriverTokenDO> implements DriverTokenManager {

}
