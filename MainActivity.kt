package com.novaai.assistant

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val bg = Color.rgb(10, 12, 22)
    private val panel = Color.rgb(22, 25, 42)
    private val purple = Color.rgb(139, 110, 255)
    private val white = Color.rgb(245, 246, 255)
    private val muted = Color.rgb(160, 166, 190)
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var scroll: ScrollView
    private lateinit var status: TextView
    private val history = JSONArray()
    private val prefs by lazy { getSharedPreferences("nova_settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildScreen()
        addMessage("assistant", "Hi! I'm NOVA. What would you like to explore today? ✨")
    }

    private fun buildScreen() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(12))
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(TextView(this).apply {
            text = "✦  NOVA AI"
            textSize = 22f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        })
        brand.addView(TextView(this).apply {
            text = "YOUR AI SPACE"
            textSize = 10f
            letterSpacing = 0.18f
            setTextColor(muted)
        })
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(makeButton("⚙", panel) { showSettings() }, LinearLayout.LayoutParams(dp(48), dp(44)))
        outer.addView(header)

        val welcome = TextView(this).apply {
            text = "Ask anything. Discover what's possible."
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(18), 0, dp(12))
        }
        outer.addView(welcome)

        status = TextView(this).apply {
            text = if (prefs.getString("api_key", "").isNullOrBlank()) "Add an API key in ⚙ Settings to enable AI replies." else "Ready when you are."
            textSize = 12f
            setTextColor(if (prefs.getString("api_key", "").isNullOrBlank()) Color.rgb(255, 190, 105) else muted)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(panel, 12)
        }
        outer.addView(status)

        scroll = ScrollView(this).apply { isFillViewport = true }
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(14), 0, dp(12))
        }
        scroll.addView(messages)
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(6), dp(6))
            background = rounded(panel, 22)
        }
        input = EditText(this).apply {
            hint = "Message NOVA..."
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            maxLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(10), dp(8), dp(8), dp(8))
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(makeButton("↑", purple) { sendMessage() }, LinearLayout.LayoutParams(dp(48), dp(44)))
        outer.addView(composer)
        setContentView(outer)
    }

    private fun addMessage(role: String, text: String) {
        if (role != "system") history.put(JSONObject().put("role", role).put("content", text))
        val bubble = TextView(this).apply {
            this.text = (if (role == "user") "YOU\n" else "✦ NOVA\n") + text
            textSize = 15f
            setTextColor(white)
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(if (role == "user") Color.rgb(55, 48, 96) else panel, 16)
        }
        val lp = LinearLayout.LayoutParams(
            if (role == "user") (resources.displayMetrics.widthPixels * 0.82f).toInt() else -1,
            -2
        )
        lp.gravity = if (role == "user") Gravity.END else Gravity.START
        lp.bottomMargin = dp(10)
        messages.addView(bubble, lp)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        val key = prefs.getString("api_key", "").orEmpty()
        if (key.isBlank()) {
            showSettings()
            Toast.makeText(this, "Add your API key to turn on AI replies.", Toast.LENGTH_LONG).show()
            return
        }
        input.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(input.windowToken, 0)
        addMessage("user", text)
        status.text = "NOVA is thinking…"
        executor.execute {
            try {
                val reply = requestReply(key)
                runOnUiThread {
                    addMessage("assistant", reply)
                    status.text = "Ready when you are."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    addMessage("assistant", "I couldn't connect just now. Check your API key and internet connection, then try again.")
                    status.text = "Connection issue"
                }
            }
        }
    }

    private fun requestReply(key: String): String {
        val url = URL("https://api.openai.com/v1/chat/completions")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            val body = JSONObject()
                .put("model", "gpt-4o-mini")
                .put("messages", history)
                .put("temperature", 0.7)
            connection.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) throw Exception("API error $code")
            val json = JSONObject(response)
            return json.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim()
        } finally {
            connection.disconnect()
        }
    }

    private fun showSettings() {
        val field = EditText(this).apply {
            hint = "Paste your API key"
            setText(prefs.getString("api_key", ""))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(white)
            setHintTextColor(muted)
        }
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(TextView(this@MainActivity).apply {
                text = "Your key is stored on this device. Don't share your key or the APK with it embedded."
                setTextColor(muted)
                textSize = 13f
            })
            addView(field)
        }
        AlertDialog.Builder(this)
            .setTitle("NOVA settings")
            .setView(wrap)
            .setPositiveButton("Save") { _, _ ->
                prefs.edit().putString("api_key", field.text.toString().trim()).apply()
                status.text = if (field.text.isNullOrBlank()) "Add an API key in ⚙ Settings to enable AI replies." else "Ready when you are."
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear key") { _, _ ->
                prefs.edit().remove("api_key").apply()
                status.text = "Add an API key in ⚙ Settings to enable AI replies."
            }
            .show()
    }

    private fun makeButton(label: String, color: Int, action: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(color, 16)
            setOnClickListener { action() }
        }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
