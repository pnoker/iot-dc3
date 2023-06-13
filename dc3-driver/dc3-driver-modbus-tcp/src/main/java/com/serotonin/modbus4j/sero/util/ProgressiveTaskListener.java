/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.serotonin.modbus4j.sero.util;

/**
 * <p>ProgressiveTaskListener interface.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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
