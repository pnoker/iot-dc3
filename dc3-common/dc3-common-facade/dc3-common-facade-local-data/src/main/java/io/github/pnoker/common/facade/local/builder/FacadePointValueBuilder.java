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

import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Converts between the facade-api shapes and {@code dc3-common-data} internals.
 * <p>
 * Maps {@code calValue} to {@code value}, converts {@code LocalDateTime createTime} to
 * epoch seconds.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface FacadePointValueBuilder {

    @Mapping(source = "calValue", target = "value")
    @Mapping(source = "createTime", target = "createTime")
    FacadePointValueBO toFacadeBO(PointValueBO bo);

    /**
     * Convert LocalDateTime to epoch seconds.
     */
    default long map(LocalDateTime value) {
        if (Objects.isNull(value)) {
            return 0L;
        }
        return value.toEpochSecond(ZoneOffset.UTC);
    }

}
