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
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * One streamed frame emitted by the model runtime.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
public record AgenticRuntimeStreamFrame(AgenticStreamDelta delta, String finishReason) {

    public AgenticRuntimeStreamFrame {
        delta = Objects.nonNull(delta) ? delta : AgenticStreamDelta.empty();
    }

    public boolean hasContent() {
        return delta.hasContent();
    }

    public boolean hasFinishReason() {
        return StringUtils.isNotBlank(finishReason);
    }

}
