package io.github.pnoker.center.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.entity.model.ApiDO;
import io.github.pnoker.center.auth.manager.ApiManager;
import io.github.pnoker.center.auth.mapper.ApiMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 接口表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class ApiManagerImpl extends ServiceImpl<ApiMapper, ApiDO> implements ApiManager {

}
