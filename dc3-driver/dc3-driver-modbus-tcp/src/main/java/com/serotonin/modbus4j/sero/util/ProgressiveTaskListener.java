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

package com.serotonin.modbus4j.sero.util;

/**
 * <p>ProgressiveTaskListener interface.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public interface ProgressiveTaskListener {
    /**
     * Optionally called occasionally by the task to declare the progress that has been made.
     *
     * @param progress float between 0 and 1 where 0 is no progress and 1 is completed.
     */
    void progressUpdate(float progress);

    /**
     * Notification that the task has been cancelled. Should only be called once for the task.
     */
    void taskCancelled();

    /**
     * Notification that the task has been completed. Should only be called once for the task.
     */
    void taskCompleted();
}
