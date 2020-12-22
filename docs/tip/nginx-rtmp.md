## 开发环境

* Ubuntu 
* [nginx](http://nginx.org/en/download.html)
* [nginx-rtmp-module](https://github.com/arut/nginx-rtmp-module)
* [VLC media player](https://www.videolan.org/)



## 依赖

安装依赖库

```bash
sudo apt-get update
sudo apt-get install libpcre3 libpcre3-dev openssl libssl-dev
```



## 编译

配置&编译Nginx,使用默认配置,并添加nginx-rtmp模块。

```bash
./configure --add-module=../nginx-rtmp-module
make
sudo make install
```



## Nginx 配置文件参考

```nginx
#user  nobody;
worker_processes  1;

#error_log  logs/error.log;
#error_log  logs/error.log  notice;
#error_log  logs/error.log  info;

#pid        logs/nginx.pid;

events {
    worker_connections  1024;
}

rtmp {

    server {

        listen 1935;

        chunk_size 4000;

        # TV mode: one publisher, many subscribers
        application rtmp {

            # enable live streaming
            live on;

            # record first 1K of stream
            record all;
            record_path /tmp/av;
            record_max_size 4K;

            # append current timestamp to each flv
            record_unique on;

            # publish only from localhost
            allow publish all;
            # deny publish all;

            #allow play all;
        }

        # video on demand
        application mp4 {
            play /var/mp4s;
        }
    }
}

# HTTP can be used for accessing RTMP stats
http {
    include       mime.types;
    default_type  application/octet-stream;

    #log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
    #                  '$status $body_bytes_sent "$http_referer" '
    #                  '"$http_user_agent" "$http_x_forwarded_for"';

    #access_log  logs/access.log  main;

    sendfile        on;
    #tcp_nopush     on;

    #keepalive_timeout  0;
    keepalive_timeout  65;

    #gzip  on;

    server {
        listen       80;
        server_name  localhost;

        #charset koi8-r;

        #access_log  logs/host.access.log  main;

        location / {
            root   html;
            index  index.html index.htm;
        }

        # This URL provides RTMP statistics in XML
        location /stat {
            rtmp_stat all;

            # Use this stylesheet to view XML as web page
            # in browser
            rtmp_stat_stylesheet stat.xsl;
        }

        location /stat.xsl {
            # XML stylesheet to view RTMP stats.
            # Copy stat.xsl wherever you want
            # and put the full directory path here
            root /usr/local/nginx/nginx-rtmp-module/;
        }

        #error_page  404              /404.html;

        # redirect server error pages to the static page /50x.html
        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    }
}

```