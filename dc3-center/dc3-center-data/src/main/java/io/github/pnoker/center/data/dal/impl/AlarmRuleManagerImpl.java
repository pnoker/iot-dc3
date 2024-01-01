package io.github.pnoker.center.data.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.data.dal.AlarmRuleManager;
import io.github.pnoker.center.data.entity.model.AlarmRuleDO;
import io.github.pnoker.center.data.mapper.AlarmRuleMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 报警规则表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class AlarmRuleManagerImpl extends ServiceImpl<AlarmRuleMapper, AlarmRuleDO> implements AlarmRuleManager {

}
