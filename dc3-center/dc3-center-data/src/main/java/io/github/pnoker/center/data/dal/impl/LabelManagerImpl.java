package io.github.pnoker.center.data.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.data.dal.LabelManager;
import io.github.pnoker.center.data.entity.model.LabelDO;
import io.github.pnoker.center.data.mapper.LabelMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 标签表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class LabelManagerImpl extends ServiceImpl<LabelMapper, LabelDO> implements LabelManager {

}
