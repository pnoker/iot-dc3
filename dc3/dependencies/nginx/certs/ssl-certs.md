# Issue and Install Let's Encrypt SSL Certificates with acme.sh

> Note: the dc3-web image currently serves plain HTTP on port 80 only. Nginx no longer terminates SSL and no
> certificates are mounted. This document is kept as a reference; when HTTPS is needed again, follow the procedure
> below to issue a certificate and restore the `ssl_certificate` directives in the nginx configuration.

Let's Encrypt is a free, automated, and open certificate authority (CA), operated for the public benefit by the
Internet Security Research Group (ISRG). acme.sh implements the ACME protocol and issues free certificates from
Let's Encrypt.

## Install acme.sh

```bash
curl  https://get.acme.sh | sh
source ~/.bashrc
```

## Issue a certificate

The advantage of the DNS mode is that it needs no server and no public IP - only a DNS record to complete domain
validation - and it supports wildcard certificates. The drawback is that it requires the DNS provider's API;
otherwise acme.sh cannot renew certificates automatically, and domain ownership must be re-validated manually
every time.

### Configure the Aliyun AccessKey

```bash
# Create an API token in the Aliyun console, then export the credentials:
# RAM Access Control -> Access Credentials -> AccessKey
export Ali_Key="AccessKey ID"
export Ali_Secret="AccessKey Secret"
```

### Configure Aliyun DNS

```bash
acme.sh --issue --dns dns_ali -d dc3.site -d *.dc3.site
```

## Install the certificate

> reloadcmd: reloads the web server so it picks up the renewed certificate files. The example uses nginx; replace it
> with the reload command of any other server if needed.

```bash
acme.sh --installcert -d dc3.site --key-file /etc/letsencrypt/live/dc3.site/dc3.site.key --fullchain-file /etc/letsencrypt/live/dc3.site/fullchain.cer --reloadcmd "service nginx force-reload"
```

## Renew certificates

Let's Encrypt certificates are valid for 3 months and must be reissued regularly. acme.sh manages issuance
automatically: after a certificate is installed as shown above, acme.sh renews it periodically on its own.

```bash
acme.sh --renew -d dc3.site --force
```

## Cancel automatic renewal

To stop automatic renewal for a specific domain, use the command below. Certificates that were already issued remain
valid until they expire.

```bash
acme.sh --remove -d dc3.site
```
