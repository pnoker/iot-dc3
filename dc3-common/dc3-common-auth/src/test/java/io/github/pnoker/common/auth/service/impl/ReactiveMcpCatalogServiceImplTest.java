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
package io.github.pnoker.common.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.github.pnoker.common.auth.entity.builder.McpAuditBuilder;
import io.github.pnoker.common.auth.entity.builder.McpToolBuilder;
import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.vo.McpAuditVO;
import io.github.pnoker.common.auth.entity.vo.McpToolVO;
import io.github.pnoker.common.auth.repository.ReactiveMcpCatalogStore;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ReactiveMcpCatalogServiceImplTest {
    @Test
    void mapsToolPageWithoutLegacyRecordsEnvelope() {
        ReactiveMcpCatalogStore store = mock(ReactiveMcpCatalogStore.class);
        McpToolBuilder builder = mock(McpToolBuilder.class);
        McpAuditBuilder auditBuilder = mock(McpAuditBuilder.class);
        McpToolRecord record = new McpToolRecord();
        McpToolVO view = new McpToolVO();
        when(store.listTools("device", "LOW", new PageRequest(20, 10)))
                .thenReturn(Mono.just(OffsetPage.of(List.of(record), 20, 10, 21)));
        when(builder.buildVOByRecord(record)).thenReturn(view);

        OffsetPage<McpToolVO> page = new ReactiveMcpCatalogServiceImpl(store, builder, auditBuilder)
                .listTools("device", "LOW", new PageRequest(20, 10))
                .block();

        assertThat(page.items()).containsExactly(view);
        assertThat(page.offset()).isEqualTo(20);
        assertThat(page.limit()).isEqualTo(10);
        assertThat(page.total()).isEqualTo(21);
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void mapsAuditPageAndPreservesTenantQuery() {
        ReactiveMcpCatalogStore store = mock(ReactiveMcpCatalogStore.class);
        McpToolBuilder builder = mock(McpToolBuilder.class);
        McpAuditBuilder auditBuilder = mock(McpAuditBuilder.class);
        McpAuditCommand record = new McpAuditCommand();
        McpAuditVO view = new McpAuditVO();
        PageRequest request = new PageRequest(0, 50);
        when(store.listAudit(7L, 9L, "tool", "ERROR", "HIGH", request))
                .thenReturn(Mono.just(OffsetPage.of(List.of(record), 0, 50, 1)));
        when(auditBuilder.buildVOByRecord(record)).thenReturn(view);

        OffsetPage<McpAuditVO> page = new ReactiveMcpCatalogServiceImpl(store, builder, auditBuilder)
                .listAudit(7L, 9L, "tool", "ERROR", "HIGH", request)
                .block();

        assertThat(page.items()).containsExactly(view);
        verify(store).listAudit(7L, 9L, "tool", "ERROR", "HIGH", request);
    }
}
