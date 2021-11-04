import {Message, Notification} from 'element-ui';

let failNotify = true, failNotifyText = '';

/**
 *  向剪切板写入指定内容
 *
 * @param content
 * @param detail
 * @param message
 * @returns {boolean}
 */
export const setCopyContent = (content, detail, message) => {
    const input = document.createElement('input');
    input.setAttribute('id', 'copy-id-input');
    input.setAttribute('value', content);
    document.body.appendChild(input);

    input.select();
    if (document.execCommand('copy')) {
        let tip = '已复制该内容到剪切板！';
        if (detail) {
            if (message) {
                tip = `已复制 ${message} 到剪切板！`;
            } else {
                tip = `已复制 ${content} 到剪切板！`;
            }
        }
        Message.success({message: tip, center: true});
    }

    document.body.removeChild(document.getElementById('copy-id-input'));
};

/**
 * 表单序列化
 *
 * @param data
 * @returns {string}
 */
export const serialize = data => {
    let list = [];
    Object.keys(data).forEach(ele => {
        list.push(`${ele}=${data[ele]}`)
    });
    return list.join('&');
};

/**
 * 获取对象类型
 *
 * @param obj
 * @returns {string|*}
 */
export const getObjType = obj => {
    let toString = Object.prototype.toString;
    let map = {
        '[object Boolean]': 'boolean',
        '[object Number]': 'number',
        '[object String]': 'string',
        '[object Function]': 'function',
        '[object Array]': 'array',
        '[object Date]': 'date',
        '[object RegExp]': 'regExp',
        '[object Undefined]': 'undefined',
        '[object Null]': 'null',
        '[object Object]': 'object'
    };
    if (obj instanceof Element) {
        return 'element';
    }
    return map[toString.call(obj)];
};

/**
 * 对象深拷贝
 *
 * @param data
 * @returns {{}|*}
 */
export const deepClone = data => {
    let type = getObjType(data);
    let obj;
    if (type === 'array') {
        obj = [];
    } else if (type === 'object') {
        obj = {};
    } else {
        //不再具有下一层次
        return data;
    }
    if (type === 'array') {
        for (let i = 0, len = data.length; i < len; i++) {
            obj.push(deepClone(data[i]));
        }
    } else if (type === 'object') {
        for (let key in data) {
            obj[key] = deepClone(data[key]);
        }
    }
    return obj;
};

/**
 * 设置灰度模式
 *
 * @param status
 */
export const toggleGrayMode = (status) => {
    if (status) {
        document.body.className = document.body.className + ' grayMode';
    } else {
        document.body.className = document.body.className.replace(' grayMode', '');
    }
};

/**
 * 设置主题
 *
 * @param name
 */
export const setTheme = (name) => {
    document.body.className = name;
};

/**
 * 加密处理
 *
 * @param params
 * @returns {any}
 */
export const encryption = (params) => {
    let {
        data,
        type,
        param,
        key
    } = params;
    let result = JSON.parse(JSON.stringify(data));
    if (type === 'Base64') {
        param.forEach(ele => {
            result[ele] = btoa(result[ele]);
        })
    } else if (type === 'Aes') {
        param.forEach(ele => {
            result[ele] = window.CryptoJS.AES.encrypt(result[ele], key).toString();
        })
    }
    return result;
};


/**
 * 递归寻找子类的父类
 *
 * @param menu
 * @param id
 * @returns {undefined|*}
 */
export const findParent = (menu, id) => {
    for (let i = 0; i < menu.length; i++) {
        if (menu[i].children.length !== 0) {
            for (let j = 0; j < menu[i].children.length; j++) {
                if (menu[i].children[j].id === id) {
                    return menu[i];
                } else {
                    if (menu[i].children[j].children.length !== 0) {
                        return findParent(menu[i].children[j].children, id);
                    }
                }
            }
        }
    }
};

/**
 * 动态插入css
 *
 * @param url
 */
export const loadStyle = url => {
    const link = document.createElement('link');
    link.type = 'text/css';
    link.rel = 'stylesheet';
    link.href = url;
    const head = document.getElementsByTagName('head')[0];
    head.appendChild(link);
};

/**
 * 判断2个对象属性和值是否相等
 *
 * @param obj1
 * @param obj2
 * @returns {boolean}
 */
export const diff = (obj1, obj2) => {
    delete obj1.close;
    let o1 = obj1 instanceof Object;
    let o2 = obj2 instanceof Object;
    if (!o1 || !o2) {
        return obj1 === obj2;
    }

    if (Object.keys(obj1).length !== Object.keys(obj2).length) {
        return false;
    }

    for (let attr in obj1) {
        let t1 = obj1[attr] instanceof Object;
        let t2 = obj2[attr] instanceof Object;
        if (t1 && t2) {
            return diff(obj1[attr], obj2[attr]);
        } else if (obj1[attr] !== obj2[attr]) {
            return false;
        }
    }
    return true;
};

/**
 * 根据字典的value显示label
 *
 * @param dic
 * @param value
 * @returns {string|*}
 */
export const findDicLabel = (dic, value) => {
    let result = '';
    if (isNull(dic)) return value;
    if (typeof (value) == 'string' || typeof (value) == 'number' || typeof (value) == 'boolean') {
        let index = 0;
        index = findDicIndex(dic, value);
        if (index !== -1) {
            result = dic[index].label;
        } else {
            result = value;
        }
    } else if (value instanceof Array) {
        result = [];
        let index = 0;
        value.forEach(ele => {
            index = findDicIndex(dic, ele);
            if (index !== -1) {
                result.push(dic[index].label);
            } else {
                result.push(value);
            }
        });
        result = result.toString();
    }
    return result;
};

/**
 * 根据字典的value查找对应的index
 *
 * @param dic
 * @param value
 * @returns {number}
 */
export const findDicIndex = (dic, value) => {
    for (let i = 0; i < dic.length; i++) {
        if (dic[i].value === value) {
            return i;
        }
    }
    return -1;
};

/**
 * 生成随机len位数字
 *
 * @param len
 * @param date
 * @returns {string}
 */
export const randomLenNum = (len, date) => {
    let random = '';
    random = Math.ceil(Math.random() * 100000000000000).toString().substr(0, len ? len : 4);
    if (date) random = random + Date.now();
    return random;
};

/**
 *
 * @param date1
 * @param date2
 * @returns {{leave1: number, hours: number, seconds: number, leave2: number, leave3: number, minutes: number, days: number}}
 */
export const calcDate = (date1, date2) => {
    let date3 = date2 - date1;

    let days = Math.floor(date3 / (24 * 3600 * 1000));

    let leave1 = date3 % (24 * 3600 * 1000);//计算天数后剩余的毫秒数
    let hours = Math.floor(leave1 / (3600 * 1000));

    let leave2 = leave1 % (3600 * 1000);//计算小时数后剩余的毫秒数
    let minutes = Math.floor(leave2 / (60 * 1000));

    let leave3 = leave2 % (60 * 1000);//计算分钟数后剩余的毫秒数
    let seconds = Math.round(date3 / 1000);
    return {
        leave1,
        leave2,
        leave3,
        days: days,
        hours: hours,
        minutes: minutes,
        seconds: seconds,
    }
};

/**
 * 日期格式化
 *
 * @param date
 * @returns {string}
 */
export function dateFormat(date) {
    let format = 'yyyy-MM-dd hh:mm:ss';
    if (date !== 'Invalid Date') {
        let o = {
            "M+": date.getMonth() + 1, //month
            "d+": date.getDate(), //day
            "h+": date.getHours(), //hour
            "m+": date.getMinutes(), //minute
            "s+": date.getSeconds(), //second
            "q+": Math.floor((date.getMonth() + 3) / 3), //quarter
            "S": date.getMilliseconds() //millisecond
        };
        if (/(y+)/.test(format)) format = format.replace(RegExp.$1,
            (date.getFullYear() + "").substr(4 - RegExp.$1.length));
        for (let k in o)
            if (new RegExp("(" + k + ")").test(format))
                format = format.replace(RegExp.$1,
                    RegExp.$1.length === 1 ? o[k] :
                        ("00" + o[k]).substr(("" + o[k]).length));
        return format;
    }
    return '';
}

/**
 * 打开全屏/关闭全屏
 */
export const triggerFullscreen = () => {
    if (fullscreenEnable()) {
        exitFullScreen();
    } else {
        reqFullScreen();
    }
};

/**
 * esc监听全屏
 *
 * @param callback
 */
export const listenFullscreen = (callback) => {
    function listen() {
        callback()
    }

    document.addEventListener('fullscreenchange', function () {
        listen();
    });
    document.addEventListener('mozfullscreenchange', function () {
        listen();
    });
    document.addEventListener('webkitfullscreenchange', function () {
        listen();
    });
    document.addEventListener('msfullscreenchange', function () {
        listen();
    });
};

/**
 * 浏览器判断是否全屏
 */
export const fullscreenEnable = () => {
    return document.isFullScreen || document.mozIsFullScreen || document.webkitIsFullScreen;
};

/**
 * 浏览器全屏
 */
export const reqFullScreen = () => {
    if (document.documentElement.requestFullScreen) {
        document.documentElement.requestFullScreen();
    } else if (document.documentElement.webkitRequestFullScreen) {
        document.documentElement.webkitRequestFullScreen();
    } else if (document.documentElement.mozRequestFullScreen) {
        document.documentElement.mozRequestFullScreen();
    }
};

/**
 * 浏览器退出全屏
 */
export const exitFullScreen = () => {
    if (document.documentElement.requestFullScreen) {
        document.exitFullScreen();
    } else if (document.documentElement.webkitRequestFullScreen) {
        document.webkitCancelFullScreen();
    } else if (document.documentElement.mozRequestFullScreen) {
        document.mozCancelFullScreen();
    }
};

/**
 * 打开小窗口
 *
 * @param url
 * @param title
 * @param w
 * @param h
 */
export const openWindow = (url, title, w, h) => {
    // Fixes dual-screen position                            Most browsers       Firefox
    const dualScreenLeft = window.screenLeft !== undefined ? window.screenLeft : screen.left;
    const dualScreenTop = window.screenTop !== undefined ? window.screenTop : screen.top;

    const width = window.innerWidth ? window.innerWidth : document.documentElement.clientWidth ? document.documentElement.clientWidth : screen.width;
    const height = window.innerHeight ? window.innerHeight : document.documentElement.clientHeight ? document.documentElement.clientHeight : screen.height;

    const left = ((width / 2) - (w / 2)) + dualScreenLeft;
    const top = ((height / 2) - (h / 2)) + dualScreenTop;
    const newWindow = window.open(url, title, 'toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=no, resizable=yes, copyhistory=no, width=' + w + ', height=' + h + ', top=' + top + ', left=' + left);

    // Puts focus on the newWindow
    if (window.focus) {
        newWindow.focus()
    }
};

/**
 * 判断是否为Url
 * @param url
 * @returns {boolean}
 */
export function isUrl(url) {
    return /^http[s]?:\/\/.*/.test(url);
}

/**
 * 判断是否为Email
 * @param email
 * @returns {boolean}
 */
export function isEmail(email) {
    return /^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$/.test(email);
}

/**
 * 判断是否为手机号码
 * @param phone
 * @returns {boolean}
 */
export function isPhone(phone) {
    return /^(13[0-9]|14[5|7]|15[0|1|2|3|4|5|6|7|8|9]|18[0|1|2|3|5|6|7|8|9])\\d{8}$/.test(phone);
}

/**
 * 判断是否为整数
 * @param num
 * @param type
 * @returns {boolean}
 */
export function isNum(num, type) {
    let regName = /[^\d.]/g;
    if (type == 1) {
        if (!regName.test(num)) return false;
    } else if (type == 2) {
        regName = /[^\d]/g;
        if (!regName.test(num)) return false;
    }
    return true;
}

/**
 * 判断是否为小数
 * @param num
 * @param type
 * @returns {boolean}
 */
export function isNumord(num, type) {
    let regName = /[^\d.]/g;
    if (type == 1) {
        if (!regName.test(num)) return false;
    } else if (type == 2) {
        regName = /[^\d.]/g;
        if (!regName.test(num)) return false;
    }
    return true;
}

/**
 * 判断是否为空
 * @param val
 * @returns {boolean}
 */
export function isNull(val) {
    if (typeof val == 'boolean') {
        return false;
    }
    if (typeof val == 'number') {
        return false;
    }
    if (val instanceof Array) {
        if (val.length === 0) return true;
    } else if (val instanceof Object) {
        if (JSON.stringify(val) === '{}') return true;
    } else {
        if (val === 'null' || val == null || val === 'undefined' || val === undefined || val === '') return true;
        return false;
    }
    return false;
}

/**
 * 成功操作
 *
 * @param message
 */
export const successMessage = (message) => {
    if (message === '' || message == null) {
        message = '操作成功!';
    }
    Notification.success({
        title: '成功',
        message: message,
        onClose: () => {
            failNotify = false;
            failNotifyText = '';
        }
    });
};

/**
 * 失败操作
 *
 * @param message
 * @param error
 */
export const failMessage = (message, error) => {
    let show = true;
    if (message === '' || message == null) {
        message = '操作失败!';
    }
    if (failNotify && failNotifyText === message) {
        show = false;
    }
    if (error) {
        console.error(error);
    }
    if (show) {
        failNotify = true;
        failNotifyText = message;
        Notification.error({
            title: '错误',
            dangerouslyUseHTMLString: true,
            message: `${message}`,
            onClose: () => {
                failNotify = false;
                failNotifyText = '';
            }
        });
    }
};
