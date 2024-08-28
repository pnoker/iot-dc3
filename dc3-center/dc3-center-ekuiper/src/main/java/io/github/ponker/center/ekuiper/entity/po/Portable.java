package io.github.ponker.center.ekuiper.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "portable")
public class Portable {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String name;
    private String describtion;
}
