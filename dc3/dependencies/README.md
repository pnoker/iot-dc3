# 使用 ACME.SH 申请并安装 LET’S ENCRYPT SSL 证书

Let’s Encrypt 是一个免费的, 自动化的, 开放的证书颁发机构（CA）, 为公众的利益而运行。 它是一项由 Internet Security Research Group（ISRG）提供的服务。
acme.sh 则是实现了 acme 协议, 可以从 letsencrypt 生成免费的证书。

## 安装 ACME.SH

```bash
curl  https://get.acme.sh | sh
source ~/.bashrc
```

## 申请证书

这种方式的好处是, 你不需要任何服务器, 不需要任何公网 ip, 只需要 dns 的解析记录即可完成验证, 而且可申请泛域名证书。
坏处是, 需要配合DNS解析服务商的API使用, 否则 acme.sh 将无法自动更新证书, 每次都需要手动再次重新解析验证域名所有权。

### 配置阿里云 AccessKey

```bash
# 阿里云控制台申请 API Token, 并配置环境变量如下
# RAM 访问控制 -> 访问凭证管理 -> AccessKey
export Ali_Key="AccessKey ID"
export Ali_Secret="AccessKey Secret"
```

### 配置阿里云 DNS

```bash
acme.sh --issue --dns dns_ali -d dc3.site -d *.dc3.site
```

## 安装证书

> reloadcmd: 用于让web服务器重新加载新的证书文件, 例子中使用的是 nginx 服务器, 您也可以定义成其它服务器。

```bash
acme.sh --installcert -d dc3.site --key-file /etc/letsencrypt/live/dc3.site/dc3.site.key --fullchain-file /etc/letsencrypt/live/dc3.site/fullchain.cer --reloadcmd "service nginx force-reload"
```

## 更新证书

Let’s Encrypt 的证书有效期为3个月, 每3个月得重新申请证书。
通过 acme.sh 可以自动管理SSL证书的申请。通过上面步骤的安装后 acme.sh 会定期自动更新SSL证书。

```bash
acme.sh --renew -d dc3.site --force
```

## 取消更新

有时候你可能需要移除特定域名的自动申请, 这时候可以使用下面的命令让 acme.sh 取消对特定域名的自动续期。当然已申请的证书仍然有效, 不会失效。

```bash
acme.sh --remove -d dc3.site
```
