package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.enums.ApiTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;

public record ApiFilter(String serviceName, ApiTypeEnum apiTypeFlag, String apiName, String apiCode,
                        String apiGroup, EnableFlagEnum enableFlag, PageRequest page) {
    public ApiFilter {
        page = page == null ? PageRequest.firstPage() : page;
        serviceName = normalize(serviceName); apiName = normalize(apiName);
        apiCode = normalize(apiCode); apiGroup = normalize(apiGroup);
    }
    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
