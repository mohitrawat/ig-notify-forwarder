package dev.vorgal.ignotify

import android.app.Notification
import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject

// Forwards Instagram notifications only. Every other app's notifications are dropped
// here and never leave the phone. A heartbeat every 5 minutes lets the receiver tell
// "listener died" apart from "nobody posted".
class IgListener : NotificationListenerService() {
    private val handler = Handler(Looper.getMainLooper())
    private val heartbeat = object : Runnable {
        override fun run() {
            Forwarder.send(this@IgListener, JSONObject().put("type", "heartbeat"))
            handler.postDelayed(this, HEARTBEAT_MS)
        }
    }

    override fun onListenerConnected() {
        handler.removeCallbacks(heartbeat)
        handler.post(heartbeat)
    }

    override fun onListenerDisconnected() {
        handler.removeCallbacks(heartbeat)
        requestRebind(ComponentName(this, IgListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != INSTAGRAM) return
        val n = sbn.notification
        val e = n.extras
        val event = JSONObject()
            .put("type", "notification")
            .put("key", sbn.key)
            .put("postTime", sbn.postTime)
            .put("tag", sbn.tag)
            .put("id", sbn.id)
            .put("channel", n.channelId)
            .put("group", n.group)
            .put("category", n.category)
            .put("title", e.getCharSequence(Notification.EXTRA_TITLE)?.toString())
            .put("text", e.getCharSequence(Notification.EXTRA_TEXT)?.toString())
            .put("subText", e.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString())
            .put("bigText", e.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString())
        // Dump every simple extra once, so the first test shows exactly what Instagram sends.
        val extras = JSONObject()
        for (k in e.keySet()) {
            @Suppress("DEPRECATION")
            val v = e.get(k)
            if (v is CharSequence || v is Number || v is Boolean) extras.put(k, v.toString())
        }
        event.put("extras", extras)
        Forwarder.send(this, event)
    }

    companion object {
        const val INSTAGRAM = "com.instagram.android"
        private const val HEARTBEAT_MS = 5 * 60 * 1000L
    }
}
