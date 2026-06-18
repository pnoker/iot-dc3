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

package io.github.pnoker.common.auth.entity.builder;

import io.github.pnoker.common.auth.entity.bo.IdentityAuditLogBO;
import io.github.pnoker.common.auth.entity.model.IdentityAuditLogDO;
import io.github.pnoker.common.auth.entity.vo.IdentityAuditLogVO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct builder converting between identity audit log DO, BO, and VO.
 *
 * @author pnoker
 * @version 2026.6.14
 * @since 2026.6.14
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface IdentityAuditLogBuilder {

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    IdentityAuditLogBO buildBOByDO(IdentityAuditLogDO entityDO);

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<IdentityAuditLogBO> buildBOListByDOList(List<IdentityAuditLogDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    IdentityAuditLogVO buildVOByBO(IdentityAuditLogBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<IdentityAuditLogVO> buildVOListByBOList(List<IdentityAuditLogBO> entityBOList);

}
