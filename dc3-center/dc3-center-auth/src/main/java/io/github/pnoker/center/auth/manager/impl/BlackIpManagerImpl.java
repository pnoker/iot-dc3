package io.github.pnoker.center.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.entity.model.BlackIpDO;
import io.github.pnoker.center.auth.manager.BlackIpManager;
import io.github.pnoker.center.auth.mapper.BlackIpMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Ip黑名单表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class BlackIpManagerImpl extends ServiceImpl<BlackIpMapper, BlackIpDO> implements BlackIpManager {

}
