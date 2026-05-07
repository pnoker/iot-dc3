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

package io.github.pnoker.common.facade.local.builder;

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.entity.ext.UserIdentityExt;
import io.github.pnoker.common.entity.ext.UserSocialExt;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Objects;

/**
 * FacadeUser ↔ auth UserBO mapper.
 * <p>
 * {@code UserBO} carries typed {@code UserSocialExt}/{@code UserIdentityExt} objects,
 * while the facade contract (matching {@code GrpcUserDTO}) carries their JSON forms, so
 * these two fields need an explicit conversion.
 *
 * @author pnoker
 * @since 2026.5.5
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface FacadeUserBuilder {

    @Mapping(target = "socialExt", ignore = true)
    @Mapping(target = "identityExt", ignore = true)
    FacadeUserBO toFacadeBO(UserBO authBO);

    @AfterMapping
    default void afterProcess(UserBO authBO, @MappingTarget FacadeUserBO facadeBO) {
        UserSocialExt social = authBO.getSocialExt();
        if (Objects.nonNull(social)) {
            facadeBO.setSocialExt(JsonUtil.toJsonString(social));
        }

        UserIdentityExt identity = authBO.getIdentityExt();
        if (Objects.nonNull(identity)) {
            facadeBO.setIdentityExt(JsonUtil.toJsonString(identity));
        }
    }

}
