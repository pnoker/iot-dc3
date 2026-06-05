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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.data.entity.property.AlarmWindowProperties;
import io.github.pnoker.common.enums.WindowModeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Routes a window request to either the in-memory buffer (short windows) or
 * the repository (long windows). The cutoff is configurable via
 * {@code dc3.alarm.window.local-cutoff} (default 5 minutes); rules whose
 * duration sits at or below the cutoff stay local, larger ones go to the
 * time-series store.
 *
 * <p>Marked {@code @Primary} so injection of the bare {@link WindowDataSource}
 * picks the hybrid by default. Tests inject the local / repository sources
 * directly when they want to target one side.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Component
@Primary
@RequiredArgsConstructor
public class HybridWindowDataSource implements WindowDataSource {

    private final LocalWindowDataSource localWindowDataSource;

    private final RepositoryWindowDataSource repositoryWindowDataSource;

    private final AlarmWindowProperties properties;

    @Override
    public AggregateOutcome aggregate(WindowSpec spec, RuleFact fact, WindowModeEnum mode) {
        return select(spec).aggregate(spec, fact, mode);
    }

    @Override
    public List<WindowSample> samples(WindowSpec spec, RuleFact fact) {
        return select(spec).samples(spec, fact);
    }

    private WindowDataSource select(WindowSpec spec) {
        if (Objects.isNull(spec) || Objects.isNull(spec.duration())) {
            return localWindowDataSource;
        }
        Duration cutoff = properties.getLocalCutoff();
        if (Objects.isNull(cutoff) || spec.duration().compareTo(cutoff) <= 0) {
            return localWindowDataSource;
        }
        return repositoryWindowDataSource;
    }

}
