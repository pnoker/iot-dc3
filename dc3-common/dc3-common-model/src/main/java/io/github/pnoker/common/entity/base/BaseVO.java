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

package io.github.pnoker.common.entity.base;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.constant.common.TimeConstant;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base view object providing common fields.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class BaseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Primary key ID
     */
    @Schema(description = "Unique primary key of the record; required on update requests.", example = "1024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Primary key ID can't be empty", groups = {Update.class})
    private Long id;

    /**
     * Description
     */
    @Schema(description = "Free-form remark or description for the record.", example = "System auto-configured device")
    private String remark;

    /**
     * Creator ID
     */
    @Schema(description = "Identifier of the user who created the record; server-populated.", example = "1000")
    private Long creatorId;

    /**
     * Creator Name
     */
    @Schema(description = "Display name of the user who created the record; server-populated.", example = "admin")
    private String creatorName;

    /**
     * Create Time
     */
    @Schema(description = "Timestamp when the record was created; server-populated.", example = "2025-09-01 12:00:00")
    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime createTime;

    /**
     * Operator ID
     */
    @Schema(description = "Identifier of the user who last updated the record; server-populated.", example = "1000")
    private Long operatorId;

    /**
     * Operator Name
     */
    @Schema(description = "Display name of the user who last updated the record; server-populated.", example = "admin")
    private String operatorName;

    /**
     * Operate Time
     */
    @Schema(description = "Timestamp when the record was last updated; server-populated.", example = "2025-09-01 12:00:00")
    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime operateTime;

}
