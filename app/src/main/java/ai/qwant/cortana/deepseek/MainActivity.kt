package ai.qwant.cortana.deepseek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatLine(val role: String, val text: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CortanaApp() }
    }
}

@Composable
fun CortanaApp() {
    val context = LocalContext.current
    val store = remember { ChatStore(context) }
    val cyan = Color(0xFF54E8FF)
    val bg = Color(0xFF050B18)

    var endpoint by remember { mutableStateOf("http://127.0.0.1:8080") }
    var model by remember { mutableStateOf("local-model") }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("LOCAL MEMORY • ŁADOWANIE") }

    val messages = remember { mutableStateListOf<ChatLine>() }
    val scope = rememberCoroutineScope()
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    LaunchedEffect(store) {
        val savedMessages = withContext(Dispatchers.IO) { store.loadMessages() }
        if (savedMessages.isEmpty()) {
            val welcome = ChatLine(
                "assistant",
                "Cortana DeepSeek gotowa. Historia jest zapisywana lokalnie na tym urządzeniu. Uruchom llama-server, aby rozpocząć rozmowę offline."
            )
            messages.add(welcome)
            withContext(Dispatchers.IO) { store.addMessage(welcome) }
        } else {
            messages.addAll(savedMessages)
        }
        ready = true
        status = "LOCAL MEMORY • GOTOWA"
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = cyan, background = bg, surface = Color(0xFF101B2C))) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF07172A), bg)))
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CORTANA", color = cyan, fontSize = 29.sp, letterSpacing = 7.sp)
                    Text("DEEPSEEK • LOCAL INTELLIGENCE", color = Color.LightGray, fontSize = 11.sp)
                }
                TextButton(onClick = { showClearDialog = true }, enabled = ready && !busy) {
                    Text("WYCZYŚĆ PAMIĘĆ", color = cyan, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0x1826DFFF), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("◉", color = cyan, fontSize = 64.sp)
                    Text("HOLOGRAPHIC CORE", color = cyan, fontSize = 10.sp, letterSpacing = 3.sp)
                }
            }

            Text(status, color = cyan, fontSize = 10.sp, modifier = Modifier.padding(vertical = 6.dp))
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                label = { Text("Lokalny endpoint") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Nazwa modelu") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(messages) { line ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (line.role == "user") Color(0xFF12344A) else Color(0xFF101B2C)
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(if (line.role == "user") "TY" else "CORTANA", color = cyan, fontSize = 10.sp)
                            Text(line.text, color = Color.White)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Napisz do Cortany…") },
                enabled = ready && !busy
            )
            Button(
                onClick = {
                    val prompt = input.trim()
                    if (prompt.isNotEmpty() && !busy && ready) {
                        val userLine = ChatLine("user", prompt)
                        messages.add(userLine)
                        input = ""
                        busy = true
                        status = "LOCAL CORE • PRZETWARZANIE"
                        val conversationSnapshot = messages.toList()

                        scope.launch {
                            withContext(Dispatchers.IO) { store.addMessage(userLine) }

                            val answer = withContext(Dispatchers.IO) {
                                try {
                                    val history = JSONArray().put(
                                        JSONObject()
                                            .put("role", "system")
                                            .put("content", "Jesteś Cortaną, polskojęzyczną asystentką AI. Odpowiadaj jasno i rzeczowo.")
                                    )
                                    conversationSnapshot.takeLast(20).forEach { line ->
                                        history.put(JSONObject().put("role", line.role).put("content", line.text))
                                    }
                                    val payload = JSONObject()
                                        .put("model", model)
                                        .put("messages", history)
                                        .put("temperature", 0.7)
                                    val request = Request.Builder()
                                        .url(endpoint.trimEnd('/') + "/v1/chat/completions")
                                        .post(payload.toString().toRequestBody("application/json".toMediaType()))
                                        .build()

                                    client.newCall(request).execute().use { response ->
                                        if (!response.isSuccessful) error("HTTP ${response.code}")
                                        JSONObject(response.body?.string() ?: "{}")
                                            .getJSONArray("choices")
                                            .getJSONObject(0)
                                            .getJSONObject("message")
                                            .getString("content")
                                    }
                                } catch (error: Exception) {
                                    "Brak połączenia z lokalnym silnikiem: ${error.message}. Uruchom llama-server na 127.0.0.1:8080."
                                }
                            }

                            val assistantLine = ChatLine("assistant", answer)
                            messages.add(assistantLine)
                            withContext(Dispatchers.IO) { store.addMessage(assistantLine) }
                            busy = false
                            status = "LOCAL MEMORY • ZAPISANO"
                        }
                    }
                },
                enabled = ready && !busy && input.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (!ready) "ŁADOWANIE PAMIĘCI…" else if (busy) "PRZETWARZANIE…" else "WYŚLIJ DO CORTANY")
            }

            Text(
                "LOCAL SQLITE MEMORY • OFFLINE INFERENCE • v0.2.0",
                color = Color.Gray,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Wyczyścić lokalną pamięć?") },
            text = { Text("Usunięta zostanie cała historia rozmów zapisana w tej aplikacji. Tej operacji nie można cofnąć.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { store.clearMessages() }
                            messages.clear()
                            val welcome = ChatLine("assistant", "Pamięć rozmów została wyczyszczona. Możemy zacząć od nowa.")
                            messages.add(welcome)
                            withContext(Dispatchers.IO) { store.addMessage(welcome) }
                            status = "LOCAL MEMORY • WYCZYSZCZONA"
                            showClearDialog = false
                        }
                    }
                ) { Text("WYCZYŚĆ") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("ANULUJ") }
            }
        )
    }
}
