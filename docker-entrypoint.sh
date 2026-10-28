#!/bin/sh
set -eu

# The mounted file contains public CA certificates only, never a database password.
certificate_file=/etc/secrets/aiven-truststore.b64
truststore_file=/tmp/aiven-truststore.p12

if [ -f "$certificate_file" ]; then
    umask 077
    temporary_file=$(mktemp /tmp/safework-truststore.XXXXXX)
    trap 'rm -f "$temporary_file"' EXIT HUP INT TERM
    if ! base64 -d "$certificate_file" > "$temporary_file"; then
        echo 'SafeWork: el archivo aiven-truststore.b64 no contiene Base64 valido.' >&2
        exit 1
    fi
    if ! keytool -list -keystore "$temporary_file" -storetype PKCS12 \
        -storepass changeit >/dev/null 2>&1; then
        echo 'SafeWork: el certificado de Aiven no es un almacen PKCS12 valido.' >&2
        exit 1
    fi
    mv -f "$temporary_file" "$truststore_file"
    trap - EXIT HUP INT TERM
    echo 'SafeWork: certificado de confianza de Aiven preparado.'
else
    case "${SAFEWORK_DB_URL:-}" in
        *trustCertificateKeyStoreUrl=file:/tmp/aiven-truststore.p12*)
            echo 'SafeWork: falta Secret File aiven-truststore.b64 en /etc/secrets.' >&2
            exit 1
            ;;
    esac
fi

exec "$@"
