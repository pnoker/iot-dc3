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
package io.github.pnoker.common.data.repository;

import reactor.core.publisher.Mono;

/** Reactive tenant-scoped reads used by the alarm rule pipeline. */
public interface ReactiveRuleStateLookup {

    /** Report whether the rule state lookup has firing state. */
    Mono<Boolean> hasFiringState(long tenantId, long ruleId, byte alarmTargetTypeFlag, long entityId);

    /** Load the firing alarm id scoped to the tenant by id. */
    Mono<Long> getFiringAlarmId(long tenantId, long ruleId, byte alarmTargetTypeFlag, long entityId);
}
