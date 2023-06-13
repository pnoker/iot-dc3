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

package com.serotonin.modbus4j.sero.epoll;

import java.io.IOException;

/**
 * A callback interface for input streams.
 * <p>
 * NOTE: if the InputStreamEPoll instance is terminated, any running processes will be destroyed without any
 * notification to this callback.
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public interface Modbus4JInputStreamCallback {
    /**
     * Called when content is read from the input stream.
     *
     * @param buf the content that was read. This is a shared byte array. Contents can be manipulated within this call,
     *            but the array itself should not be stored beyond the call since the contents will be changed.
     * @param len the length of content that was read.
     */
    void input(byte[] buf, int len);

    /**
     * Called when the closure of the input stream is detected.
     */
    void closed();

    /**
     * Called if there is an {@link IOException} while reading input stream.
     *
     * @param e the exception that was received
     */
    void ioException(IOException e);

    /**
     * Called if the InputStreamEPoll instance was terminated while the input stream was still registered.
     */
    void terminated();
}
