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

import io.github.pnoker.common.agentic.entity.bo.ActionBO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticActionStatusEnum;
import io.github.pnoker.db.core.dialect.StandardR2dbcDialect;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

/** Regression coverage for timezone-safe timestamp binds across the action store statements. */
class R2dbcActionStoreTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 42L;
    private static final String CONVERSATION_ID = "conv-1";

    private final ScriptedAgenticDatabase database = new ScriptedAgenticDatabase();
    private final R2dbcActionStore store = new R2dbcActionStore(
            database,
            ScriptedAgenticDatabase.passThroughPageTransaction(),
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
    void createBindsEveryTimestampAsAnInstant() {
        ActionBO action = action(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));

        StepVerifier.create(store.create(action)).expectNextCount(1).verifyComplete();

        ScriptedAgenticDatabase.Execution insert = database.firstMatching("INSERT INTO dc3_agentic.dc3_action");
        assertThat(insert.binds().get("expire_time"))
                .as("TIMESTAMPTZ binds must carry an explicit instant, not a zone-less wall clock")
                .isInstanceOf(Instant.class);
        assertThat(insert.binds().get("create_time")).isInstanceOf(Instant.class);
        assertThat(insert.binds().get("operate_time")).isInstanceOf(Instant.class);
    }

    @Test
    void createBindsANullExpireTimeWithTheInstantType() {
        StepVerifier.create(store.create(action(null))).expectNextCount(1).verifyComplete();

        ScriptedAgenticDatabase.Execution insert = database.firstMatching("INSERT INTO dc3_agentic.dc3_action");
        assertThat(insert.nulls()).containsEntry("expire_time", Instant.class);
        assertThat(insert.binds()).doesNotContainKey("expire_time");
    }

    @Test
    void listPendingBindsTheExpiryWindowAsInstants() {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Live", "{}", false);

        StepVerifier.create(store.listPending(0, 10, CONVERSATION_ID, header, Instant.now()))
                .expectNextCount(1)
                .verifyComplete();

        ScriptedAgenticDatabase.Execution query = database.firstMatching("FROM dc3_agentic.dc3_action");
        assertThat(query.binds().get("now")).isInstanceOf(Instant.class);
    }

    @Test
    void claimPendingBindsOperateTimeAsAnInstant() {
        ActionBO created = action(null);
        StepVerifier.create(store.create(created)).expectNextCount(1).verifyComplete();

        StepVerifier.create(store.claimPending("action-1", header, AgenticActionStatusEnum.CONFIRMED, Instant.now()))
                .expectNextCount(1)
                .verifyComplete();

        ScriptedAgenticDatabase.Execution update = database.firstMatching("UPDATE dc3_agentic.dc3_action");
        assertThat(update.binds().get("operate_time")).isInstanceOf(Instant.class);
        assertThat(update.binds().get("now")).isInstanceOf(Instant.class);
    }

    private ActionBO action(LocalDateTime expireTime) {
        ActionBO action = new ActionBO();
        action.setActionId("action-1");
        action.setConversationId(CONVERSATION_ID);
        action.setActionType("write");
        action.setTitle("Write value");
        action.setDescription("Writes a device point value");
        action.setTenantId(TENANT_ID);
        action.setUserId(USER_ID);
        action.setExpireTime(expireTime);
        return action;
    }
}
