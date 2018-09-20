package com.pnoker.common.util.base.exception;

/**
 * @author: Pnoker
 * @email: pnokers@gmail.com
 * @project: iot-dc3
 * @copyright: Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>
 * The class Config exception.
 */
public class ConfigException extends RuntimeException {

    private static final long serialVersionUID = 6480772904575978373L;

    /**
     * Instantiates a new Config exception.
     *
     * @param message the message
     */
    public ConfigException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Config exception.
     */
    public ConfigException() {

    }
}
