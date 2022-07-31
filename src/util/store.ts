/*
 * Copyright 2022 Pnoker All Rights Reserved
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

import { isNull } from '@/util/utils'
import { decode, encode } from 'js-base64'
import Cookies from 'js-cookie'

export function getCookies(key) {
    return JSON.parse(decode(Cookies.get(key)))
}

export function setCookies(key, value) {
    return Cookies.set(key, encode(JSON.stringify(value)))
}

export function removeCookies(key) {
    return Cookies.remove(key)
}

export const setStore = (key, value, isSession) => {
    const obj = {
        dataType: typeof value,
        content: value,
        type: isSession,
        datetime: new Date().getTime(),
    }
    if (isSession) window.sessionStorage.setItem(key, encode(JSON.stringify(obj)))
    else window.localStorage.setItem(key, encode(JSON.stringify(obj)))
}

export const getStore = (key: string, debug: boolean) => {
    let obj: any, content: any
    obj = window.localStorage.getItem(key)
    if (isNull(obj)) obj = window.sessionStorage.getItem(key)
    if (isNull(obj)) return
    try {
        obj = JSON.parse(decode(obj))
    } catch {
        return obj
    }
    if (debug) {
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

export const removeStore = (key, isSession) => {
    if (isSession) {
        window.sessionStorage.removeItem(key)
    } else {
        window.localStorage.removeItem(key)
    }
}

export const getAllStore = (isSession) => {
    const list = [] as Array<{ name: string | null; content: any }>
    if (isSession) {
        for (let i = 0; i <= window.sessionStorage.length; i++) {
            list.push({
                name: window.sessionStorage.key(i),
                content: getStore(window.sessionStorage.key(i) || '', false),
            })
        }
    } else {
        for (let i = 0; i <= window.localStorage.length; i++) {
            list.push({
                name: window.localStorage.key(i),
                content: getStore(window.localStorage.key(i) || '', false),
            })
        }
    }
    return list
}

export const clearAllStore = (isSession) => {
    if (isSession) {
        window.sessionStorage.clear()
    } else {
        window.localStorage.clear()
    }
}
