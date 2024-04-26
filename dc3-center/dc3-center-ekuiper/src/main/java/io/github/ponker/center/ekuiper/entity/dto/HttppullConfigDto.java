package io.github.ponker.center.ekuiper.entity.dto;


import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * @author : Zhen
 */
@Data
public class HttppullConfigDto {

    @NotBlank
    private String url;

    @NotBlank
    private String method;

    private Integer interval;

    private Integer timeout;

    private Boolean incremental;

    @NotBlank
    private String bodyType;

    private Boolean insecureSkipVerify;

    @NotBlank
    private String responseType;

    private Map<String,String> headers;

    private String body;
}
