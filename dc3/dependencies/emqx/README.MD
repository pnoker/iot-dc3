## CA

```bash
openssl genrsa -des3 -out ca.key 2048
pnoker
```

```bash
openssl req -new -x509 -days 3650 -key ca.key -out ca.crt
-----
Country Name (2 letter code) [AU]:CN
State or Province Name (full name) [Some-State]:BJ
Locality Name (eg, city) []:BJ
Organization Name (eg, company) [Internet Widgits Pty Ltd]:DC3
Organizational Unit Name (eg, section) []:DC3
Common Name (e.g. server FQDN or YOUR name) []:IOT
Email Address []:
```


## Server

```bash
openssl genrsa -des3 -out server.key 2048
dc3-server
```

```bash
openssl req -new -out server.csr -key server.key
-----
Country Name (2 letter code) [AU]:CN
State or Province Name (full name) [Some-State]:BJ
Locality Name (eg, city) []:BJ
Organization Name (eg, company) [Internet Widgits Pty Ltd]:DC3
Organizational Unit Name (eg, section) []:DC3
Common Name (e.g. server FQDN or YOUR name) []:IOT
Email Address []:

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:
An optional company name []:
```

```bash
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -days 3650
```


## Client

```bash
openssl genrsa -des3 -out client.key 2048
dc3-client
```

```bash
openssl req -out client.csr -key client.key -new
-----
Country Name (2 letter code) [AU]:CN
State or Province Name (full name) [Some-State]:BJ
Locality Name (eg, city) []:BJ
Organization Name (eg, company) [Internet Widgits Pty Ltd]:DC3
Organizational Unit Name (eg, section) []:DC3
Common Name (e.g. server FQDN or YOUR name) []:IOT
Email Address []:

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:pnoker
An optional company name []:
```

```bash
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt -days 3650
```

## Fx

```bash
openssl genrsa -out fx.key 2048
dc3-client
```

```bash
openssl req -out fx.csr -key fx.key -new
-----
Country Name (2 letter code) [AU]:CN
State or Province Name (full name) [Some-State]:BJ
Locality Name (eg, city) []:BJ
Organization Name (eg, company) [Internet Widgits Pty Ltd]:DC3
Organizational Unit Name (eg, section) []:DC3
Common Name (e.g. server FQDN or YOUR name) []:IOT
Email Address []:

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:
An optional company name []:
```

```bash
openssl x509 -req -in fx.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out fx.crt -days 3650
```