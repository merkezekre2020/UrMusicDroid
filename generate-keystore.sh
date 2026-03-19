#!/bin/bash
# Debug keystore oluşturur (bir kez çalıştırmanız yeterli)
# Bu dosyayı repo'ya commit edin, böylece CI ve lokal build aynı imzayı kullanır.

mkdir -p keystore

if [ -f keystore/debug.keystore ]; then
    echo "keystore/debug.keystore zaten mevcut. Üzerine yazılsın mı? (e/h)"
    read -r answer
    if [ "$answer" != "e" ]; then
        echo "İptal edildi."
        exit 0
    fi
fi

keytool -genkeypair -v \
    -keystore keystore/debug.keystore \
    -alias androiddebugkey \
    -keyalg RSA -keysize 2048 \
    -validity 10000 \
    -storepass android \
    -keypass android \
    -dname "C=US, O=Android, CN=Android Debug"

echo ""
echo "Debug keystore oluşturuldu: keystore/debug.keystore"
echo "Bu dosyayı git'e commit edin:"
echo "  git add keystore/debug.keystore && git commit -m 'Add debug keystore'"
