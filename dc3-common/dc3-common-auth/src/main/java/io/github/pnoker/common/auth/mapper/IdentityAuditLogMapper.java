/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.pnoker.common.auth.entity.model.IdentityAuditLogDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis-Plus mapper for the dc3_identity_audit_log table.
 *
 * @author pnoker
 * @version 2026.6.14
 * @since 2026.6.14
 */
public interface IdentityAuditLogMapper extends BaseMapper<IdentityAuditLogDO> {

    List<IdentityAuditLogDO> listIdentityAudit(@Param("tenantId") Long tenantId,
                                               @Param("principalId") Long principalId,
                                               @Param("action") String action,
                                               @Param("resourceType") String resourceType,
                                               @Param("resourceId") Long resourceId,
                                               @Param("status") String status,
                                               @Param("limit") int limit);
}
