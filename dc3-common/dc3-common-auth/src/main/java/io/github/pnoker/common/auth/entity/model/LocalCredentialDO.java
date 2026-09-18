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
package io.github.pnoker.common.auth.entity.model;

import io.github.pnoker.common.entity.ext.JsonExt;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Persistence object for the dc3_local_credential table.
 *
 * @author pnoker
 * @since 2026.6.12
 */
@Getter
@Setter
@ToString
public class LocalCredentialDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long principalId;
    private String loginName;
    private String loginNameNormalized;
    private String credentialType;

    @ToString.Exclude
    private String passwordHash;

    private String passwordAlgorithm;
    private JsonExt passwordParams;
    private LocalDateTime passwordUpdatedTime;
    private LocalDateTime passwordExpireTime;
    private Integer failedAttempts;
    private LocalDateTime lockedUntil;
    private Byte requirePasswordChange;
    private Byte enableFlag;
    private JsonExt credentialExt;
    private String remark;
    private Long creatorId;
    private String creatorName;
    private LocalDateTime createTime;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operateTime;
    private Byte deleted;
}
