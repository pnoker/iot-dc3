package io.github.pnoker.center.data.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.data.entity.model.AlarmNotifyProfileDO;
import io.github.pnoker.center.data.manager.AlarmNotifyProfileManager;
import io.github.pnoker.center.data.mapper.AlarmNotifyProfileMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 报警通知模板表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class AlarmNotifyProfileManagerImpl extends ServiceImpl<AlarmNotifyProfileMapper, AlarmNotifyProfileDO> implements AlarmNotifyProfileManager {

}
