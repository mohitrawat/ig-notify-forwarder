package dev.vorgal.ignotify

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

// Sends events to the receiver. Events that fail to send are kept in a local file
// and retried before the next event, so a network drop delays data but does not lose it.
object Forwarder {
    private const val PREFS = "cfg"
    private const val MAX_QUEUED = 5000
    private val io = Executors.newSingleThreadExecutor()

    fun url(ctx: Context): String = prefs(ctx).getString("url", "") ?: ""
    fun token(ctx: Context): String = prefs(ctx).getString("token", "") ?: ""

    fun save(ctx: Context, url: String, token: String) {
        prefs(ctx).edit().putString("url", url.trim()).putString("token", token.trim()).apply()
    }

    fun send(ctx: Context, event: JSONObject) {
        event.put("createdAt", System.currentTimeMillis())
        event.put("device", Build.MODEL)
        val line = event.toString()
        val app = ctx.applicationContext
        io.execute { deliver(app, line) }
    }

    fun queuedCount(ctx: Context): Int = queueFile(ctx).let { if (it.exists()) it.readLines().size else 0 }

    private fun deliver(ctx: Context, line: String) {
        val file = queueFile(ctx)
        val pending = (if (file.exists()) file.readLines() else emptyList()) + line
        var sent = 0
        for (p in pending) {
            if (!post(ctx, p)) break
            sent++
        }
        val rest = pending.drop(sent).takeLast(MAX_QUEUED)
        if (rest.isEmpty()) file.delete() else file.writeText(rest.joinToString("\n", postfix = "\n"))
    }

    private fun post(ctx: Context, body: String): Boolean {
        val target = url(ctx)
        if (target.isBlank()) return false
        return try {
            val conn = URL(target).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-Token", token(ctx))
            conn.outputStream.use { it.write(body.toByteArray()) }
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            ok
        } catch (e: Exception) {
            false
        }
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun queueFile(ctx: Context) = File(ctx.filesDir, "queue.jsonl")
}
