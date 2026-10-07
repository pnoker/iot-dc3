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
 * Command history business object carried across the facade boundary. Fields are trimmed to
 * what an agent needs: identifiers, status, error details and timing. Times are ISO-8601
 * strings; param/result values stay as their JSON payload strings.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacadeCommandHistoryBO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String recordId;

    private String deviceId;

    private String commandId;

    private String commandCode;

    private String paramValues;

    private String resultValues;

    private String status;

    private String errorCode;

    private String errorMessage;

    private String source;

    private String sourceUserId;

    private String occurTime;

    private String sendTime;

    private String finishTime;

    private String expireTime;
}
