package dev.vorgal.ignotify

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject

// One settings screen: receiver URL, shared token, and shortcuts to the two
// system settings the listener needs. Built in code to avoid layout resources.
class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = EditText(this).apply {
            hint = "http://192.168.1.10:8787/ingest"
            setText(Forwarder.url(this@MainActivity))
        }
        val token = EditText(this).apply {
            hint = "shared token"
            setText(Forwarder.token(this@MainActivity))
        }
        status = TextView(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            addView(TextView(context).apply { text = "Receiver URL" })
            addView(url)
            addView(TextView(context).apply { text = "Token" })
            addView(token)
            addView(button("Save") { Forwarder.save(context, url.text.toString(), token.text.toString()); refresh() })
            addView(button("Send test event") {
                Forwarder.send(context, JSONObject().put("type", "test"))
                refresh()
            })
            addView(button("Open notification access") {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            })
            addView(button("Open battery optimization") {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            })
            addView(status)
        }
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        status.text = "Notification access: ${if (enabled) "ON" else "OFF"}\n" +
            "Queued (unsent) events: ${Forwarder.queuedCount(this)}"
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }
}
