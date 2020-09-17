package com.dc3.common.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RequestApi {
    /**
     * Request Mapping Url
     */
    private String url;

    /**
     * Request Type
     * Get,Post,Put,Delete
     */
    private String type;

    /**
     * Controller Name
     */
    private String controller;

    /**
     * Request Handler Method Name
     */
    private String method;

    /**
     * Method Param Type Array
     */
    private Class<?>[] params;
}
