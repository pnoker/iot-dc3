package io.github.pnoker.center.data.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.data.entity.model.GroupDO;
import io.github.pnoker.center.data.manager.GroupManager;
import io.github.pnoker.center.data.mapper.GroupMapper;
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
