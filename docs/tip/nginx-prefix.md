### Location 语法

> location [=|~|~*|^~] /uri/ { … }

| 规则 | 说明                                     | 例子                 |
| ---- | ---------------------------------------- | -------------------- |
| =    | 精准匹配                                 | location = /api/list |
| ~    | 正则匹配（区分大小写），支持正则         | location ~ /api/     |
| ~*   | 正则匹配（**不** 区分大小写）            | location ~* /api/    |
| !~   | 正则不匹配（区分大小写）                 | location !~ /api/    |
| !~*  | 正则不匹配（**不** 区分大小写）          | location !~* /api/   |
| ^~   | 字符串匹配（区分大小写），优先级高于正则 | location ^~ /api/    |
| /    | 通用匹配                                 | location /           |



### 优先级

查找顺序和优先级

- 带有“=“的精确匹配优先
- 没有修饰符的精确匹配
- 正则表达式按照他们在配置文件中定义的顺序
- 带有“^~”修饰符的，开头匹配
- 带有“~” 或“~\*” 修饰符的，如果正则表达式与URI匹配
- 没有修饰符的，如果指定字符串与URI开头匹配



### 例子

```
server {
    listen              80;
    server_name         abc.com;
    access_log  "pipe:rollback /data/log/nginx/access.log interval=1d baknum=7 maxsize=1G"  main;

    location ^~/user/ {
        proxy_set_header Host $host;
        proxy_set_header  X-Real-IP        $remote_addr;
        proxy_set_header  X-Forwarded-For  $proxy_add_x_forwarded_for;
        proxy_set_header X-NginX-Proxy true;

        proxy_pass http://user/;
    }

    location ^~/order/ {
        proxy_set_header Host $host;
        proxy_set_header  X-Real-IP        $remote_addr;
        proxy_set_header  X-Forwarded-For  $proxy_add_x_forwarded_for;
        proxy_set_header X-NginX-Proxy true;

        proxy_pass http://order/;
    }

}
```

`^~/user/`表示匹配前缀是user的请求，`proxy_pass`的结尾有 `/`， 则会把`/user/*`后面的路径直接拼接到后面，即移除`user`.

