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
        try {
            val msg = error.message?.take(80) ?: error.javaClass.simpleName
            val body = "Crash: $msg\n${error.javaClass.name}"
            val jsonPayload = "{\"title\":\"LegionForge crash: $msg\",\"body\":\"$body\"}"
            val url = URL("https://api.github.com/repos/$REPO/issues")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "LegionForge")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.outputStream.write(jsonPayload.toByteArray())
            conn.responseCode
        } catch (_: Exception) { }
    }
}