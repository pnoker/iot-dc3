package com.pnoker.common.base.exception;

/**
 * @author: Pnoker
 * @email: pnokers@gmail.com
 * @project: iot-dc3
 * @copyright: Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>
 * The class Import exception.
 */
public class ImportException extends RuntimeException {

    private static final long serialVersionUID = -4740091660440744697L;

    /**
     * Instantiates a new Import exception.
     *
     * @param message the message
     */
    public ImportException(String message) {
        super(message);
    }
}
