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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.TenantBindBO;
import io.github.pnoker.common.auth.entity.model.TenantBindDO;
import io.github.pnoker.common.auth.entity.vo.TenantBindVO;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct builder converting between tenant binding BO, VO, and DO.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface TenantBindBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    TenantBindBO buildBOByVO(TenantBindVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<TenantBindBO> buildBOListByVOList(List<TenantBindVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "deleted", ignore = true)
    TenantBindDO buildDOByBO(TenantBindBO entityBO);

    /**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     */
    List<TenantBindDO> buildDOListByBOList(List<TenantBindBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    TenantBindBO buildBOByDO(TenantBindDO entityDO);

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<TenantBindBO> buildBOListByDOList(List<TenantBindDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    TenantBindVO buildVOByBO(TenantBindBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<TenantBindVO> buildVOListByBOList(List<TenantBindBO> entityBOList);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    default Page<TenantBindBO> buildBOPageByDOPage(Page<TenantBindDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    default Page<TenantBindVO> buildVOPageByBOPage(Page<TenantBindBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
