package io.github.pnoker.center.auth.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.auth.dal.MenuManager;
import io.github.pnoker.center.auth.entity.model.MenuDO;
import io.github.pnoker.center.auth.mapper.MenuMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 菜单表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class MenuManagerImpl extends ServiceImpl<MenuMapper, MenuDO> implements MenuManager {

}
