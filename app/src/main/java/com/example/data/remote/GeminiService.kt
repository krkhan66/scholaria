package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getTutorResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineFallbackResponse(prompt)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", "You are Scholaria AI Tutor, an encouraging and clear academic assistant for high school students. Explain concepts step by step in a friendly, easy-to-understand format with key formulas or bullet points.")
                    }))
                })
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", prompt)
                    }))
                }))
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseStr = response.body?.string() ?: ""
                    val jsonResp = JSONObject(responseStr)
                    val candidates = jsonResp.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCand = candidates.getJSONObject(0)
                        val content = firstCand.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", getOfflineFallbackResponse(prompt))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        getOfflineFallbackResponse(prompt)
    }

    private fun getOfflineFallbackResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("math") || lower.contains("quadratic") || lower.contains("equation") -> {
                "📘 **Quadratic Equations Guide**:\nA quadratic equation is in the form `ax² + bx + c = 0`.\n\n" +
                "**Quadratic Formula**:\n`x = [-b ± √(b² - 4ac)] / 2a`\n\n" +
                "• **Discriminant (D = b² - 4ac)**:\n  - If D > 0: 2 distinct real roots\n  - If D = 0: 2 equal real roots\n  - If D < 0: No real roots\n\n" +
                "Would you like to try solving a practice problem together?"
            }
            lower.contains("physics") || lower.contains("optic") || lower.contains("reflection") || lower.contains("light") -> {
                "💡 **Physics: Reflection of Light**:\n\n" +
                "**Laws of Reflection**:\n" +
                "1. Angle of incidence (i) equals angle of reflection (r).\n" +
                "2. Incident ray, reflected ray, and normal lie in the same plane.\n\n" +
                "**Mirror Formula**:\n`1/f = 1/v + 1/u`\n*(where f = focal length, v = image distance, u = object distance)*"
            }
            lower.contains("chemistry") || lower.contains("element") || lower.contains("periodic") -> {
                "🧪 **Chemistry Study Tip**:\n\n" +
                "Remember the first 20 elements using mnemonic:\n" +
                "*\"Happy Henry Likes Little Brown Balls On Nuts Friday Night...\"*\n" +
                "(H, He, Li, Be, B, C, N, O, F, Ne...)\n\n" +
                "Valency is key to writing chemical formulas!"
            }
            lower.contains("history") || lower.contains("essay") -> {
                "✍️ **History Essay Writing Tip**:\n\n" +
                "1. **Introduction**: State thesis and key dates.\n" +
                "2. **Body Paragraphs**: Focus on Causes, Major Events, and Immediate Consequences.\n" +
                "3. **Conclusion**: Summarize long-term impact on society."
            }
            else -> {
                "✨ **Scholaria AI Tutor**:\nThat's a great question about \"$prompt\"!\n\n" +
                "To tackle this effectively:\n" +
                "1. Break down the core concept into simple components.\n" +
                "2. Review your Grade 10 Chapter notes & examples.\n" +
                "3. Solve 2-3 practice problems step-by-step.\n\n" +
                "Feel free to ask specific formulas, definitions, or homework problems!"
            }
        }
    }
}
