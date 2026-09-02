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
package io.github.pnoker.common.agentic.repository;

import io.github.pnoker.common.agentic.entity.bo.AttachmentBO;
import io.github.pnoker.common.entity.common.RequestHeader;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for attachment metadata. */
public interface ReactiveAttachmentStore {

    Mono<AttachmentBO> save(AttachmentBO attachment);

    Flux<AttachmentBO> list(String conversationId, RequestHeader.PrincipalHeader header);

    Flux<AttachmentBO> findByIds(Collection<Long> ids, RequestHeader.PrincipalHeader header);
}
