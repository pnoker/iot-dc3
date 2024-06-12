package io.github.ponker.center.ekuiper.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author : Zhen
 * @date : 2024/5/20
 */
@Data
public class WebSocketConfigDto {

    @NotBlank
    private String addr;
}
