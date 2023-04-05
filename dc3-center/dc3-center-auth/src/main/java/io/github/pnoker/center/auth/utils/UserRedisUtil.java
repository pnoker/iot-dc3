package io.github.pnoker.center.auth.utils;

import cn.hutool.core.collection.CollUtil;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户缓存工具类
 *
 * @author linys
 * @since 2023.04.02
 * <p>
 * todo 缓存需要所有微服务模块都能访问
 */
@Slf4j
@Component
public class UserRedisUtil {

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 获取redis key
     *
     * @param suffix   类型，SuffixConstant
     * @param userName 用户名称
     * @param tenantId 租户id
     * @return redis key
     */
    public String getKey(String suffix, String userName, String tenantId) {
        return PrefixConstant.USER + suffix + SymbolConstant.DOUBLE_COLON + userName +
                SymbolConstant.HASHTAG + tenantId;
    }

    /**
     * 查询redis value
     *
     * @param key key
     * @return value
     */
    public <T> T getValue(String key) {
        ValueOperations<String, T> operations = redisTemplate.opsForValue();
        return operations.get(key);
    }

    /**
     * 添加 Key 缓存,并设置失效时间
     *
     * @param key   String key
     * @param value Object
     * @param time  Time
     * @param unit  TimeUnit
     * @param <T>   Value Type
     */
    public <T> void setKey(String key, final T value, long time, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, time, unit);
    }

    /**
     * 查询Key对应的Set值
     *
     * @param key Key
     * @param <T> value
     * @return Set<T>
     */
    public <T> Set<T> getSetValue(String key) {
        SetOperations operations = redisTemplate.opsForSet();
        return operations.members(key);
    }

    /**
     * 追加Set
     *
     * @param key      Key
     * @param valueSet Value
     * @param time     Expire Time
     * @param unit     Time Unit
     * @param <T>      Type
     */
    public <T> void appendSetValue(String key, final Set<T> valueSet, long time, TimeUnit unit) {
        redisTemplate.opsForSet().add(key, valueSet.toArray());
        redisTemplate.expire(key, time, unit);
    }

    /**
     * 设置Set
     *
     * @param key      Key
     * @param valueSet Value
     * @param time     Expire Time
     * @param unit     Time Unit
     * @param <T>      Type
     */
    public <T> void setSetValue(String key, final Set<T> valueSet, long time, TimeUnit unit) {
        Set<Object> oldValueSet = getSetValue(key);
        if (CollUtil.isNotEmpty(oldValueSet)) {
            redisTemplate.opsForSet().remove(key, oldValueSet.toArray());
        }
        appendSetValue(key, valueSet, time, unit);
    }
}
