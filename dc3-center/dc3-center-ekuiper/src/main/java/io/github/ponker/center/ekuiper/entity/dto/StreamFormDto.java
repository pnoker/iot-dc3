package io.github.ponker.center.ekuiper.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author : Zhen
 */
@Data
public class StreamFormDto {

    @NotBlank(message = "SQL不能为空")
    private String sql;

}
