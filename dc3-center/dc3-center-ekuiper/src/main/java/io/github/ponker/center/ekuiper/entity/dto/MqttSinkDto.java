package io.github.ponker.center.ekuiper.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author : Zhen
 */
@Data
public class MqttSinkDto {

    @NotBlank
    private String format;

    private Boolean sendSingle;

    private Boolean omitIfEmpty;


    @NotBlank
    private String server;

    @NotBlank
    private String topic;

    private Integer qos;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    private String protocolVersion;

    private Boolean insecureSkipVerify;

    @JsonProperty("retained")
    private Boolean retained;

//    @JsonProperty("resourceId")
//    private String resourceId;

}
