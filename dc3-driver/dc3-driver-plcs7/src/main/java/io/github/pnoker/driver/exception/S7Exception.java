/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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
package io.github.pnoker.driver.exception;

/**
 * The Class S7Exception is an exception related to S7 Communication
 *
 * @author Thomas Rudin
 */
public final class S7Exception extends RuntimeException {

    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new s7 exception.
     */
    public S7Exception() {
    }

    /**
     * Instantiates a new s7 exception.
     *
     * @param message the message
     */
    public S7Exception(final String message) {
        super(message);
    }

    /**
     * Instantiates a new s7 exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public S7Exception(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new s7 exception.
     *
     * @param cause the cause
     */
    public S7Exception(final Throwable cause) {
        super(cause);
    }

}
