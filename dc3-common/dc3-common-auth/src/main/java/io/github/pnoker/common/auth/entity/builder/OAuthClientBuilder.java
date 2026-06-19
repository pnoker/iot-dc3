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

import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import io.github.pnoker.common.auth.entity.vo.OAuthClientVO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;

import java.util.List;

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
    OAuthClientVO buildVOByRecord(OAuthRegisteredClientRecord entityRecord);

    /**
     * RecordList to VOList
     *
     * @param entityRecordList EntityRecord Array
     * @return EntityVO Array
     */
    List<OAuthClientVO> buildVOListByRecordList(List<OAuthRegisteredClientRecord> entityRecordList);

}
