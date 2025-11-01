@echo off
cd app
if not exist mstream-release.keystore (
    keytool -genkey -v -keystore mstream-release.keystore -alias mstream -keyalg RSA -keysize 2048 -validity 10000 -storepass mstream123 -keypass mstream123 -dname "CN=MStream, OU=Dev, O=MStream, L=City, S=State, C=US"
)
cd ..
gradlew assembleRelease
start "" "app\build\outputs\apk\release"
pause
