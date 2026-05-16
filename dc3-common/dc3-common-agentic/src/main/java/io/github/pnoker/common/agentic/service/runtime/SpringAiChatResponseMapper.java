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
package io.github.pnoker.common.agentic.service.runtime;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Maps Spring AI chat responses into runtime-owned result objects.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Slf4j
@Component
public class SpringAiChatResponseMapper {

    public String assistantContent(ChatResponse chatResponse) {
        return Objects.nonNull(chatResponse) && Objects.nonNull(chatResponse.getResult())
                && Objects.nonNull(chatResponse.getResult().getOutput())
                        ? StringUtils.defaultString(chatResponse.getResult().getOutput().getText())
                        : "";
    }

    public String finishReasonOrNull(ChatResponse chatResponse) {
        String finishReason = Objects.nonNull(chatResponse) && Objects.nonNull(chatResponse.getResult())
                && Objects.nonNull(chatResponse.getResult().getMetadata())
                        ? chatResponse.getResult().getMetadata().getFinishReason()
                        : null;
        return StringUtils.trimToNull(finishReason);
    }

    public AgenticStreamDelta streamDelta(ChatResponse response) {
        if (Objects.isNull(response) || Objects.isNull(response.getResult())) {
            return AgenticStreamDelta.empty();
        }
        Generation generation = response.getResult();
        String content = Objects.nonNull(generation.getOutput()) ? generation.getOutput().getText() : null;
        if (log.isDebugEnabled()) {
            log.debug("Agentic stream chunk, contentLen={}", Objects.isNull(content) ? 0 : content.length());
        }
        return new AgenticStreamDelta(StringUtils.defaultString(content));
    }

}
