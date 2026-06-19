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

package io.github.pnoker.common.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
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
import java.util.List;

/**
 * Request view object for replacing an MCP connection's tool whitelist.
 *
 * @author pnoker
 * @version 2026.6.19
 * @since 2026.6.19
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "MCP connection tools replace request")
public class McpConnectionToolsReplaceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Connection id can't be empty", groups = {Update.class})
    @Schema(description = "Primary key of the MCP connection to update.", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long connectionId;

    @Schema(description = "New tool id whitelist; fully overwrites the previous one.", example = "[\"tool_read_device\"]")
    private List<String> toolIds;

}
