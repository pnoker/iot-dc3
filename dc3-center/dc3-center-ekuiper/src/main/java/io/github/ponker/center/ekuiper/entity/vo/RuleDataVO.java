package io.github.ponker.center.ekuiper.entity.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * @author : Zhen
 */
@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleDataVO {

    @NotBlank
    private String id;

    private String name;

    @NotBlank
    private String status;
}
