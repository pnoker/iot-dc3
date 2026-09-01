package io.github.pnoker.common.manager.grpc;

import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.api.common.SortDirection;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;

import java.util.List;

/** Converts the wire page contract without silently repairing invalid input. */
public final class GrpcPageUtil {

    private GrpcPageUtil() {
    }

    public static io.github.pnoker.db.r2dbc.core.page.PageRequest require(PageRequest page) {
        if (page == null) {
            throw new IllegalArgumentException("page is required");
        }
        return new io.github.pnoker.db.r2dbc.core.page.PageRequest(page.getOffset(), page.getLimit(), sort(page));
    }

    public static List<SortSpec> sort(PageRequest page) {
        if (page == null) {
            throw new IllegalArgumentException("page is required");
        }
        return page.getSortList().stream().map(spec -> {
            if (spec.getField().isBlank() || spec.getDirection() == SortDirection.SORT_DIRECTION_UNSPECIFIED) {
                throw new IllegalArgumentException("sort field and direction are required");
            }
            return new SortSpec(spec.getField(), spec.getDirection() == SortDirection.SORT_DIRECTION_DESC
                    ? SortSpec.Direction.DESC
                    : SortSpec.Direction.ASC);
        }).toList();
    }
}
