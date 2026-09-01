package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;

@Schema(description = "Event attribute config list request using zero-based offset pagination")
public record EventAttributeConfigOffsetRequest(
        @Schema(description = "Zero-based result offset") Long offset,
        @Schema(description = "Maximum number of records") Integer limit,
        @Schema(description = "Stable, whitelisted sort fields") List<SortSpec> sort,
        @Schema(description = "Event attribute identifier filter") Long attributeId,
        @Schema(description = "Device identifier filter") Long deviceId,
        @Schema(description = "Event identifier filter") Long eventId,
        @Schema(description = "Enable state filter") EnableFlagEnum enableFlag,
        @Schema(description = "Optimistic-lock version filter") Integer version) {
    public EventAttributeConfigOffsetRequest {
        offset = offset == null ? 0L : offset; limit = limit == null ? PageRequest.DEFAULT_LIMIT : limit; sort = sort == null ? List.of() : List.copyOf(sort);
        if (offset < 0 || limit < 1 || limit > PageRequest.MAX_LIMIT) throw new IllegalArgumentException("invalid pagination");
        Set<String> fields = Set.of("id", "attributeId", "deviceId", "eventId", "createTime", "operateTime", "version");
        if (sort.stream().anyMatch(spec -> spec == null || !fields.contains(spec.field()))) throw new IllegalArgumentException("unsupported sort field");
    }
    public EventAttributeConfigOffsetRequest() { this(0L, PageRequest.DEFAULT_LIMIT, List.of(), null, null, null, null, null); }
}
