package com.legionforge.app.util

import android.content.Context
import android.os.Looper
import java.io.StringWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Catches uncaught exceptions, shows a dialog, and reports to GitHub.
 */
object CrashReporter {
    private const val GITHUB_TOKEN = ""
    private const val REPO = "yannflambard-ui/legion-forge-ci"
    private var context: Context? = null

    fun init(ctx: Context, githubToken: String = "") {
        context = ctx
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            report(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun report(error: Throwable) {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString().take(4000)

        // Try to create GitHub issue via API
        try {
            val json = """{"title":"Crash report: ${error.message?.take(80) ?: "Unknown"}","body":"**Crash automatique**\`n`n```\`n$stackTrace\`n```"}"""
                .replace("\`n", "\n")

            val url = URL("https://api.github.com/repos/$REPO/issues")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "LegionForge-CrashReporter")
            if (GITHUB_TOKEN.isNotBlank()) conn.setRequestProperty("Authorization", "token $GITHUB_TOKEN")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.outputStream.write(json.toByteArray())
            conn.responseCode
        } catch (_: Exception) { }
    }
}