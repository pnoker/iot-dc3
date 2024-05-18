/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { isNull } from '@/utils/utils'
import { decode, encode } from 'js-base64'
import Cookies from 'js-cookie'

/**
 * 获取 Cookies 值
 *
 * @param key key
 */
export const getCookies = (key: string) => {
    const cookieString = Cookies.get(key) as string | ''
    return JSON.parse(decode(cookieString))
}

/**
 * 更新、新增 Cookies 值
 *
 * @param key key
 * @param value cookies
 */
export const setCookies = (key: string, value: any) => {
    return Cookies.set(key, encode(JSON.stringify(value)))
}

/**
 * 删除 Cookies 值
 *
 * @param key key
 */
export const removeCookies = (key: string) => {
    return Cookies.remove(key)
}

/**
 * 获取 Storage 值
 *
 * @param key key
 * @param isSession 是否为 Session Storage，默认 false: Local Storage
 */
export const getStorage = (key: string, isSession?: boolean) => {
    let obj: any, content: any
    if (isSession) obj = window.sessionStorage.getItem(key)
    else obj = window.localStorage.getItem(key)
    if (isNull(obj)) return

    try {
        obj = JSON.parse(decode(obj))
    } catch {
        return obj
    }

    if (obj.dataType === 'string') {
        content = obj.content
    } else if (obj.dataType === 'number') {
        content = Number(obj.content)
    } else if (obj.dataType === 'boolean') {
        content = eval(obj.content)
    } else if (obj.dataType === 'object') {
        content = obj.content
    }
    return content
}

/**
 * 更新、新增 Storage 值
 *
 * @param key key
 * @param value storage
 * @param isSession 是否为 Session Storage，默认 false: Local Storage
 */
export const setStorage = (key: string, value: any, isSession?: boolean) => {
    const obj = {
        dataType: typeof value,
        content: value,
        type: isSession,
        datetime: new Date().getTime()
    }
    if (isSession) window.sessionStorage.setItem(key, encode(JSON.stringify(obj)))
    else window.localStorage.setItem(key, encode(JSON.stringify(obj)))
}

/**
 * 删除 Storage 值
 *
 * @param key key
 * @param isSession 是否为 Session Storage，默认 false: Local Storage
 */
export const removeStorage = (key: string, isSession?: boolean) => {
    if (isSession) window.sessionStorage.removeItem(key)
    else window.localStorage.removeItem(key)
}

/**
 * 获取全部的 Storage 值
 *
 * @param isSession 是否为 Session Storage，默认 false: Local Storage
 */
export const getAllStorage = (isSession?: boolean) => {
    const list = [] as Array<{ name: string | null; content: any }>
    if (isSession) {
        for (let i = 0; i <= window.sessionStorage.length; i++) {
            list.push({
                name: window.sessionStorage.key(i),
                content: getStorage(window.sessionStorage.key(i) || '')
            })
        }
    } else {
        for (let i = 0; i <= window.localStorage.length; i++) {
            list.push({
                name: window.localStorage.key(i),
                content: getStorage(window.localStorage.key(i) || '')
            })
        }
    }
    return list
}

/**
 * 删除全部的 Storage 值
 *
 * @param isSession 是否为 Session Storage，默认 false: Local Storage
 */
export const clearAllStorage = (isSession?: boolean) => {
    if (isSession) {
        window.sessionStorage.clear()
    } else {
        window.localStorage.clear()
    }
}
