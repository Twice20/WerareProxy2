package com.werare.proxy

import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

/**
 * VPN-туннель: заворачивает трафик выбранного приложения в tun-интерфейс.
 *
 * ВАЖНО: честно говоря, сам по себе TUN-интерфейс без user-space стека
 * работать не будет. В этот сервис нужно подключить tun2socks (badvpn)
 * либо hev-socks5-tunnel - они читают пакеты из tun и гонят их
 * в локальный SOCKS5 127.0.0.1:10808, где уже сидит ProxyEngine.
 * Без нативной библиотеки этот класс оставляет туннель "пустым",
 * но ровно так выглядит интеграция: Builder -> establish -> tun2socks -> ProxyEngine.
 */
class WerareVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = Prefs(this)
        val builder = Builder()
            .setSession("WerareProxy")
            .addAddress("10.0.0.2", 32)
            .addDnsServer("1.1.1.1")
            .addRoute("0.0.0.0", 0)
            .setMtu(1500)

        val target = prefs.targetApp
        if (target.isNotEmpty()) {
            try { builder.addAllowedApplication(target) }
            catch (e: Exception) { e.printStackTrace() }
        }

        vpnInterface?.close()
        vpnInterface = builder.establish()

        // === СЮДА подключается tun2socks: ===
        // tun2socks.start(vpnInterface.fd, "127.0.0.1:10808", ...)

        return Service.START_STICKY
    }

    override fun onDestroy() {
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        super.onDestroy()
    }
}
