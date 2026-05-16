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
package io.github.pnoker.common.agentic.service.chat;

import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tracks runtime events produced during one agentic turn.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
public class AgenticRunTrace {

    private final Queue<AgenticRunEvent> pendingEvents = new ConcurrentLinkedQueue<>();

    private final List<AgenticRunEvent> recordedEvents = Collections.synchronizedList(new ArrayList<>());

    public Queue<AgenticRunEvent> pendingEvents() {
        return pendingEvents;
    }

    public void recordPendingEvent(AgenticRunEvent event) {
        if (Objects.nonNull(event)) {
            pendingEvents.offer(event);
        }
    }

    public List<AgenticRunEvent> drainPendingEvents() {
        List<AgenticRunEvent> drained = new ArrayList<>();
        AgenticRunEvent event = pendingEvents.poll();
        while (Objects.nonNull(event)) {
            drained.add(event);
            recordedEvents.add(event);
            event = pendingEvents.poll();
        }
        return drained;
    }

    public List<AgenticRunEvent> recordedEvents() {
        synchronized (recordedEvents) {
            return List.copyOf(recordedEvents);
        }
    }

    public List<AgenticRunEvent> drainAndRecordedEvents() {
        drainPendingEvents();
        return recordedEvents();
    }

}
