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

import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.entity.bo.RuleStateBO;

import java.time.LocalDateTime;

/**
 * Applies notification policies to rule matches.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface NotifyPolicyEngine {

    /**
     * Decide whether a matched rule should notify through a channel binding.
     *
     * @param match   rule match
     * @param notify  notify policy
     * @param bind    channel binding
     * @param state   runtime state
     * @param now     decision time
     * @return decision
     */
    NotifyDecision decide(RuleMatch match, NotifyBO notify, NotifyChannelBindBO bind, RuleStateBO state,
                          LocalDateTime now);

}
