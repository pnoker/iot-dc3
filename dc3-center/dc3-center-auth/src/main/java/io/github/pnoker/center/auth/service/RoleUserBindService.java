package io.github.pnoker.center.auth.service;

import io.github.pnoker.center.auth.entity.query.RoleUserBindPageQuery;
import io.github.pnoker.common.base.Service;
import io.github.pnoker.common.model.Role;
import io.github.pnoker.common.model.RoleUserBind;

import java.util.List;

/**
 * role user mapper service
 *
 * @author linys
 * @since 2023.04.02
 */
public interface RoleUserBindService extends Service<RoleUserBind, RoleUserBindPageQuery> {

    /**
     * 根据 租户id 和 用户id 查询
     *
     * @param tenantId 租户id
     * @param userId   用户id
     * @return Role list
     */
    List<Role> listRoleByTenantIdAndUserId(String tenantId, String userId);
}
