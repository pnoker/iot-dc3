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

package io.github.pnoker.common.dal.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Label binding view object (VO) representing the association between labels and
 * entities.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Label Bind view object")
public class LabelBindVO extends BaseVO {

    /**
     * Entity type flag.
     */
    @Schema(description = "Entity type enum")
    @NotNull(message = "Entity type flag can't be empty", groups = {Add.class, Update.class})
    private EntityTypeEnum entityTypeFlag;

    /**
     * Label ID.
     */
    @Schema(description = "label ID")
    @NotNull(message = "Label ID can't be empty", groups = {Add.class, Update.class})
    private Long labelId;

    /**
     * Entity ID.
     */
    @Schema(description = "Associated entity ID")
    @NotNull(message = "Entity ID can't be empty", groups = {Add.class, Update.class})
    private Long entityId;

}
