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

package io.github.pnoker.common.driver.entity.bean;

import io.github.pnoker.common.enums.DriverStatusEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Driver health result returned by protocol drivers.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DriverHealthState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Driver state that should be reported.
     */
    @Builder.Default
    private DriverStatusEnum status = DriverStatusEnum.ONLINE;

    public static DriverHealthState online() {
        return of(DriverStatusEnum.ONLINE);
    }

    public static DriverHealthState offline() {
        return of(DriverStatusEnum.OFFLINE);
    }

    public static DriverHealthState maintain() {
        return of(DriverStatusEnum.MAINTAIN);
    }

    public static DriverHealthState fault() {
        return of(DriverStatusEnum.FAULT);
    }

    public static DriverHealthState of(DriverStatusEnum status) {
        return DriverHealthState.builder()
                .status(status)
                .build();
    }

}
