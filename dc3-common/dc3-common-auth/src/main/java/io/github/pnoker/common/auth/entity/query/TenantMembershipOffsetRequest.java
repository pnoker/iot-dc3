package io.github.pnoker.common.auth.entity.query;

import io.github.pnoker.common.enums.MembershipStatusEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Canonical tenant membership list request using zero-based offset pagination. */
@Schema(description = "Tenant membership list request using zero-based offset pagination")
public record TenantMembershipOffsetRequest(
        @Schema(description = "Zero-based result offset") Long offset,
        @Schema(description = "Maximum number of records") Integer limit,
        @Schema(description = "Stable, whitelisted sort fields") List<SortSpec> sort,
        @Schema(description = "Principal identifier filter") Long principalId,
        @Schema(description = "Principal classification filter") PrincipalTypeEnum principalType,
        @Schema(description = "Membership status filter") MembershipStatusEnum membershipStatus) {

    public TenantMembershipOffsetRequest {
        offset = offset == null ? 0L : offset;
        limit = limit == null ? PageRequest.DEFAULT_LIMIT : limit;
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        }
    }

    public TenantMembershipOffsetRequest() {
        this(0L, PageRequest.DEFAULT_LIMIT, List.of(), null, null, null);
    }
}
