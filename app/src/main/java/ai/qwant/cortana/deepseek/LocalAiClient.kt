package ai.qwant.cortana.deepseek

import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object LocalAiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    fun chat(
        endpoint: String,
        model: String,
        systemPrompt: String,
        messages: List<ChatLine>,
        memoryNotes: List<String>
    ): String {
        require(model.isNotBlank()) { "Podaj identyfikator modelu w Ustawieniach." }
        val base = validatedLocalBase(endpoint)
        val memoryContext = if (memoryNotes.isEmpty()) {
            "\n\nPAMIĘĆ UŻYTKOWNIKA: brak zapisanych notatek."
        } else {
            "\n\nPAMIĘĆ UŻYTKOWNIKA (jawnie zapisane notatki, traktuj je jako kontekst):\n" +
                memoryNotes.take(30).joinToString("\n") { "- " + it.take(1200) }
        }
        val history = JSONArray()
        history.put(JSONObject().put("role", "system").put("content", systemPrompt.take(8000) + memoryContext))
        messages.takeLast(20).forEach { line ->
            if (line.role == "user" || line.role == "assistant") {
                history.put(JSONObject().put("role", line.role).put("content", line.text.take(12000)))
            }
        }
        val payload = JSONObject()
            .put("model", model.trim())
            .put("messages", history)
            .put("temperature", 0.7)
            .put("stream", false)
        val url = base.newBuilder().addPathSegments("v1/chat/completions").build()
        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Serwer zwrócił HTTP " + response.code + ": " + body.take(500))
            }
            val choices = JSONObject(body).optJSONArray("choices")
                ?: throw IllegalStateException("Nieprawidłowa odpowiedź API: brak choices.")
            if (choices.length() == 0) throw IllegalStateException("Model zwrócił pustą odpowiedź.")
            val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content").orEmpty()
            if (content.isBlank()) throw IllegalStateException("Model zwrócił pustą treść.")
            content.trim()
        }
    }

    fun checkHealth(endpoint: String) {
        val base = validatedLocalBase(endpoint)
        val request = Request.Builder().url(base.newBuilder().addPathSegment("health").build()).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Serwer /health zwrócił HTTP " + response.code + ".")
            }
        }
    }

    private fun validatedLocalBase(rawEndpoint: String): HttpUrl {
        val base = rawEndpoint.trim().trimEnd('/').toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Nieprawidłowy adres. Użyj np. http://127.0.0.1:8080.")
        if (base.scheme != "http" && base.scheme != "https") {
            throw IllegalArgumentException("Dozwolone są wyłącznie adresy HTTP lub HTTPS w sieci lokalnej.")
        }
        val host = base.host.lowercase()
        val ipLiteral = host.contains(':') || IPV4_PATTERN.matches(host)
        val localName = host == "localhost" || host.endsWith(".local")
        if (!ipLiteral && !localName) {
            throw SecurityException("Dozwolone są tylko localhost, nazwy .local i prywatne adresy IP.")
        }
        val addresses = try {
            InetAddress.getAllByName(host)
        } catch (error: Exception) {
            throw IllegalArgumentException("Nie można odnaleźć lokalnego hosta: " + error.message)
        }
        if (addresses.isEmpty() || addresses.any {
                !(it.isLoopbackAddress || it.isSiteLocalAddress || it.isLinkLocalAddress)
            }
        ) {
            throw SecurityException("Połączenie z publicznym adresem jest zablokowane. Użyj serwera lokalnego lub prywatnego IP.")
        }
        if (host == "localhost" && addresses.any { !it.isLoopbackAddress }) {
            throw SecurityException("Nazwa localhost musi wskazywać wyłącznie na loopback.")
        }
        return base
    }

    private val IPV4_PATTERN = Regex("""\d{1,3}(\.\d{1,3}){3}""")
}
