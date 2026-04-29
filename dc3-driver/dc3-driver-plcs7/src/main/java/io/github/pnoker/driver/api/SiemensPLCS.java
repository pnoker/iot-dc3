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

package io.github.pnoker.driver.api;

/**
 * Enum representing different models of Siemens PLC systems.
 * This enum defines the various Siemens PLC series that are supported
 * by the driver, including S7-200, S7-300, S7-400, S7-1200, and S7-1500 series.
 * Each series may have different communication characteristics and memory layouts.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public enum SiemensPLCS {
    S_200,
    S_200_SMART,
    /**
     * Non-200 series (S7-300/400/1200/1500).
     */
    S_NON_200,
    S_300,
    S_400,
    S_1200,
    S_1500,
}
