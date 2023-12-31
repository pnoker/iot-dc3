package io.github.pnoker.center.manager.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.manager.entity.model.ProfileDO;
import io.github.pnoker.center.manager.dal.ProfileManager;
import io.github.pnoker.center.manager.mapper.ProfileMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 设备模板表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class ProfileManagerImpl extends ServiceImpl<ProfileMapper, ProfileDO> implements ProfileManager {

}
