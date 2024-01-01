package io.github.pnoker.center.data.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.data.dal.AlarmMessageProfileManager;
import io.github.pnoker.center.data.entity.model.AlarmMessageProfileDO;
import io.github.pnoker.center.data.mapper.AlarmMessageProfileMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 报警信息模板表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class AlarmMessageProfileManagerImpl extends ServiceImpl<AlarmMessageProfileMapper, AlarmMessageProfileDO> implements AlarmMessageProfileManager {

}
