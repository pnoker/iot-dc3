package io.github.pnoker.center.manager.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.manager.dal.ProfileBindManager;
import io.github.pnoker.center.manager.entity.model.ProfileBindDO;
import io.github.pnoker.center.manager.mapper.ProfileBindMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 模板-设备关联表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class ProfileBindManagerImpl extends ServiceImpl<ProfileBindMapper, ProfileBindDO> implements ProfileBindManager {

}
