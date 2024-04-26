package io.github.ponker.center.ekuiper.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author : Zhen
 */
@Data
public class RedisConfigDto {

    @NotBlank
    private String dataType;

    @NotBlank
    private String addr;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;
}
