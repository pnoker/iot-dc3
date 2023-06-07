## 安装 acme.sh

https://get.acme.sh | sh

## DNS-API

> 阿里云控制台中申请子账号 API Token，并配置环境变量如下

```bash
export Ali_Key="sdfsdfsdfljlbjkljlkjsdfoiwje"
export Ali_Secret="jlsdflanljkljlfdsaklkjflsa"
```

## 生成证书

```bash
acme.sh --issue --dns dns_ali -d example.com -d www.example.com
```

## 安装证书

```bash
acme.sh --installcert -d dc3.site --key-file /etc/letsencrypt/live/dc3.site/dc3.site.key --fullchain-file /etc/letsencrypt/live/dc3.site/fullchain.cer --reloadcmd "service nginx force-reload"
```