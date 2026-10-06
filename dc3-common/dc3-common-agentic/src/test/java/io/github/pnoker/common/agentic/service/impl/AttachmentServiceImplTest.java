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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.bo.AttachmentBO;
import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.repository.ReactiveAttachmentStore;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.exception.ServiceException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit coverage of attachment deletion and upload failure classification: row soft-delete plus
 * best-effort file unlink, the negative guarantees (cross-tenant ids fail closed as not-found,
 * unlink failures never roll back the row), and the split between client validation failures and
 * infrastructure faults on upload.
 */
@ExtendWith(MockitoExtension.class)
class AttachmentServiceImplTest {

    @Mock
    private ReactiveAttachmentStore attachmentStore;

    @Mock
    private SessionService sessionService;

    @TempDir
    Path storageRoot;

    private Path outsideRoot;

    private AttachmentServiceImpl service;

    private AttachmentFileCleaner fileCleaner;

    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() throws Exception {
        AgenticProperties properties = new AgenticProperties();
        properties.setAttachmentStoragePath(storageRoot.toString());
        fileCleaner = new AttachmentFileCleaner(properties);
        service = new AttachmentServiceImpl(attachmentStore, sessionService, fileCleaner);
        outsideRoot = Files.createTempDirectory("dc3-attachment-outside");
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
    }

    @Test
    void deleteSoftDeletesRowAndUnlinksStoredFile() throws Exception {
        Path file = storedFile(storageRoot, "tenant_1_user_2_conversation_conv-1-note.txt");
        AttachmentBO row = attachment(42L, file);
        when(attachmentStore.getById(42L, header)).thenReturn(Mono.just(row));
        when(attachmentStore.delete(42L, header)).thenReturn(Mono.just(1L));

        StepVerifier.create(service.delete(42L, header)).expectNext(1L).verifyComplete();

        assertThat(Files.exists(file)).as("stored file must be unlinked after the row is soft-deleted").isFalse();
        verify(attachmentStore).delete(42L, header);
    }

    @Test
    void deleteUnknownIdFailsClosedAsNotFoundWithoutTouchingTheRow() {
        when(attachmentStore.getById(42L, header)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(42L, header))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage()).isEqualTo("Attachment does not exist");
                })
                .verify();

        verify(attachmentStore, never()).delete(anyLong(), any());
    }

    @Test
    void deleteRejectsMissingIdAsValidationAndMissingHeaderAsServerError() {
        StepVerifier.create(service.delete(null, header))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(RequestException.class);
                    assertThat(error.getMessage()).isEqualTo("Attachment id is required");
                })
                .verify();

        StepVerifier.create(service.delete(42L, null))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalStateException.class);
                    assertThat(error.getMessage()).isEqualTo("Principal header is required");
                })
                .verify();

        verifyNoInteractions(attachmentStore);
    }

    @Test
    void deleteCommitsSoftDeleteWhenStoredFileIsAlreadyGone() {
        AttachmentBO row = attachment(
                42L, storageRoot.resolve("tenant_1_user_2_conversation_conv-1-missing.txt"));
        when(attachmentStore.getById(42L, header)).thenReturn(Mono.just(row));
        when(attachmentStore.delete(42L, header)).thenReturn(Mono.just(1L));

        StepVerifier.create(service.delete(42L, header)).expectNext(1L).verifyComplete();

        verify(attachmentStore).delete(42L, header);
    }

    @Test
    void deleteKeepsSoftDeleteWhenFilePathEscapesStorageRoot() throws Exception {
        Path outsideFile = storedFile(outsideRoot, "escaped.txt");
        AttachmentBO row = attachment(42L, outsideFile);
        when(attachmentStore.getById(42L, header)).thenReturn(Mono.just(row));
        when(attachmentStore.delete(42L, header)).thenReturn(Mono.just(1L));

        StepVerifier.create(service.delete(42L, header)).expectNext(1L).verifyComplete();

        assertThat(Files.exists(outsideFile)).as("guard must refuse to unlink outside the storage root").isTrue();
        verify(attachmentStore).delete(42L, header);
    }

    @Test
    void uploadStoresFileUnderConfiguredStorageRoot() {
        FilePart filePart = filePart("note.txt", "abc".getBytes(StandardCharsets.UTF_8));
        when(sessionService.touch("conv-1", header, null)).thenReturn(Mono.just(new SessionBO()));
        when(attachmentStore.save(any(AttachmentBO.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, AttachmentBO.class)));

        StepVerifier.create(service.upload("conv-1", filePart, header))
                .assertNext(saved -> assertThat(saved.getFilePath()).startsWith(storageRoot.toString()))
                .verifyComplete();

        ArgumentCaptor<AttachmentBO> captor = ArgumentCaptor.forClass(AttachmentBO.class);
        verify(attachmentStore).save(captor.capture());
        Path storedPath = Path.of(captor.getValue().getFilePath());
        assertThat(storedPath.startsWith(storageRoot)).as("stored file must stay inside the storage root").isTrue();
        assertThat(Files.exists(storedPath)).as("upload writes the file at the captured path").isTrue();
    }

    @Test
    void uploadWrapsInfrastructureFailuresAsServiceErrors() {
        FilePart filePart = filePart("note.txt", "abc".getBytes(StandardCharsets.UTF_8));
        when(sessionService.touch("conv-1", header, null)).thenReturn(Mono.just(new SessionBO()));
        when(attachmentStore.save(any(AttachmentBO.class)))
                .thenReturn(Mono.error(new RuntimeException("connection refused")));

        StepVerifier.create(service.upload("conv-1", filePart, header))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ServiceException.class);
                    assertThat(error.getMessage()).isEqualTo("Attachment file save failed");
                    assertThat(error.getCause()).hasMessage("connection refused");
                })
                .verify();
    }

    @Test
    void unlinkAllRemovesOnlyFilesInsideTheStorageRoot() throws Exception {
        Path inside = storedFile(storageRoot, "inside.txt");
        Path outside = storedFile(outsideRoot, "outside.txt");

        StepVerifier.create(fileCleaner.unlinkAll(List.of(inside.toString(), outside.toString(), " ")))
                .verifyComplete();

        assertThat(Files.exists(inside)).as("in-root file must be removed").isFalse();
        assertThat(Files.exists(outside)).as("out-of-root file must survive").isTrue();
    }

    private Path storedFile(Path directory, String fileName) throws Exception {
        return Files.writeString(directory.resolve(fileName), "payload");
    }

    private FilePart filePart(String fileName, byte[] content) {
        FilePart part = org.mockito.Mockito.mock(FilePart.class);
        when(part.filename()).thenReturn(fileName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(content.length);
        headers.setContentType(MediaType.TEXT_PLAIN);
        when(part.headers()).thenReturn(headers);
        when(part.content()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(content)));
        return part;
    }

    private AttachmentBO attachment(Long id, Path file) {
        AttachmentBO row = new AttachmentBO();
        row.setId(id);
        row.setConversationId("conv-1");
        row.setFileName(file.getFileName().toString());
        row.setContentType(MediaType.TEXT_PLAIN.toString());
        row.setSize(7L);
        row.setFilePath(file.toString());
        row.setTenantId(header.getTenantId());
        row.setUserId(header.getPrincipalId());
        return row;
    }
}
