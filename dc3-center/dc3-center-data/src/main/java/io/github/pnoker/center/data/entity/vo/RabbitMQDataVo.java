package io.github.pnoker.center.data.entity.vo;

import lombok.Data;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

@Data
public class RabbitMQDataVo {
    private List<Long> times;
    private List<Double> values;
    private List<Integer> ivalues;
}
