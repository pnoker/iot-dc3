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

import io.github.pnoker.common.auth.entity.bo.OAuthClientRegistrationBO;
import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import io.github.pnoker.common.auth.entity.vo.OAuthClientRegistrationRequestVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientVO;
import io.github.pnoker.common.enums.OAuthClientTypeEnum;
import io.github.pnoker.common.enums.OAuthGrantTypeEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;

/**
 * MapStruct builder converting an OAuth registered client projection to its view object.
 *
 * @author pnoker
 * @version 2026.6.19
 * @since 2026.6.19
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface OAuthClientBuilder {

    /**
     * Record to VO
     *
     * @param entityRecord EntityRecord
     * @return EntityVO
     */
    @Mapping(target = "clientType", ignore = true)
    OAuthClientVO buildVOByRecord(OAuthRegisteredClientRecord entityRecord);

    @AfterMapping
    default void afterProcess(OAuthRegisteredClientRecord entityRecord, @MappingTarget OAuthClientVO entityVO) {
        entityVO.setClientType(OAuthClientTypeEnum.ofValue(entityRecord.getClientType()));
    }

    /**
     * RecordList to VOList
     *
     * @param entityRecordList EntityRecord Array
     * @return EntityVO Array
     */
    List<OAuthClientVO> buildVOListByRecordList(List<OAuthRegisteredClientRecord> entityRecordList);

    /**
     * RequestVO to BO (write-path input; string client type / grant types parsed into domain enums)
     *
     * @param entityVO Registration request VO
     * @return EntityBO
     */
    @Mapping(target = "clientType", ignore = true)
    @Mapping(target = "grantTypes", ignore = true)
    OAuthClientRegistrationBO buildBOByRequestVO(OAuthClientRegistrationRequestVO entityVO);

    @AfterMapping
    default void afterProcess(OAuthClientRegistrationRequestVO entityVO,
                              @MappingTarget OAuthClientRegistrationBO entityBO) {
        entityBO.setClientType(
                OAuthClientTypeEnum.ofValue(StringUtils.upperCase(StringUtils.trimToNull(entityVO.getClientType()))));
        if (Objects.nonNull(entityVO.getGrantTypes())) {
            entityBO.setGrantTypes(entityVO.getGrantTypes().stream()
                    .map(StringUtils::trimToNull)
                    .filter(Objects::nonNull)
                    .map(OAuthGrantTypeEnum::ofValue)
                    .filter(Objects::nonNull)
                    .toList());
        }
    }

    /**
     * Whether the request carries a non-blank but unrecognized client_type. Lets the caller reject it
     * (preserving the original "unsupported client_type" check) instead of silently defaulting to PUBLIC.
     *
     * @param entityVO Registration request VO
     * @return true when client_type is present yet not a known {@link OAuthClientTypeEnum}
     */
    default boolean isUnknownClientType(OAuthClientRegistrationRequestVO entityVO) {
        String raw = StringUtils.upperCase(StringUtils.trimToNull(entityVO.getClientType()));
        return Objects.nonNull(raw) && Objects.isNull(OAuthClientTypeEnum.ofValue(raw));
    }

}
