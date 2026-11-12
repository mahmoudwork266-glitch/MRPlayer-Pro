package com.mahmoudrateb.player

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class Channel(
    val id: String,
    val name: String,
    val group: String,
    val logo: String?,
    val url: String,
    val stalkerCmd: String? = null
)

class StalkerSession(val loadUrl: String, val mac: String, val token: String)

object Net {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 15000
            c.readTimeout = 30000
            c.setRequestProperty("User-Agent", "MRPlayer/1.0")
            headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            try {
                c.inputStream.bufferedReader().use { it.readText() }
            } finally {
                c.disconnect()
            }
        }
}

object M3uParser {
    private val attr = Regex("([\\w-]+)=\"([^\"]*)\"")

    fun parse(text: String): List<Channel> {
        val out = mutableListOf<Channel>()
        var name = ""
        var group = "Other"
        var logo: String? = null
        var pending = false
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("#EXTINF")) {
                val attrs = attr.findAll(line).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                group = attrs["group-title"]?.ifBlank { null } ?: "Other"
                logo = attrs["tvg-logo"]?.ifBlank { null }
                name = line.substringAfterLast(",").trim().ifBlank { attrs["tvg-name"] ?: "Unknown" }
                pending = true
            } else if (line.isNotEmpty() && !line.startsWith("#") && pending) {
                out.add(Channel(out.size.toString(), name, group, logo, line))
                pending = false
            }
        }
        return out
    }
}

object Xtream {
    suspend fun load(server: String, user: String, pass: String): List<Channel> {
        var base = server.trim().trimEnd('/')
        if (!base.startsWith("http")) base = "http://$base"
        val u = URLEncoder.encode(user, "UTF-8")
        val p = URLEncoder.encode(pass, "UTF-8")
        val api = "$base/player_api.php?username=$u&password=$p"
        val auth = JSONObject(Net.get(api))
        if (auth.optJSONObject("user_info")?.optInt("auth", 0) != 1) error("Invalid login")
        val catArr = JSONArray(Net.get("$api&action=get_live_categories"))
        val cats = HashMap<String, String>()
        for (i in 0 until catArr.length()) {
            val o = catArr.getJSONObject(i)
            cats[o.optString("category_id")] = o.optString("category_name")
        }
        val arr = JSONArray(Net.get("$api&action=get_live_streams"))
        val list = ArrayList<Channel>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("stream_id")
            list.add(
                Channel(
                    id, o.optString("name"),
                    cats[o.optString("category_id")] ?: "Other",
                    o.optString("stream_icon").ifBlank { null },
                    "$base/live/$user/$pass/$id.m3u8"
                )
            )
        }
        return list
    }
}

object Stalker {
    private fun hdr(mac: String, token: String?): Map<String, String> {
        val m = mutableMapOf("Cookie" to "mac=$mac; stb_lang=en; timezone=GMT")
        if (token != null) m["Authorization"] = "Bearer $token"
        return m
    }

    suspend fun connect(portal: String, mac: String): Pair<StalkerSession, List<Channel>> {
        var base = portal.trim().trimEnd('/')
        if (!base.startsWith("http")) base = "http://$base"
        if (base.endsWith("/c")) base = base.removeSuffix("/c")
        val load = "$base/server/load.php"
        val hs = JSONObject(Net.get("$load?type=stb&action=handshake&token=&JsHttpRequest=1-xml", hdr(mac, null)))
        val token = hs.getJSONObject("js").getString("token")
        val h = hdr(mac, token)
        val gmap = HashMap<String, String>()
        runCatching {
            val g = JSONObject(Net.get("$load?type=itv&action=get_genres&JsHttpRequest=1-xml", h)).getJSONArray("js")
            for (i in 0 until g.length()) {
                val o = g.getJSONObject(i)
                gmap[o.optString("id")] = o.optString("title")
            }
        }
        val data = JSONObject(Net.get("$load?type=itv&action=get_all_channels&JsHttpRequest=1-xml", h))
            .getJSONObject("js").getJSONArray("data")
        val list = ArrayList<Channel>(data.length())
        for (i in 0 until data.length()) {
            val o = data.getJSONObject(i)
            list.add(
                Channel(
                    o.optString("id"), o.optString("name"),
                    gmap[o.optString("tv_genre_id")] ?: "Other",
                    o.optString("logo").ifBlank { null }, "", o.optString("cmd")
                )
            )
        }
        return StalkerSession(load, mac, token) to list
    }

    suspend fun link(s: StalkerSession, cmd: String): String {
        val r = JSONObject(
            Net.get(
                "${s.loadUrl}?type=itv&action=create_link&cmd=${URLEncoder.encode(cmd, "UTF-8")}&JsHttpRequest=1-xml",
                hdr(s.mac, s.token)
            )
        )
        return r.getJSONObject("js").getString("cmd").substringAfter(' ').trim()
    }
}
