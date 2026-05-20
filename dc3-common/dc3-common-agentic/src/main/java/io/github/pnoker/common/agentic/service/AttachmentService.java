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
package io.github.pnoker.common.agentic.service;

import io.github.pnoker.common.agentic.entity.bo.AttachmentBO;
import io.github.pnoker.common.entity.common.RequestHeader;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.List;


/**
 * Service for uploading and summarizing conversation attachments.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface AttachmentService {

    Mono<AttachmentBO> upload(String conversationId, FilePart filePart, RequestHeader.UserHeader header);

    List<AttachmentBO> list(String conversationId, RequestHeader.UserHeader header);

    String summarize(List<Long> attachmentIds, RequestHeader.UserHeader header);

}
