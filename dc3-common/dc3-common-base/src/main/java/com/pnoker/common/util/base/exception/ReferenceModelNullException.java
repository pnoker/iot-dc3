package com.pnoker.common.util.base.exception;

/**
 * @author: Pnoker
 * @email: pnokers@gmail.com
 * @project: iot-dc3
 * @copyright: Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>
 * The class Reference model null exception.
 */
public class ReferenceModelNullException extends RuntimeException {
    private static final long serialVersionUID = -318154770875589045L;

    /**
     * Instantiates a new Reference model null exception.
     *
     * @param message the message
     */
    public ReferenceModelNullException(String message) {
        super(message);
    }
}
