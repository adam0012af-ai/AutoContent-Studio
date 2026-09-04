package com.adam.iptvplayer

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class IptvRepository(private val server: String, private val username: String, private val password: String) {
    data class Category(val id: String, val name: String)
    data class Item(val id: String, val name: String, val icon: String?, val extension: String?, val categoryId: String, val kind: String)
    data class Episode(val id: String, val title: String, val extension: String)

    private val base = server.trim().trimEnd('/')
    private fun enc(v: String) = URLEncoder.encode(v, "UTF-8")
    private fun api(action: String? = null, extra: String = ""): String {
        val a = if (action == null) "" else "&action=$action"
        return "$base/player_api.php?username=${enc(username)}&password=${enc(password)}$a$extra"
    }

    fun authenticate(): JSONObject = JSONObject(get(api()))

    fun categories(kind: String): List<Category> {
        val action = when (kind) { "live" -> "get_live_categories"; "vod" -> "get_vod_categories"; else -> "get_series_categories" }
        val arr = JSONArray(get(api(action)))
        return buildList {
            add(Category("all", "All"))
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(Category(o.optString("category_id"), o.optString("category_name", "Category")))
            }
        }
    }

    fun items(kind: String, categoryId: String? = null): List<Item> {
        val action = when (kind) { "live" -> "get_live_streams"; "vod" -> "get_vod_streams"; else -> "get_series" }
        val arr = JSONArray(get(api(action)))
        val out = ArrayList<Item>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val cat = o.optString("category_id")
            if (!categoryId.isNullOrBlank() && categoryId != "all" && cat != categoryId) continue
            val id = if (kind == "series") o.optString("series_id") else o.optString("stream_id")
            val icon = when (kind) { "series" -> o.optString("cover"); else -> o.optString("stream_icon") }
            out += Item(id, o.optString("name", "Untitled"), icon, o.optString("container_extension", if (kind == "live") "ts" else "mp4"), cat, kind)
        }
        return out
    }

    fun episodes(seriesId: String): List<Episode> {
        val root = JSONObject(get(api("get_series_info", "&series_id=${enc(seriesId)}")))
        val eps = root.optJSONObject("episodes") ?: return emptyList()
        val result = ArrayList<Episode>()
        val seasons = eps.keys()
        while (seasons.hasNext()) {
            val season = seasons.next()
            val arr = eps.optJSONArray(season) ?: continue
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                result += Episode(o.optString("id"), o.optString("title", "Episode ${i + 1}"), o.optString("container_extension", "mp4"))
            }
        }
        return result
    }

    fun streamUrl(item: Item): String = when (item.kind) {
        "live" -> "$base/live/${enc(username)}/${enc(password)}/${item.id}.${item.extension ?: "ts"}"
        "vod" -> "$base/movie/${enc(username)}/${enc(password)}/${item.id}.${item.extension ?: "mp4"}"
        else -> error("Series requires an episode")
    }

    fun episodeUrl(ep: Episode): String = "$base/series/${enc(username)}/${enc(password)}/${ep.id}.${ep.extension}"

    private fun get(address: String): String {
        val c = URL(address).openConnection() as HttpURLConnection
        c.connectTimeout = 12000; c.readTimeout = 20000
        c.setRequestProperty("User-Agent", "AdamIPTV/1.0 Android")
        try {
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("Server error $code")
            return text
        } finally { c.disconnect() }
    }
}
