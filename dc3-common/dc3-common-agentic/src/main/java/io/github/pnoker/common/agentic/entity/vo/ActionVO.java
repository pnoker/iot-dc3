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
package io.github.pnoker.common.agentic.entity.vo;

import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.AgenticActionStatusEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View object for agentic action API responses.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "Action view object")
public class ActionVO extends BaseVO {

    @Schema(description = "action ID")

    private String actionId;

    @Schema(description = "conversation ID")

    private String conversationId;

    @Schema(description = "action type")

    private String actionType;

    @Schema(description = "title")

    private String title;

    @Schema(description = "Description")

    private String description;

    private Map<String, Object> payload;

    @Schema(description = "status")

    private AgenticActionStatusEnum status;

    @Schema(description = "expire time")

    private LocalDateTime expireTime;

}
