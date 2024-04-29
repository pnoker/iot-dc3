package io.github.ponker.center.ekuiper.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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
