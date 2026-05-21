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

package io.github.pnoker.common.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Async notification dispatch payload published on the {@code
 * dc3.q.notify.task} queue. Carries the rendered payload and the row id of a
 * PENDING {@code dc3_notify_history}; the consumer (NotifyWorker) sends it
 * through the channel adapter and updates that row with the result.
 *
 * <p>The worker resolves channel-level secrets fresh on consumption rather
 * than embedding them on the wire, so the queue itself never carries
 * webhook tokens or signing secrets.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.21
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NotifyTaskDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * dc3_notify_history.id of the PENDING row to update on completion.
     */
    private Long notifyHistoryId;

    /**
     * Tenant the notification belongs to.
     */
    private Long tenantId;

    /**
     * dc3_notify_channel.id used to look up credentials and adapter routing.
     */
    private Long channelId;

    /**
     * Channel type as encoded by NotifyChannelTypeFlagEnum.
     */
    private Byte channelTypeFlag;

    /**
     * Rendered payload type (corresponds to MessagePayload.payloadType, e.g.
     * "feishu-card", "webhook-json").
     */
    private String payloadType;

    /**
     * Rendered payload content keyed by template variable.
     */
    private Map<String, Object> payload;

    /**
     * Variables that the renderer could not resolve at render time. Forwarded
     * for diagnostics; the worker does not act on them.
     */
    private List<String> missingVariables;

    /**
     * Number of dispatch attempts already made on this task. Worker increments
     * this counter on requeue.
     */
    private int retryCount;

    /**
     * Timestamp the sender enqueued the task at.
     */
    private LocalDateTime createTime;

}
