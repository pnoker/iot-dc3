package io.github.ponker.center.ekuiper.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author : Zhen
 */
@Data
public class RedisSinkDto {

    @NotBlank
    private String format;

    private Boolean sendSingle;

    private Boolean omitIfEmpty;

    @NotBlank
    private String addr;

    private String key;

    private String dataType;

    private Integer db;

    private Integer expiration;

    @JsonProperty("rowkindField")
    private String rowkindField;

    @JsonProperty("password")
    private String password;

    private String field;

    private String keyType;

//    @JsonProperty("resourceId")
//    private String resourceId;

}
