package io.github.ponker.center.ekuiper.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;


/**
 * @author : Zhen
 */
@Data
public class HttppushConfigDto {

    @NotBlank
    private String method;
}
