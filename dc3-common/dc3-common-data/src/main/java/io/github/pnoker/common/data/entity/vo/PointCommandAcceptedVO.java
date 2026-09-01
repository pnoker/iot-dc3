package io.github.pnoker.common.data.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resource returned when a point command is accepted for asynchronous delivery. */
@Schema(description = "Accepted point command resource")
public record PointCommandAcceptedVO(
        @Schema(description = "Command identifier used to poll command history") String commandId,
        @Schema(description = "URI of the command status resource") String statusUri) {
}
