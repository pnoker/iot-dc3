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
package io.github.pnoker.db.agentic;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticMessageStatusEnum;
import io.github.pnoker.db.core.dialect.StandardR2dbcDialect;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

/** Regression coverage for the message store save protocol and timezone-safe timestamp binds. */
class R2dbcMessageStoreTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 42L;
    private static final String CONVERSATION_ID = "conv-1";

    private final ScriptedAgenticDatabase database = new ScriptedAgenticDatabase();
    private final R2dbcMessageStore store = new R2dbcMessageStore(
            database,
            ScriptedAgenticDatabase.passThroughOperator(),
            new ObjectMapper(),
            new StandardR2dbcDialect("postgres", "public.dc3_schema_fingerprint"));

    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() {
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(TENANT_ID);
        header.setPrincipalId(USER_ID);
        header.setPrincipalName("operator");
    }

    @Test
    void saveRunsTheSessionGuardBeforeInsertingAndBindsInstants() {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Live", "{}", false);

        StepVerifier.create(store.save(
                        CONVERSATION_ID,
                        "user",
                        AgenticMessageContent.ofText("hello"),
                        "m1",
                        AgenticMessageStatusEnum.COMPLETED,
                        header))
                .assertNext(message -> {
                    assertThat(message.getConversationId()).isEqualTo(CONVERSATION_ID);
                    assertThat(message.getMessageIndex()).isZero();
                })
                .verifyComplete();

        int guardIndex = database.indexOfFirst("SELECT id FROM dc3_agentic.dc3_session");
        int insertIndex = database.indexOfFirst("INSERT INTO dc3_agentic.dc3_message");
        assertThat(guardIndex).isGreaterThanOrEqualTo(0).isLessThan(insertIndex);

        ScriptedAgenticDatabase.Execution insert = database.firstMatching("INSERT INTO dc3_agentic.dc3_message");
        assertThat(insert.binds().get("create_time"))
                .as("TIMESTAMPTZ binds must carry an explicit instant, not a zone-less wall clock")
                .isInstanceOf(Instant.class);
        assertThat(insert.binds().get("operate_time")).isInstanceOf(Instant.class);
    }

    @Test
    void saveFailsClosedWhenTheSessionIsMissingOrDeleted() {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Gone", "{}", true);

        StepVerifier.create(store.save(
                        CONVERSATION_ID,
                        "user",
                        AgenticMessageContent.ofText("hello"),
                        "m1",
                        AgenticMessageStatusEnum.COMPLETED,
                        header))
                .expectError(IllegalStateException.class)
                .verify();

        assertThat(database.countMatching("INSERT INTO dc3_agentic.dc3_message"))
                .isZero();
    }
}
