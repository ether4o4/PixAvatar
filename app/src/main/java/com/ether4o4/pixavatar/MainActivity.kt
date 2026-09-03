package com.ether4o4.pixavatar

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var avatarView: AvatarView
    private lateinit var status: TextView
    private lateinit var input: EditText
    private lateinit var send: Button
    private lateinit var tts: TextToSpeech

    private val companion by lazy(LazyThreadSafetyMode.NONE) {
        LocalCompanion(this, lifecycleScope)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "PixAvatar"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 14)
        }

        avatarView = AvatarView(this)
        root.addView(avatarView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        status = TextView(this).apply {
            text = "Loading local AI…"
            gravity = Gravity.CENTER
            textSize = 13f
            setPadding(4, 8, 4, 8)
        }
        root.addView(status, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        input = EditText(this).apply {
            hint = "Talk to PixAvatar…"
            singleLine = true
        }
        row.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        send = Button(this).apply {
            text = "Send"
            isEnabled = false
            setOnClickListener { sendMessage() }
        }
        row.addView(send, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(row)

        setContentView(root)
        tts = TextToSpeech(this, this)

        lifecycleScope.launch {
            try {
                status.text = "Downloading Qwen2.5 0.5B…"
                avatarView.setExpression("thinking")
                val path = companion.prepare { downloaded, total ->
                    runOnUiThread {
                        if (total > 0) {
                            val pct = (downloaded * 100 / total).toInt()
                            status.text = "Downloading local AI… $pct%"
                        } else {
                            status.text = "Downloading local AI…"
                        }
                    }
                }
                status.text = "Local AI ready • ${path.substringAfterLast('/')}"
                avatarView.setExpression("happy")
                send.isEnabled = true
            } catch (t: Throwable) {
                status.text = "Model setup failed: ${t.message ?: t.javaClass.simpleName}"
                avatarView.setExpression("annoyed")
            }
        }
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty() || !send.isEnabled) return

        input.text.clear()
        send.isEnabled = false
        status.text = "Thinking…"
        avatarView.setExpression("thinking")

        lifecycleScope.launch {
            try {
                val response = companion.reply(text)
                val emotion = Regex("(?m)^EMOTION\\s*=\\s*([a-zA-Z_]+)\\s*$")
                    .find(response)?.groupValues?.getOrNull(1) ?: "neutral"
                val spoken = response.replace(
                    Regex("(?m)^EMOTION\\s*=\\s*[a-zA-Z_]+\\s*$"), ""
                ).trim()

                avatarView.setExpression(emotion)
                status.text = "PixAvatar • $emotion"
                if (spoken.isNotEmpty()) {
                    avatarView.startSpeaking((spoken.length * 55L).coerceIn(900L, 30_000L))
                    tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "pixavatar-${System.nanoTime()}")
                }
            } catch (t: Throwable) {
                status.text = "Inference failed: ${t.message ?: t.javaClass.simpleName}"
                avatarView.setExpression("annoyed")
            } finally {
                send.isEnabled = true
            }
        }
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(1.02f)
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        companion.close()
        super.onDestroy()
    }
}
