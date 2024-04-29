package io.github.ponker.center.ekuiper.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * @author : Zhen
 */
@Data
public class HttppushConfigDto {

    @NotBlank
    private String method;
}
