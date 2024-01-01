package io.github.pnoker.center.auth.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.dal.UserPasswordManager;
import io.github.pnoker.center.auth.entity.model.UserPasswordDO;
import io.github.pnoker.center.auth.mapper.UserPasswordMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户密码表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class UserPasswordManagerImpl extends ServiceImpl<UserPasswordMapper, UserPasswordDO> implements UserPasswordManager {

}
