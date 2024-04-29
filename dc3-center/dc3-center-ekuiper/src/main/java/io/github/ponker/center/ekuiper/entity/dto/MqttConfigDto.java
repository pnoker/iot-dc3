package io.github.ponker.center.ekuiper.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author : Zhen
 */

@Data
public class MqttConfigDto {

    private Integer qos;

    @NotBlank
    private String server;

    private Boolean insecureSkipVerify;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    private String protocolVersion;

    @JsonProperty("clientid")
    private String clientid;
}
