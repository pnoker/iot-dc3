package io.github.pnoker.common.auth.entity.query;

import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Canonical role-resource binding request using zero-based offset pagination. */
@Schema(description = "Role-resource binding list request using zero-based offset pagination")
public record RoleResourceBindOffsetRequest(
        @Schema(description = "Zero-based result offset") Long offset,
        @Schema(description = "Maximum number of records") Integer limit,
        @Schema(description = "Stable, whitelisted sort fields") List<SortSpec> sort,
        @Schema(description = "Role identifier filter") Long roleId,
        @Schema(description = "Resource identifier filter") Long resourceId) {
    public RoleResourceBindOffsetRequest { offset=offset==null?0L:offset; limit=limit==null?PageRequest.DEFAULT_LIMIT:limit; sort=sort==null?List.of():List.copyOf(sort); if(offset<0)throw new IllegalArgumentException("offset must be non-negative"); if(limit<1||limit>PageRequest.MAX_LIMIT)throw new IllegalArgumentException("limit must be between 1 and "+PageRequest.MAX_LIMIT); }
    public RoleResourceBindOffsetRequest(){this(0L,PageRequest.DEFAULT_LIMIT,List.of(),null,null);}
}
