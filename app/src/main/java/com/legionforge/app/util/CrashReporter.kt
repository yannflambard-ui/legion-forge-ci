package com.legionforge.app.util

import android.content.Context
import java.io.StringWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Catches uncaught exceptions, reports them, and continues to default handler.
 */
object CrashReporter {
    private const val REPO = "yannflambard-ui/legion-forge-ci"
    private var context: Context? = null

    fun init(ctx: Context) {
        context = ctx
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            report(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun report(error: Throwable) {
        val msg = error.message?.take(80) ?: error.javaClass.simpleName
        android.util.Log.i("CrashReporter", "report crash: $msg")
        reportEvent("LegionForge crash: $msg", "Crash: $msg\n${error.javaClass.name}")
    }

    /**
     * Publie un rapport d'événement sur GitHub, même quand ce n'est pas une
     * exception (ex: "Catalogue vide"). Utilisé pour le diagnostic remote.
     * Déduplique : si le titre identique a déjà été créé, on l'ignore.
     */
    fun reportEvent(title: String, body: String) {
        synchronized(eventLock) {
            try {
                val cleanTitle = title.take(80)
                // Recherche d'un issue existant avec le même title (ouvrir seulement, max 5)
                val searchUrl = URL("https://api.github.com/search/issues?q=repo:$REPO+in:title+${urlencode(cleanTitle)}+state:open")
                val searchConn = searchUrl.openConnection() as HttpURLConnection
                searchConn.setRequestProperty("Accept", "application/vnd.github+json")
                searchConn.setRequestProperty("User-Agent", "LegionForge")
                if (searchConn.responseCode == 200) {
                    val bodySearch = searchConn.inputStream.bufferedReader().use { it.readText() }
                    if (bodySearch.contains("\"total_count\":") && regexTotalCount(bodySearch) > 0) {
                        android.util.Log.i("CrashReporter", "issue déjà ouverte pour: $cleanTitle")
                        return@synchronized
                    }
                }

                val jsonPayload = buildJson(mapOf("title" to cleanTitle, "body" to body))
                val url = URL("https://api.github.com/repos/$REPO/issues")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("User-Agent", "LegionForge")
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.doOutput = true
                conn.connectTimeout = 7000
                conn.outputStream.write(jsonPayload.toByteArray())
                android.util.Log.i("CrashReporter", "rapport $cleanTitle posted code=${conn.responseCode}")
            } catch (t: Throwable) {
                android.util.Log.w("CrashReporter", "impossible d'envoyer", t)
            }
        }
    }

    private val eventLock = Object()
    private fun regexTotalCount(s: String): Int =
        Regex("\"total_count\":(\\d+)").find(s)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    private fun urlencode(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    private fun buildJson(m: Map<String, String>): String {
        fun esc(v: String) = v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        return "{" + m.entries.joinToString(",") { "\"${it.key}\":\"${esc(it.value)}\"" } + "}"
    }
}