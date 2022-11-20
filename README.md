# pdf-sign Project

## APP

### Generate a root certificate

```bash
openssl req -sha256 -nodes -newkey rsa:2048 -keyout trodix.com.key -out trodix.com.csr
```

```bash
openssl x509 -req -sha256 -days 365 -in trodix.com.csr -signkey trodix.com.key -out trodix.com.pem
```

### Generate a pk12 keystore from the root key/certificate

```bash
openssl pkcs12 -export -inkey trodix.com.key -in trodix.com.pem -out keyStore.p12
```
