package io.github.pnoker.center.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.entity.model.LabelBindDO;
import io.github.pnoker.center.auth.manager.LabelBindManager;
import io.github.pnoker.center.auth.mapper.LabelBindMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 标签关联表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class LabelBindManagerImpl extends ServiceImpl<LabelBindMapper, LabelBindDO> implements LabelBindManager {

}
