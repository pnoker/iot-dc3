package com.pnoker.common.util.core.mybatis;

import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.MySqlMapper;

/**
 * @author: Pnoker
 * @email: pnokers@gmail.com
 * @project: iot-dc3
 * @copyright: Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>
 * The interface My mapper.
 */
public interface MyMapper<T> extends Mapper<T>, MySqlMapper<T> {
}
