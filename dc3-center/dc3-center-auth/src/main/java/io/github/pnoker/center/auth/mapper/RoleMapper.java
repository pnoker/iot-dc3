package io.github.pnoker.center.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.pnoker.common.model.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * Role Mapper
 *
 * @author linys
 * @since 2023.04.02
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
