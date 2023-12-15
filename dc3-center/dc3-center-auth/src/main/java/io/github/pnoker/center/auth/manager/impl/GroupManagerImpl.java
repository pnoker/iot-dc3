package io.github.pnoker.center.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.entity.model.GroupDO;
import io.github.pnoker.center.auth.manager.GroupManager;
import io.github.pnoker.center.auth.mapper.GroupMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 分组表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class GroupManagerImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupManager {

}
