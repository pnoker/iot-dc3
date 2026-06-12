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
import io.github.pnoker.common.auth.entity.bo.ServiceAccountBO;
import io.github.pnoker.common.auth.entity.model.ServiceAccountDO;
import io.github.pnoker.common.auth.entity.vo.ServiceAccountVO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;

/**
 * MapStruct builder for service accounts.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface ServiceAccountBuilder {

    ServiceAccountBO buildBOByVO(ServiceAccountVO entityVO);

    ServiceAccountVO buildVOByBO(ServiceAccountBO entityBO);

    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ServiceAccountDO buildDOByBO(ServiceAccountBO entityBO);

    @AfterMapping
    default void afterProcess(ServiceAccountBO entityBO, @MappingTarget ServiceAccountDO entityDO) {
        if (Objects.nonNull(entityBO.getEnableFlag())) {
            entityDO.setEnableFlag(entityBO.getEnableFlag().getIndex());
        }
    }

    @Mapping(target = "enableFlag", ignore = true)
    ServiceAccountBO buildBOByDO(ServiceAccountDO entityDO);

    @AfterMapping
    default void afterProcess(ServiceAccountDO entityDO, @MappingTarget ServiceAccountBO entityBO) {
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(entityDO.getEnableFlag()));
    }

    List<ServiceAccountBO> buildBOListByDOList(List<ServiceAccountDO> entityDOList);

    default Page<ServiceAccountBO> buildBOPageByDOPage(Page<ServiceAccountDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    default Page<ServiceAccountVO> buildVOPageByBOPage(Page<ServiceAccountBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
