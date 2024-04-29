package io.github.ponker.center.ekuiper.exception;

import cn.hutool.core.util.StrUtil;


/**
 * 自定义   规则引擎  异常
 *
 * @author : Zhen
 */
public class EkuiperException extends RuntimeException {
    public EkuiperException(CharSequence template, Object... params) {
        super(StrUtil.format(template, params));
    }
}