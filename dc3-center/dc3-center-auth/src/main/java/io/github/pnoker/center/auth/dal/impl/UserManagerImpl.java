package io.github.pnoker.center.auth.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.entity.model.UserDO;
import io.github.pnoker.center.auth.dal.UserManager;
import io.github.pnoker.center.auth.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class UserManagerImpl extends ServiceImpl<UserMapper, UserDO> implements UserManager {

}
