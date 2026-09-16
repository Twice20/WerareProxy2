# WerareProxy Android

Готовый проект для Android Studio. Откройте папку проекта и выполните **Sync Project with Gradle Files**, затем Build APK.

В `app/src/main/assets/proxies.txt` помещён загруженный список прокси. Поддержан разбор `http://`, `socks4://`, `socks5://` и формата `host:port[:user:pass]`.

Важно: Android `VpnService` только создаёт TUN-интерфейс. Для полноценной маршрутизации всего трафика через SOCKS/HTTP нужен user-space tun2socks (например, нативный компонент); без него TUN не перенаправляет пакеты сам по себе. Локальный SOCKS5 engine при этом собирается и умеет подключаться к HTTP CONNECT, SOCKS4/SOCKS4A и SOCKS5 upstream.
