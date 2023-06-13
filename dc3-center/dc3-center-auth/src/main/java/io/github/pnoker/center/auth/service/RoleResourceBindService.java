package io.github.pnoker.center.auth.service;

import io.github.pnoker.center.auth.entity.query.RoleResourceBindPageQuery;
import io.github.pnoker.common.base.Service;
import io.github.pnoker.common.model.Resource;
import io.github.pnoker.common.model.RoleResourceBind;

import java.util.List;

/**
 * role resource bind service
 *
 * @author linys
 * @since 2023.04.02
 */
public interface RoleResourceBindService extends Service<RoleResourceBind, RoleResourceBindPageQuery> {

    /**
     * 根据TenantId与UserId查询资源
     *
     * @param roleId 角色id
     * @return 资源列表
     */
    List<Resource> listResourceByRoleId(String roleId);
}
