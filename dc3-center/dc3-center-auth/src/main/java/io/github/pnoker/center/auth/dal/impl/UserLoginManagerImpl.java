package io.github.pnoker.center.auth.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.dal.UserLoginManager;
import io.github.pnoker.center.auth.entity.model.UserLoginDO;
import io.github.pnoker.center.auth.mapper.UserLoginMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户登录表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class UserLoginManagerImpl extends ServiceImpl<UserLoginMapper, UserLoginDO> implements UserLoginManager {

}
