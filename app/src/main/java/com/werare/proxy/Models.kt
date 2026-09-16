package com.werare.proxy

data class WerareConfig(val name:String, val host:String, val port:Int, val type:String, val user:String, val pass:String)

data class ProxyServer(val host:String, val port:Int, val type:String="SOCKS5", val user:String?=null, val pass:String?=null) {
    override fun toString(): String = buildString { append(type.lowercase()); append("://"); append(host); append(":"); append(port); if (!user.isNullOrEmpty()) append(":").append(user).append(":").append(pass ?: "") }
    companion object {
        fun parse(raw:String): ProxyServer? {
            val s=raw.trim(); if(s.isEmpty()||s.startsWith("#")) return null
            return runCatching {
                var x=s; var type="SOCKS5"
                if (x.contains("://")) { val i=x.indexOf("://"); type=x.substring(0,i).uppercase(); x=x.substring(i+3) }
                val p=x.split(":"); if(p.size<2) return null
                val port=p[1].toIntOrNull() ?: return null
                ProxyServer(p[0],port,type, p.getOrNull(2)?.ifBlank{null}, p.getOrNull(3)?.ifBlank{null})
            }.getOrNull()
        }
    }
}

class Prefs(context: android.content.Context) {
    private val p=context.getSharedPreferences("werare", android.content.Context.MODE_PRIVATE)
    var unlocked:Boolean get()=p.getBoolean("unlocked",false) set(v){p.edit().putBoolean("unlocked",v).apply()}
    var lastProxy:String get()=p.getString("lastProxy","") ?: "" set(v){p.edit().putString("lastProxy",v).apply()}
    var targetApp:String get()=p.getString("targetApp","") ?: "" set(v){p.edit().putString("targetApp",v).apply()}
    var showOverlayButton:Boolean get()=p.getBoolean("overlay",true) set(v){p.edit().putBoolean("overlay",v).apply()}
    var buttonSize:Int get()=p.getInt("size",120) set(v){p.edit().putInt("size",v).apply()}
    var cornerRadius:Int get()=p.getInt("radius",40) set(v){p.edit().putInt("radius",v).apply()}
    var buttonOpacity:Int get()=p.getInt("opacity",100) set(v){p.edit().putInt("opacity",v).apply()}
    var durationSec:Int get()=p.getInt("duration",60) set(v){p.edit().putInt("duration",v).apply()}
    var lagMs:Int get()=p.getInt("lag",0) set(v){p.edit().putInt("lag",v).apply()}
    var lossPercent:Int get()=p.getInt("loss",0) set(v){p.edit().putInt("loss",v).apply()}
    var buttonColor:Int get()=p.getInt("color",android.graphics.Color.parseColor("#AEEA00")) set(v){p.edit().putInt("color",v).apply()}
}
