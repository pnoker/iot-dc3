package io.github.ponker.center.ekuiper.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * @author : Zhen
 */
@Data
public class RestSinkDto {

    @NotBlank
    private String format;

    private Boolean sendSingle;

    private Boolean omitIfEmpty;

    @NotBlank
    private String url;

    @NotBlank
    private String method;

    private String bodyType;

    private Map<String, String> headers;

    private String responseType;

    private Boolean insecureSkipVerify;

    @JsonProperty("timeout")
    private Integer timeout;

    @JsonProperty("debugResp")
    private Boolean debugResp;

//    @JsonProperty("resourceId")
//    private String resourceId;

}
