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

import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.model.LocalCredentialDO;
import io.github.pnoker.common.auth.entity.vo.LocalCredentialVO;
import io.github.pnoker.common.enums.CredentialTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PasswordAlgorithmEnum;
import io.github.pnoker.common.enums.RequirePasswordChangeFlagEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import java.util.List;
import java.util.Objects;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct builder for local credentials.
 *
 * @author pnoker
 * @since 2026.6.12
 */
@Mapper(
        componentModel = "spring",
        uses = {MapStructUtil.class})
public interface LocalCredentialBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    @Mapping(source = "password", target = "rawPassword")
    @Mapping(target = "loginNameNormalized", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    LocalCredentialBO buildBOByVO(LocalCredentialVO entityVO);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "password", ignore = true)
    LocalCredentialVO buildVOByBO(LocalCredentialBO entityBO);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "credentialType", ignore = true)
    @Mapping(target = "passwordAlgorithm", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "requirePasswordChange", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    LocalCredentialDO buildDOByBO(LocalCredentialBO entityBO);

    /**
     * After process.
     *
     * @param entityBO business object
     * @param entityDO persistence object
     */
    @AfterMapping
    default void afterProcess(LocalCredentialBO entityBO, @MappingTarget LocalCredentialDO entityDO) {
        if (Objects.nonNull(entityBO.getCredentialType())) {
            entityDO.setCredentialType(entityBO.getCredentialType().getValue());
        }
        if (Objects.nonNull(entityBO.getPasswordAlgorithm())) {
            entityDO.setPasswordAlgorithm(entityBO.getPasswordAlgorithm().getValue());
        }
        if (Objects.nonNull(entityBO.getEnableFlag())) {
            entityDO.setEnableFlag(entityBO.getEnableFlag().getIndex());
        }
        if (Objects.nonNull(entityBO.getRequirePasswordChange())) {
            entityDO.setRequirePasswordChange(
                    entityBO.getRequirePasswordChange().getIndex());
        }
    }

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    @Mapping(target = "credentialType", ignore = true)
    @Mapping(target = "passwordAlgorithm", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "requirePasswordChange", ignore = true)
    @Mapping(target = "rawPassword", ignore = true)
    LocalCredentialBO buildBOByDO(LocalCredentialDO entityDO);

    /**
     * After process.
     *
     * @param entityDO persistence object
     * @param entityBO business object
     */
    @AfterMapping
    default void afterProcess(LocalCredentialDO entityDO, @MappingTarget LocalCredentialBO entityBO) {
        entityBO.setCredentialType(CredentialTypeEnum.ofValue(entityDO.getCredentialType()));
        entityBO.setPasswordAlgorithm(PasswordAlgorithmEnum.ofValue(entityDO.getPasswordAlgorithm()));
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(entityDO.getEnableFlag()));
        entityBO.setRequirePasswordChange(RequirePasswordChangeFlagEnum.ofIndex(entityDO.getRequirePasswordChange()));
    }

    /**
     * Convert do list to bo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<LocalCredentialBO> buildBOListByDOList(List<LocalCredentialDO> entityDOList);
}
