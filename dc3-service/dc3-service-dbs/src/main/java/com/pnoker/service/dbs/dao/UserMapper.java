package com.pnoker.service.dbs.dao;

import com.pnoker.common.util.core.mybatis.MyMapper;
import com.pnoker.common.util.model.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface UserMapper extends MyMapper<User> {
}