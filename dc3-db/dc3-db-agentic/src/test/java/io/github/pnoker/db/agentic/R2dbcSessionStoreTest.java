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

import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.model.SessionExt;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.db.core.dialect.StandardR2dbcDialect;
import io.github.pnoker.db.core.transaction.PageTransaction;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

/**
 * Regression coverage for the session store concurrency contract: soft-deleted conversation ids
 * revive instead of failing on the natural-key constraint, concurrent updates merge under the
 * row lock instead of losing writes, the delete locks the session row before touching children,
 * and every timestamp bind is an {@link Instant} so TIMESTAMPTZ encoding cannot drift with the
 * database session timezone.
 */
class R2dbcSessionStoreTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 42L;
    private static final String CONVERSATION_ID = "conv-1";

    private final ScriptedAgenticDatabase database = new ScriptedAgenticDatabase();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final R2dbcSessionStore store = new R2dbcSessionStore(
            database,
            ScriptedAgenticDatabase.passThroughOperator(),
            Mockito.mock(PageTransaction.class),
            objectMapper,
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
    void touchRevivesASoftDeletedConversationAsAFreshSession() {
        database.seedSession(
                TENANT_ID, USER_ID, CONVERSATION_ID, "Old Title", "{\"model\":\"dead\",\"temperature\":0.1}", true);
        SessionExt incoming = new SessionExt();
        incoming.setModel("fresh-model");

        StepVerifier.create(store.touch(CONVERSATION_ID, incoming, header))
                .assertNext(session -> {
                    assertThat(session.getTitle()).isEqualTo("New Conversation");
                    assertThat(session.getSessionExt().getModel()).isEqualTo("fresh-model");
                    assertThat(session.getSessionExt().getTemperature())
                            .as("the tombstone's dead preferences must not leak into the revived session")
                            .isNull();
                })
                .verifyComplete();

        ScriptedAgenticDatabase.SessionRow row = database.session(TENANT_ID, USER_ID, CONVERSATION_ID);
        assertThat(row.deleted).as("the tombstone row must be live again").isZero();
        assertThat(row.title).isEqualTo("New Conversation");
        assertThat((Instant) row.createTime)
                .as("revive restarts the conversation lifecycle")
                .isAfter(Instant.parse("2026-01-02T00:00:00Z"));
        assertThat(database.countMatching("INSERT INTO dc3_agentic.dc3_session"))
                .as("revive must not attempt an insert against the unique natural key")
                .isZero();
    }

    @Test
    void touchMergesIntoTheLiveSessionUnderLock() {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Keep", "{\"temperature\":0.5}", false);
        SessionExt incoming = new SessionExt();
        incoming.setModel("m1");

        StepVerifier.create(store.touch(CONVERSATION_ID, incoming, header))
                .assertNext(session -> {
                    assertThat(session.getSessionExt().getModel()).isEqualTo("m1");
                    assertThat(session.getSessionExt().getTemperature()).isEqualTo(0.5);
                    assertThat(session.getTitle()).isEqualTo("Keep");
                })
                .verifyComplete();

        assertThat(database.firstMatching("LIMIT 1 FOR UPDATE").sql())
                .as("the decision must read the row under FOR UPDATE without a deleted filter")
                .doesNotContain("deleted = 0");
    }

    @Test
    void touchRecoversWhenAConcurrentInsertWinsTheConversation() {
        database.scriptNextSessionInsertViolation(() ->
                database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Winner", "{\"temperature\":0.7}", false));
        SessionExt incoming = new SessionExt();
        incoming.setModel("mine");

        StepVerifier.create(store.touch(CONVERSATION_ID, incoming, header))
                .assertNext(session -> {
                    assertThat(session.getSessionExt().getModel()).isEqualTo("mine");
                    assertThat(session.getSessionExt().getTemperature())
                            .as("the retry merges onto the concurrent winner's state")
                            .isEqualTo(0.7);
                })
                .verifyComplete();

        assertThat(database.session(TENANT_ID, USER_ID, CONVERSATION_ID).deleted)
                .isZero();
    }

    @Test
    void concurrentUpdatesMergeInsteadOfLosingTheFirstWrite() throws Exception {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Base", "{\"temperature\":0.5}", false);
        // Park the first updater's session UPDATE mid-flight so the second updater subscribes
        // while the first is still inside its locked read-merge-write window.
        ScriptedAgenticDatabase.Hold firstUpdateHold = database.holdNext("UPDATE dc3_agentic.dc3_session");

        SessionExt temperatureChange = new SessionExt();
        temperatureChange.setTemperature(0.9);
        CompletableFuture<SessionBO> first = store.update(CONVERSATION_ID, temperatureChange, "First Title", header)
                .toFuture();
        firstUpdateHold.awaitRouted();

        SessionExt modelChange = new SessionExt();
        modelChange.setModel("m2");
        CompletableFuture<SessionBO> second =
                store.update(CONVERSATION_ID, modelChange, null, header).toFuture();
        assertThat(second.isDone())
                .as("the second updater must wait on the row lock held by the first")
                .isFalse();

        firstUpdateHold.release();
        SessionBO firstResult = first.get(5, TimeUnit.SECONDS);
        SessionBO secondResult = second.get(5, TimeUnit.SECONDS);

        assertThat(firstResult.getSessionExt().getTemperature()).isEqualTo(0.9);
        assertThat(secondResult.getSessionExt().getModel()).isEqualTo("m2");
        assertThat(secondResult.getSessionExt().getTemperature())
                .as("the second update must merge over the first update's committed state, not lose it")
                .isEqualTo(0.9);
        assertThat(secondResult.getTitle())
                .as("a blank title keeps the first updater's rename")
                .isEqualTo("First Title");
    }

    @Test
    void updateRunsTheLockMergeWriteSequenceInsideOneTransaction() {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Base", "{}", false);
        StepVerifier.create(store.update(CONVERSATION_ID, null, "Renamed", header))
                .assertNext(session -> assertThat(session.getTitle()).isEqualTo("Renamed"))
                .verifyComplete();

        assertThat(database.indexOfFirst("deleted = 0 LIMIT 1 FOR UPDATE"))
                .as("the update path must open with a locking read of the live row")
                .isZero();
        assertThat(database.firstMatching("UPDATE dc3_agentic.dc3_session").binds())
                .containsEntry("title", "Renamed");
    }

    @Test
    void updateCompletesEmptyWhenTheSessionDoesNotExist() {
        StepVerifier.create(store.update("missing", null, "Renamed", header)).verifyComplete();
        assertThat(database.countMatching("UPDATE dc3_agentic.dc3_session")).isZero();
    }

    @Test
    void deleteLocksTheLiveSessionBeforeSoftDeletingChildren() {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Doomed", "{}", false);
        database.seedAttachment(TENANT_ID, USER_ID, CONVERSATION_ID, "/store/a.txt", false);
        database.seedAttachment(TENANT_ID, USER_ID, CONVERSATION_ID, "/store/b.txt", false);

        StepVerifier.create(store.delete(CONVERSATION_ID, header))
                .expectNext(1L)
                .verifyComplete();

        assertThat(database.indexOfFirst("SELECT id FROM dc3_agentic.dc3_session"))
                .as("the delete must lock the session row first so uploads serialize against it")
                .isZero();
        assertThat(database.session(TENANT_ID, USER_ID, CONVERSATION_ID).deleted)
                .isOne();
        assertThat(database.attachments())
                .as("the delete must soft-delete the conversation's attachments")
                .allSatisfy(row -> assertThat(row.deleted).isOne());
        List<ScriptedAgenticDatabase.Execution> statements = database.executions();
        assertThat(statements.get(statements.size() - 1).sql())
                .as("the session row must be soft-deleted after the children")
                .contains("UPDATE dc3_agentic.dc3_session SET deleted = 1");
    }

    @Test
    void deleteIsANoOpWhenTheSessionIsAlreadyDeleted() {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Gone", "{}", true);

        StepVerifier.create(store.delete(CONVERSATION_ID, header))
                .expectNext(0L)
                .verifyComplete();

        assertThat(database.countMatching("UPDATE dc3_agentic.dc3_session")).isZero();
        assertThat(database.countMatching("UPDATE dc3_agentic.dc3_message")).isZero();
    }

    @Test
    void touchBindsTimestampsAsInstantsSoEncodingIsTimezoneIndependent() {
        StepVerifier.create(store.touch(CONVERSATION_ID, null, header))
                .expectNextCount(1)
                .verifyComplete();

        ScriptedAgenticDatabase.Execution insert = database.firstMatching("INSERT INTO dc3_agentic.dc3_session");
        assertThat(insert.binds().get("create_time"))
                .as("TIMESTAMPTZ binds must carry an explicit instant, not a zone-less wall clock")
                .isInstanceOf(Instant.class);
        assertThat(insert.binds().get("operate_time")).isInstanceOf(Instant.class);
    }

    @Test
    void reviveAndDeleteBindsTimestampsAsInstants() {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Old", "{}", true);
        StepVerifier.create(store.touch(CONVERSATION_ID, null, header))
                .expectNextCount(1)
                .verifyComplete();
        assertThat(database.firstMatching("SET deleted = 0").binds().get("create_time"))
                .isInstanceOf(Instant.class);

        StepVerifier.create(store.delete(CONVERSATION_ID, header))
                .expectNext(1L)
                .verifyComplete();
        assertThat(database.firstMatching("UPDATE dc3_agentic.dc3_session SET deleted = 1")
                        .binds()
                        .get("operate_time"))
                .isInstanceOf(Instant.class);
    }

    @Test
    void getNormalizesNonUtcDriverTimesToUtcWallClock() {
        database.seedSessionWithOffsetTimes(TENANT_ID, USER_ID, CONVERSATION_ID);

        StepVerifier.create(store.get(CONVERSATION_ID, header))
                .assertNext(session -> {
                    assertThat(session.getCreateTime())
                            .as("an offset driver decode must normalize to the UTC wall clock")
                            .isEqualTo(java.time.LocalDateTime.parse("2026-03-01T04:00"));
                    assertThat(session.getOperateTime()).isEqualTo(java.time.LocalDateTime.parse("2026-03-01T04:00"));
                })
                .verifyComplete();
    }
}
