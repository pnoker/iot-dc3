package io.github.pnoker.common.agentic.tools;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.enums.DriverTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverToolTest {
    @Mock private DriverFacade driverFacade;

    @Test
    void reactiveSearchUsesCanonicalOffset() {
        FacadeDriverBO driver = driver(101L, "Virtual");
        when(driverFacade.listReactive(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.just(OffsetPage.of(List.of(driver), 10, 5, 11)));
        DriverTool tool = new DriverTool(driverFacade);
        StepVerifier.create(tool.searchDriversReactive("Virtual", 10, 5, context()))
                .assertNext(result -> assertThat(result.data().items()).containsExactly(driver)).verifyComplete();
        org.mockito.Mockito.verify(driverFacade).listReactive(org.mockito.ArgumentMatchers.argThat(query -> query.tenantId().equals(11L) && query.offset() == 10));
    }

    @Test
    void reactiveLookupNormalizesBatchIds() {
        when(driverFacade.listByIdsReactive(11L, List.of(101L, 102L))).thenReturn(Flux.just(driver(101L, "A"), driver(102L, "B")));
        DriverTool tool = new DriverTool(driverFacade);
        StepVerifier.create(tool.lookupDriversByIdsReactive(Arrays.asList(null, 101L, -1L, 101L, 102L), context()))
                .assertNext(result -> assertThat(result.data()).hasSize(2)).verifyComplete();
    }

    @Test
    void reactiveLookupRejectsInvalidId() {
        DriverTool tool = new DriverTool(driverFacade);
        StepVerifier.create(tool.lookupDriverByIdReactive(0L, context()))
                .assertNext(result -> assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_INVALID_ARGUMENT)).verifyComplete();
        org.mockito.Mockito.verifyNoInteractions(driverFacade);
    }

    private static FacadeDriverBO driver(Long id, String name) {
        FacadeDriverBO driver = new FacadeDriverBO(); driver.setId(id); driver.setDriverName(name);
        driver.setDriverCode(name.toLowerCase()); driver.setDriverTypeFlag(DriverTypeEnum.DRIVER_CLIENT);
        driver.setEnableFlag(EnableFlagEnum.ENABLE); driver.setTenantId(11L); return driver;
    }

    private static ToolContext context() {
        Map<String, Object> values = new HashMap<>(); values.put(AgenticConstant.ToolContextKey.TENANT_ID, 11L);
        values.put(AgenticConstant.ToolContextKey.USER_ID, 22L); return new ToolContext(values);
    }
}
