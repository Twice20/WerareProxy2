package com.werare.proxy

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

/** Экспорт / импорт / отправка конфигов с расширением .WerareProxy. */
object ConfigManager {

    fun toJson(c: WerareConfig): String = JSONObject().apply {
        put("format", "WerareProxy/1")
        put("name", c.name)
        put("host", c.host)
        put("port", c.port)
        put("type", c.type)
        put("user", c.user)
        put("pass", c.pass)
    }.toString()

    fun fromJson(raw: String): WerareConfig {
        val o = JSONObject(raw)
        if (!o.has("format")) throw IllegalArgumentException("Это не .WerareProxy файл")
        return WerareConfig(
            name = o.optString("name", "config"),
            host = o.optString("host", ""),
            port = o.optInt("port", 1080),
            type = o.optString("type", "SOCKS5"),
            user = o.optString("user", ""),
            pass = o.optString("pass", "")
        )
    }

    fun export(context: Context, uri: Uri, c: WerareConfig) {
        context.contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(toJson(c).toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Не удалось записать файл")
    }

    fun import(context: Context, uri: Uri): WerareConfig? = runCatching {
        context.contentResolver.openInputStream(uri)?.use {
            fromJson(it.readBytes().toString(Charsets.UTF_8))
        }
    }.getOrNull()

    /** Складывает конфиг в cache и возвращает content:// Uri для отправки. */
    fun shareUri(context: Context, c: WerareConfig): Uri {
        val dir = File(context.cacheDir, "configs").apply { mkdirs() }
        val safeName = c.name.ifBlank { "config" }.replace(Regex("[^A-Za-z0-9_\-]"), "")
        val file = File(dir, "$safeName.WerareProxy")
        file.writeText(toJson(c))
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntent(context: Context, c: WerareConfig): Intent {
        val uri = shareUri(context, c)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Отправить конфиг")
    }
}
