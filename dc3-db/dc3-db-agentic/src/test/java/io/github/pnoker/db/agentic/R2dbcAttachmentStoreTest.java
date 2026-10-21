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

import io.github.pnoker.common.agentic.entity.bo.AttachmentBO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.db.core.dialect.StandardR2dbcDialect;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/**
 * Regression coverage for the attachment save guard and the per-tenant quota aggregates: the
 * insert runs in one transaction behind a live-session row lock, so a save racing a concurrent
 * session delete either lands before the delete or fails with a clear error, never surviving as
 * a zombie row of a soft-deleted conversation. Timestamps bind as {@link Instant} so TIMESTAMPTZ
 * encoding cannot drift. The count/byte aggregates span the whole tenant (every user), exclude
 * soft-deleted rows, and return zero for tenants without live attachments.
 */
class R2dbcAttachmentStoreTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 42L;
    private static final String CONVERSATION_ID = "conv-1";

    private final ScriptedAgenticDatabase database = new ScriptedAgenticDatabase();
    private final R2dbcAttachmentStore store = new R2dbcAttachmentStore(
            database,
            ScriptedAgenticDatabase.passThroughOperator(),
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
    void saveInsertsBehindTheLiveSessionGuardWhenTheSessionIsLive() {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Live", "{}", false);

        StepVerifier.create(store.save(attachment("/store/note.txt")))
                .assertNext(saved -> {
                    assertThat(saved.getConversationId()).isEqualTo(CONVERSATION_ID);
                    assertThat(saved.getFilePath()).isEqualTo("/store/note.txt");
                })
                .verifyComplete();

        int guardIndex = database.indexOfFirst("SELECT id FROM dc3_agentic.dc3_session");
        int insertIndex = database.indexOfFirst("INSERT INTO dc3_agentic.dc3_attachment");
        assertThat(guardIndex)
                .as("the live-session guard must run before the insert in the same transaction")
                .isGreaterThanOrEqualTo(0)
                .isLessThan(insertIndex);
    }

    @Test
    void saveFailsClosedWhenTheSessionIsASoftDeletedTombstone() {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Gone", "{}", true);

        StepVerifier.create(store.save(attachment("/store/note.txt")))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalStateException.class);
                    assertThat(error.getMessage()).contains("session must exist");
                })
                .verify();

        assertThat(database.countMatching("INSERT INTO dc3_agentic.dc3_attachment"))
                .as("a tombstoned session must never accept a new attachment row")
                .isZero();
    }

    @Test
    void saveFailsClosedWhenTheSessionDoesNotExist() {
        StepVerifier.create(store.save(attachment("/store/note.txt")))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(IllegalStateException.class))
                .verify();

        assertThat(database.countMatching("INSERT INTO dc3_agentic.dc3_attachment"))
                .isZero();
    }

    @Test
    void saveLosesTheRaceAgainstAConcurrentSessionDelete() throws Exception {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Doomed", "{}", false);
        // Park the delete's first child soft-delete so the upload's guard subscribes while the
        // delete transaction holds the session row lock.
        ScriptedAgenticDatabase.Hold deleteHold = database.holdNext("UPDATE dc3_agentic.dc3_message");

        R2dbcSessionStore sessionStore = sessionStore();
        CompletableFuture<Long> delete =
                sessionStore.delete(CONVERSATION_ID, header).toFuture();
        deleteHold.awaitRouted();

        CompletableFuture<AttachmentBO> upload =
                store.save(attachment("/store/late.txt")).toFuture();
        assertThat(upload.isDone())
                .as("the upload guard must wait on the session row lock held by the delete")
                .isFalse();

        deleteHold.release();
        assertThat(delete.get(5, TimeUnit.SECONDS)).isEqualTo(1L);
        assertThat(upload)
                .as("the upload must fail once the delete committed, instead of surviving it")
                .isCompletedExceptionally();

        assertThat(database.countMatching("INSERT INTO dc3_agentic.dc3_attachment"))
                .as("no attachment row may outlive the session delete")
                .isZero();
        assertThat(database.session(TENANT_ID, USER_ID, CONVERSATION_ID).deleted)
                .isOne();
    }

    @Test
    void listDeletedFilePathsStreamsOnlySoftDeletedRows() {
        database.seedAttachment(TENANT_ID, USER_ID, CONVERSATION_ID, "/store/gone.txt", true);
        database.seedAttachment(TENANT_ID, USER_ID, CONVERSATION_ID, "/store/live.txt", false);

        StepVerifier.create(store.listDeletedFilePaths(CONVERSATION_ID, header))
                .expectNext("/store/gone.txt")
                .verifyComplete();
    }

    @Test
    void tenantAggregatesCountEveryLiveRowOfTheTenantRegardlessOfUser() {
        database.seedAttachment(TENANT_ID, USER_ID, CONVERSATION_ID, "/store/live-1.txt", 11L, false);
        // Quotas are per tenant: a colleague's upload counts against the same budget.
        database.seedAttachment(TENANT_ID, 43L, "conv-other-user", "/store/live-2.txt", 22L, false);
        // Soft-deleted rows release their budget; other tenants never touch it.
        database.seedAttachment(TENANT_ID, USER_ID, CONVERSATION_ID, "/store/gone.txt", 33L, true);
        database.seedAttachment(999L, 43L, "conv-other-tenant", "/store/other-tenant.txt", 44L, false);

        StepVerifier.create(store.countByTenant(header)).expectNext(2L).verifyComplete();
        StepVerifier.create(store.sumSizeByTenant(header)).expectNext(33L).verifyComplete();
    }

    @Test
    void tenantAggregatesReturnZeroForATenantWithoutLiveAttachments() {
        database.seedAttachment(999L, USER_ID, "conv-elsewhere", "/store/other.txt", 44L, false);

        StepVerifier.create(store.countByTenant(header)).expectNext(0L).verifyComplete();
        StepVerifier.create(store.sumSizeByTenant(header)).expectNext(0L).verifyComplete();
    }

    @Test
    void saveBindsTimestampsAsInstantsSoEncodingIsTimezoneIndependent() {
        database.seedSession(TENANT_ID, USER_ID, CONVERSATION_ID, "Live", "{}", false);

        StepVerifier.create(store.save(attachment("/store/note.txt")))
                .expectNextCount(1)
                .verifyComplete();

        ScriptedAgenticDatabase.Execution insert = database.firstMatching("INSERT INTO dc3_agentic.dc3_attachment");
        assertThat(insert.binds().get("create_time"))
                .as("TIMESTAMPTZ binds must carry an explicit instant, not a zone-less wall clock")
                .isInstanceOf(Instant.class);
        assertThat(insert.binds().get("operate_time")).isInstanceOf(Instant.class);
    }

    private R2dbcSessionStore sessionStore() {
        return new R2dbcSessionStore(
                database,
                ScriptedAgenticDatabase.passThroughOperator(),
                org.mockito.Mockito.mock(io.github.pnoker.db.core.transaction.PageTransaction.class),
                new tools.jackson.databind.ObjectMapper(),
                new StandardR2dbcDialect("postgres", "public.dc3_schema_fingerprint"));
    }

    private AttachmentBO attachment(String path) {
        AttachmentBO attachment = new AttachmentBO();
        attachment.setConversationId(CONVERSATION_ID);
        attachment.setFileName("note.txt");
        attachment.setContentType("text/plain");
        attachment.setSize(7L);
        attachment.setFilePath(path);
        attachment.setTenantId(TENANT_ID);
        attachment.setUserId(USER_ID);
        attachment.setCreatorId(USER_ID);
        attachment.setCreatorName("operator");
        attachment.setOperatorId(USER_ID);
        attachment.setOperatorName("operator");
        return attachment;
    }
}
