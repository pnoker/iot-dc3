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

package io.github.pnoker.common.manager.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.dal.entity.bo.DictionaryBO;
import io.github.pnoker.common.dal.entity.builder.DictionaryBuilder;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.Mapper;

/**
 * Dictionary For Manager Builder
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface DictionaryForManagerBuilder extends DictionaryBuilder {

    //

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByDriverBO(DriverBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getDriverName()).value(entityBO.getId().toString()).build();
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    default Page<DictionaryBO> buildVOPageByDriverBOPage(Page<DriverBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByDriverBO);
    }

    //

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByProfileBO(ProfileBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getProfileName()).value(entityBO.getId().toString()).build();
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    default Page<DictionaryBO> buildVOPageByProfileBOPage(Page<ProfileBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByProfileBO);
    }

    //

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByPointBO(PointBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getPointName()).value(entityBO.getId().toString()).build();
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    default Page<DictionaryBO> buildVOPageByPointBOPage(Page<PointBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByPointBO);
    }

    //

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByDeviceBO(DeviceBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getDeviceName()).value(entityBO.getId().toString()).build();
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    default Page<DictionaryBO> buildVOPageByDeviceBOPage(Page<DeviceBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByDeviceBO);
    }

}
