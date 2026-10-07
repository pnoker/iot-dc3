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
package io.github.pnoker.common.facade.entity.bo;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event history business object carried across the facade boundary. Fields are trimmed to
 * what an agent needs: identifiers, type and level codes, message and timing. Times are
 * ISO-8601 strings; param values stay as their JSON payload string.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacadeEventHistoryBO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String recordId;

    private String deviceId;

    private String eventId;

    private String eventCode;

    private String eventTypeFlag;

    private String eventLevelFlag;

    private String paramValues;

    private String message;

    private String occurTime;

    private String receiveTime;

    private String acknowledgeFlag;
}
