package io.github.pnoker.center.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.entity.model.LimitedIpDO;
import io.github.pnoker.center.auth.manager.LimitedIpManager;
import io.github.pnoker.center.auth.mapper.LimitedIpMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Ip限制表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class LimitedIpManagerImpl extends ServiceImpl<LimitedIpMapper, LimitedIpDO> implements LimitedIpManager {

}
