package com.mahmoudrateb.player

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.random.Random

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("mr", Context.MODE_PRIVATE)

    var channels by mutableStateOf<List<Channel>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)
    var favorites by mutableStateOf<Set<String>>(prefs.getStringSet("fav", emptySet())!!.toSet()); private set
    var selected by mutableStateOf<Channel?>(null)
    private var stalker: StalkerSession? = null

    val mac: String
        get() = prefs.getString("mac", null) ?: randomMac().also { prefs.edit().putString("mac", it).apply() }

    private fun randomMac(): String {
        val b = Random.nextBytes(3)
        return "00:1A:79:" + b.joinToString(":") { String.format(Locale.US, "%02X", it) }
    }

    fun isFav(c: Channel) = c.name in favorites

    fun toggleFav(c: Channel) {
        favorites = if (c.name in favorites) favorites - c.name else favorites + c.name
        prefs.edit().putStringSet("fav", favorites).apply()
    }

    private fun launchLoad(block: suspend () -> List<Channel>, onDone: () -> Unit) {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val l = withContext(Dispatchers.Default) { block() }
                if (l.isEmpty()) error = "No channels found" else {
                    channels = l
                    onDone()
                }
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
            } finally {
                loading = false
            }
        }
    }

    fun loadM3u(url: String, onDone: () -> Unit) =
        launchLoad({ M3uParser.parse(Net.get(url.trim())) }, onDone)

    fun loadXtream(server: String, user: String, pass: String, onDone: () -> Unit) =
        launchLoad({ Xtream.load(server, user.trim(), pass.trim()) }, onDone)

    fun loadFile(uri: Uri, onDone: () -> Unit) = launchLoad({
        withContext(Dispatchers.IO) {
            val text = getApplication<Application>().contentResolver.openInputStream(uri)!!
                .bufferedReader().use { it.readText() }
            M3uParser.parse(text)
        }
    }, onDone)

    fun loadPortal(portal: String, onDone: () -> Unit) = launchLoad({
        val (s, l) = Stalker.connect(portal, mac)
        stalker = s
        l
    }, onDone)

    suspend fun resolve(c: Channel): String =
        if (c.stalkerCmd != null) Stalker.link(stalker ?: error("No session"), c.stalkerCmd) else c.url
}
