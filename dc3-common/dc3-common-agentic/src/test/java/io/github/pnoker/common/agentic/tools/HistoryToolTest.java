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
package io.github.pnoker.common.agentic.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.facade.api.CommandHistoryFacade;
import io.github.pnoker.common.facade.api.EventHistoryFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandHistoryBO;
import io.github.pnoker.common.facade.entity.bo.FacadeEventHistoryBO;
import io.github.pnoker.db.core.page.OffsetPage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class HistoryToolTest {

    @Mock
    private CommandHistoryFacade commandHistoryFacade;

    @Mock
    private EventHistoryFacade eventHistoryFacade;

    @Test
    void commandHistoryLookupReturnsRecord() {
        FacadeCommandHistoryBO record = new FacadeCommandHistoryBO(
                "rec-1",
                "101",
                "202",
                "restart",
                null,
                "{\"ok\":true}",
                "SUCCESS",
                null,
                null,
                "AGENTtic",
                null,
                "2026-10-07T10:00:00",
                null,
                "2026-10-07T10:00:05",
                null);
        when(commandHistoryFacade.getByRecordIdReactive(11L, "rec-1")).thenReturn(Mono.just(record));

        StepVerifier.create(new HistoryTool(Optional.of(commandHistoryFacade), Optional.of(eventHistoryFacade))
                        .lookupCommandHistoryByRecordId("rec-1", toolContext()))
                .assertNext(result -> {
                    assertThat(result.success()).isTrue();
                    assertThat(result.data().getRecordId()).isEqualTo("rec-1");
                    assertThat(result.data().getResultValues()).isEqualTo("{\"ok\":true}");
                })
                .verifyComplete();
    }

    @Test
    void commandHistoryLookupRejectsBlankRecordId() {
        StepVerifier.create(new HistoryTool(Optional.of(commandHistoryFacade), Optional.of(eventHistoryFacade))
                        .lookupCommandHistoryByRecordId("  ", toolContext()))
                .assertNext(result -> {
                    assertThat(result.success()).isFalse();
                    assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_INVALID_ARGUMENT);
                })
                .verifyComplete();
        verifyNoInteractions(commandHistoryFacade);
    }

    @Test
    void commandHistorySearchReturnsPage() {
        OffsetPage<FacadeCommandHistoryBO> page = OffsetPage.of(
                List.of(new FacadeCommandHistoryBO(
                        "rec-2", "101", "202", null, null, null, "FAILED", "E1", "boom", null, null, null, null, null,
                        null)),
                0,
                10,
                1);
        when(commandHistoryFacade.listReactive(11L, "101", null, null, "FAILED", 0, 10))
                .thenReturn(Mono.just(page));

        StepVerifier.create(new HistoryTool(Optional.of(commandHistoryFacade), Optional.of(eventHistoryFacade))
                        .searchCommandHistories("101", null, null, "FAILED", 0, 10, toolContext()))
                .assertNext(result -> {
                    assertThat(result.success()).isTrue();
                    assertThat(result.data().total()).isEqualTo(1);
                    assertThat(result.data().items().getFirst().getErrorMessage())
                            .isEqualTo("boom");
                })
                .verifyComplete();
    }

    @Test
    void eventHistoryLookupReturnsRecord() {
        FacadeEventHistoryBO record = new FacadeEventHistoryBO(
                "rec-3",
                "101",
                "303",
                "EVT_CODE",
                "ALERT",
                "HIGH",
                null,
                "motor overheating",
                "2026-10-07T11:00:00",
                "2026-10-07T11:00:01",
                null);
        when(eventHistoryFacade.getByRecordIdReactive(11L, "rec-3")).thenReturn(Mono.just(record));

        StepVerifier.create(new HistoryTool(Optional.of(commandHistoryFacade), Optional.of(eventHistoryFacade))
                        .lookupEventHistoryByRecordId("rec-3", toolContext()))
                .assertNext(result -> {
                    assertThat(result.success()).isTrue();
                    assertThat(result.data().getMessage()).isEqualTo("motor overheating");
                })
                .verifyComplete();
    }

    @Test
    void historyToolsDoNotFabricateWhenFacadesAreUnavailable() {
        HistoryTool tool = new HistoryTool(Optional.empty(), Optional.empty());
        StepVerifier.create(tool.lookupCommandHistoryByRecordId("rec-1", toolContext()))
                .assertNext(result -> assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_UNAVAILABLE))
                .verifyComplete();
        StepVerifier.create(tool.searchEventHistories(null, null, 0, 10, toolContext()))
                .assertNext(result -> assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_UNAVAILABLE))
                .verifyComplete();
    }

    private ToolContext toolContext() {
        RequestHeader.PrincipalHeader header = new RequestHeader.PrincipalHeader();
        header.setTenantId(11L);
        header.setPrincipalId(22L);
        header.setPrincipalName("ops.engineer");
        header.setDisplayName("Ops Engineer");

        Map<String, Object> values = new HashMap<>();
        values.put(AgenticConstant.ToolContextKey.TENANT_ID, 11L);
        values.put(AgenticConstant.ToolContextKey.USER_ID, 22L);
        values.put(AgenticConstant.ToolContextKey.USER_HEADER, header);
        values.put(AgenticConstant.ToolContextKey.CONVERSATION_ID, "conv-1");
        return new ToolContext(values);
    }
}
