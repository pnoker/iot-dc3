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
package io.github.pnoker.common.agentic.entity.model;

import io.github.pnoker.common.enums.ConnectivityStatusEnum;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Internal aggregate outcome of one connectivity check run.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public record ConnectivityCheckResult(
        ConnectivityStatusEnum overall,
        ConnectivityLevelResult l1,
        ConnectivityLevelResult l2,
        List<String> dimensions,
        LocalDateTime checkedAt) {}
