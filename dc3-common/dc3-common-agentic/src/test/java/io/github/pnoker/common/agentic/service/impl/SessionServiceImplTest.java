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
package io.github.pnoker.common.agentic.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.repository.ReactiveAttachmentStore;
import io.github.pnoker.common.agentic.repository.ReactiveSessionStore;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.db.core.page.OffsetPage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit coverage of the session service delete orchestration: the service owns the post-commit
 * attachment file cleanup (the store stays pure SQL), cleanup failures never fail the committed
 * delete, and an already-deleted session skips cleanup entirely.
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private ReactiveSessionStore sessionStore;

    @Mock
    private ReactiveAttachmentStore attachmentStore;

    @Mock
    private RequestHeader.PrincipalHeader header;

    @TempDir
    Path storageRoot;

    private AttachmentFileCleaner fileCleaner;

    private SessionServiceImpl service;

    @BeforeEach
    void setUp() {
        AgenticProperties properties = new AgenticProperties();
        properties.setAttachmentStoragePath(storageRoot.toString());
        fileCleaner = new AttachmentFileCleaner(properties);
        service = new SessionServiceImpl(sessionStore, attachmentStore, fileCleaner);
    }

    @Test
    void delegatesTouchWithoutBlocking() {
        SessionBO session = new SessionBO();
        when(sessionStore.touch("conv", null, header)).thenReturn(Mono.just(session));

        StepVerifier.create(service.touch("conv", header, null))
                .expectNext(session)
                .verifyComplete();

        verify(sessionStore).touch("conv", null, header);
    }

    @Test
    void delegatesOffsetPage() {
        OffsetPage<SessionBO> page = OffsetPage.of(List.of(new SessionBO()), 0, 10, 1);
        when(sessionStore.list(0, 10, null, null, header)).thenReturn(Mono.just(page));

        StepVerifier.create(service.list(0, 10, null, null, header))
                .expectNext(page)
                .verifyComplete();
    }

    @Test
    void deleteUnlinksSoftDeletedAttachmentFilesAfterTheStoreDeleteCommits() throws Exception {
        Path file = Files.writeString(storageRoot.resolve("tenant_1_user_2_conversation_conv-1-a.txt"), "payload");
        when(sessionStore.delete("conv", header)).thenReturn(Mono.just(1L));
        when(attachmentStore.listDeletedFilePaths("conv", header)).thenReturn(Flux.just(file.toString(), " "));

        StepVerifier.create(service.delete("conv", header)).expectNext(1L).verifyComplete();

        assertThat(Files.exists(file))
                .as("the soft-deleted attachment's file must be unlinked")
                .isFalse();
        verify(attachmentStore).listDeletedFilePaths("conv", header);
    }

    @Test
    void deleteKeepsTheCommittedResultWhenAFileIsOutsideTheStorageRoot() throws Exception {
        Path outside = Files.createTempDirectory("dc3-session-outside");
        Path escaped = Files.writeString(outside.resolve("escaped.txt"), "payload");
        when(sessionStore.delete("conv", header)).thenReturn(Mono.just(1L));
        when(attachmentStore.listDeletedFilePaths("conv", header)).thenReturn(Flux.just(escaped.toString()));

        StepVerifier.create(service.delete("conv", header)).expectNext(1L).verifyComplete();

        assertThat(Files.exists(escaped))
                .as("the boundary guard must refuse out-of-root unlinks")
                .isTrue();
    }

    @Test
    void deleteSkipsFileCleanupWhenTheSessionWasAlreadyDeleted() {
        when(sessionStore.delete("conv", header)).thenReturn(Mono.just(0L));

        StepVerifier.create(service.delete("conv", header)).expectNext(0L).verifyComplete();

        verify(attachmentStore, never()).listDeletedFilePaths(any(), any());
    }
}
