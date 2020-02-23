package com.pnoker.common.sdk.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author pnoker
 */
@Data
@AllArgsConstructor
public class AttributeInfo {
    @NotNull
    private String value;
    @NotNull
    private String type;
}
