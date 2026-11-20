package ai.qwant.cortana.deepseek

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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
    val prefs = remember { context.getSharedPreferences("cortana_settings", android.content.Context.MODE_PRIVATE) }
    val cyan = Color(0xFF54E8FF)
    val bg = Color(0xFF050B18)

    var endpoint by remember { mutableStateOf(prefs.getString("endpoint", "http://127.0.0.1:8080") ?: "http://127.0.0.1:8080") }
    var model by remember { mutableStateOf(prefs.getString("model", "local-model") ?: "local-model") }
    var prompt by remember {
        mutableStateOf(prefs.getString(
            "system_prompt",
            "Jesteś Cortaną — polskojęzyczną, rzeczową i pomocną asystentką AI. Oznaczaj niepewność i nie zmyślaj faktów."
        ) ?: "Jesteś Cortaną — polskojęzyczną, rzeczową i pomocną asystentką AI. Oznaczaj niepewność i nie zmyślaj faktów.")
    }
    var input by remember { mutableStateOf("") }
    var noteDraft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("LOCAL CORE • ŁADOWANIE") }
    var info by remember { mutableStateOf("") }
    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    var speechStatus by remember { mutableStateOf("Mowa opcjonalna") }
    var tab by remember { mutableIntStateOf(0) }
    var clearChatDialog by remember { mutableStateOf(false) }
    var clearNotesDialog by remember { mutableStateOf(false) }

    val messages = remember { mutableStateListOf<ChatLine>() }
    val notes = remember { mutableStateListOf<MemoryNote>() }
    val scope = rememberCoroutineScope()

    DisposableEffect(store) { onDispose { store.close() } }

    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { speechStatus = "Mowa: słucham" }
            override fun onBeginningOfSpeech() { speechStatus = "Mowa: rozpoznawanie" }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { speechStatus = "Mowa: przetwarzanie" }
            override fun onError(error: Int) { speechStatus = "Mowa: błąd rozpoznawania ($error)" }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    input = listOf(input.trim(), spoken.trim()).filter { it.isNotBlank() }.joinToString(" ")
                    speechStatus = "Mowa: tekst dodany"
                } else speechStatus = "Mowa: brak tekstu"
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose { recognizer?.destroy() }
    }

    val tts = remember(context) {
        TextToSpeech(context) { result -> ttsReady = result == TextToSpeech.SUCCESS }
    }
    LaunchedEffect(ttsReady) {
        if (ttsReady) {
            val result = tts.setLanguage(Locale.forLanguageTag("pl-PL"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = false
                speechStatus = "Brak polskiego głosu TTS"
            }
        }
    }
    DisposableEffect(tts) {
        onDispose { tts.stop(); tts.shutdown() }
    }

    val startRecognition: () -> Unit = {
        if (recognizer == null) speechStatus = "Brak usługi rozpoznawania mowy"
        else try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pl-PL")
            }
            recognizer.startListening(intent)
        } catch (error: Exception) {
            speechStatus = error.message ?: "Nie można uruchomić mikrofonu"
        }
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecognition() else speechStatus = "Brak zgody na mikrofon"
    }

    LaunchedEffect(Unit) {
        try {
            val loaded = withContext(Dispatchers.IO) { store.loadMessages() }
            val savedNotes = withContext(Dispatchers.IO) { store.loadNotes() }
            if (loaded.isEmpty()) {
                val welcome = ChatLine("assistant", "Cortana DeepSeek 1.0.0 gotowa. Skonfiguruj lokalny silnik w zakładce Ustawienia.")
                messages.add(welcome)
                withContext(Dispatchers.IO) { store.addMessage(welcome) }
            } else messages.addAll(loaded)
            notes.addAll(savedNotes)
            ready = true
            status = "LOCAL CORE • GOTOWA"
        } catch (error: Exception) {
            info = "Błąd bazy danych: " + (error.message ?: "nieznany")
            status = "LOCAL CORE • BŁĄD"
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = cyan, background = bg, surface = Color(0xFF101B2C))) {
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF07172A), bg)))
                .padding(horizontal = 13.dp, vertical = 10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CORTANA", color = cyan, fontSize = 28.sp, letterSpacing = 6.sp, fontWeight = FontWeight.Bold)
                    Text("DEEPSEEK • LOCAL INTELLIGENCE", color = Color.LightGray, fontSize = 10.sp)
                }
                Text("1.0.0", color = cyan, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(9.dp))
            Box(
                Modifier.fillMaxWidth().height(88.dp)
                    .background(Brush.horizontalGradient(listOf(Color(0x1833DFFF), Color(0x18346AAE), Color(0x1833DFFF))), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("◉", color = cyan, fontSize = 46.sp)
                    Text("HOLOGRAPHIC INTERFACE", color = cyan, fontSize = 9.sp, letterSpacing = 3.sp)
                }
            }
            Text(status, color = cyan, fontSize = 10.sp, modifier = Modifier.padding(vertical = 6.dp))
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = cyan) {
                listOf("CZAT", "PAMIĘĆ", "USTAWIENIA", "EWOLUCJA").forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label, fontSize = 9.sp, maxLines = 1) })
                }
            }

            when (tab) {
                0 -> {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 7.dp)) {
                        items(messages) { line ->
                            Card(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                colors = CardDefaults.cardColors(containerColor = if (line.role == "user") Color(0xFF12344A) else Color(0xFF101B2C))
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (line.role == "user") "TY" else "CORTANA", color = cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        if (line.role == "assistant") {
                                            TextButton(
                                                onClick = { if (ttsReady) tts.speak(line.text, TextToSpeech.QUEUE_FLUSH, null, "cortana-voice") },
                                                enabled = ttsReady,
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                            ) { Text("ODCZYTAJ", color = cyan, fontSize = 9.sp) }
                                        }
                                    }
                                    Text(line.text, color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    Text(speechStatus, color = Color.LightGray, fontSize = 10.sp)
                    OutlinedTextField(
                        value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Napisz do Cortany…") }, enabled = ready && !busy, maxLines = 4
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    startRecognition()
                                } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            enabled = ready && !busy && recognizer != null,
                            modifier = Modifier.weight(0.9f)
                        ) { Text("MIKROFON", fontSize = 10.sp) }
                        Button(
                            onClick = {
                                val text = input.trim()
                                if (text.isNotEmpty() && ready && !busy) {
                                    val userLine = ChatLine("user", text)
                                    messages.add(userLine)
                                    input = ""
                                    busy = true
                                    status = "LOCAL CORE • PRZETWARZANIE"
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) { store.addMessage(userLine) }
                                            val savedNotes = withContext(Dispatchers.IO) { store.loadNotes() }
                                            val contextMessages = messages.toList().takeLast(20)
                                            val answer = withContext(Dispatchers.IO) {
                                                LocalAiClient.chat(endpoint, model, prompt, contextMessages, savedNotes.map { it.content })
                                            }
                                            val response = ChatLine("assistant", answer)
                                            messages.add(response)
                                            withContext(Dispatchers.IO) { store.addMessage(response) }
                                            status = "LOCAL CORE • ODPOWIEDŹ GOTOWA"
                                        } catch (error: Exception) {
                                            val message = ChatLine(
                                                "assistant",
                                                "Nie udało się uzyskać odpowiedzi: " + (error.message ?: "nieznany błąd") +
                                                    ". Sprawdź lokalny llama-server i ustawienia."
                                            )
                                            messages.add(message)
                                            withContext(Dispatchers.IO) { store.addMessage(message) }
                                            status = "LOCAL CORE • SPRAWDŹ POŁĄCZENIE"
                                        } finally { busy = false }
                                    }
                                }
                            },
                            enabled = ready && !busy && input.isNotBlank(),
                            modifier = Modifier.weight(1.1f)
                        ) { Text(if (!ready) "ŁADOWANIE" else if (busy) "MYŚLĘ…" else "WYŚLIJ", fontSize = 10.sp) }
                    }
                    TextButton(onClick = { clearChatDialog = true }, enabled = ready && !busy, modifier = Modifier.align(Alignment.End)) {
                        Text("WYCZYŚĆ HISTORIĘ CZATU", color = Color.LightGray, fontSize = 9.sp)
                    }
                }
                1 -> {
                    Column(Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
                        Text("PAMIĘĆ CORTANY", color = cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Notatki, które świadomie zapisujesz, są przechowywane lokalnie i dodawane do kontekstu modelu.", color = Color.LightGray, fontSize = 11.sp)
                        Spacer(Modifier.height(9.dp))
                        OutlinedTextField(value = noteDraft, onValueChange = { noteDraft = it }, label = { Text("Nowa notatka") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                        Button(
                            onClick = {
                                val value = noteDraft.trim()
                                if (value.isNotEmpty()) scope.launch {
                                    val id = withContext(Dispatchers.IO) { store.addNote(value) }
                                    notes.add(0, MemoryNote(id, value.take(3000), System.currentTimeMillis()))
                                    noteDraft = ""
                                }
                            },
                            enabled = noteDraft.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("ZAPISZ NOTATKĘ") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("NOTATKI: " + notes.size, color = cyan, fontSize = 10.sp)
                            TextButton(onClick = { clearNotesDialog = true }, enabled = notes.isNotEmpty()) { Text("USUŃ WSZYSTKIE", fontSize = 9.sp) }
                        }
                        LazyColumn(Modifier.weight(1f)) {
                            items(notes, key = { it.id }) { note ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF101B2C))) {
                                    Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(note.content, color = Color.White, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                        TextButton(onClick = {
                                            scope.launch {
                                                withContext(Dispatchers.IO) { store.deleteNote(note.id) }
                                                notes.removeAll { it.id == note.id }
                                            }
                                        }) { Text("USUŃ", color = cyan, fontSize = 9.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    Column(Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
                        Text("KONFIGURACJA LOKALNEGO AI", color = cyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Obsługiwane są localhost, nazwy .local i prywatne adresy IP. Publiczne hosty są blokowane.", color = Color.LightGray, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("Adres serwera") }, placeholder = { Text("http://127.0.0.1:8080") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Identyfikator modelu") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Instrukcja systemowa") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6)
                        Button(
                            onClick = {
                                prefs.edit().putString("endpoint", endpoint.trim()).putString("model", model.trim()).putString("system_prompt", prompt.trim()).apply()
                                info = "Ustawienia zapisane lokalnie."
                                status = "LOCAL CORE • USTAWIENIA ZAPISANE"
                            },
                            enabled = endpoint.isNotBlank() && model.isNotBlank() && prompt.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("ZAPISZ USTAWIENIA") }
                        OutlinedButton(
                            onClick = {
                                testing = true
                                info = "Sprawdzanie serwera…"
                                scope.launch {
                                    info = withContext(Dispatchers.IO) {
                                        try {
                                            LocalAiClient.checkHealth(endpoint)
                                            "Połączenie działa. Endpoint /health odpowiedział poprawnie."
                                        } catch (error: Exception) {
                                            "Test nieudany: " + (error.message ?: "brak odpowiedzi")
                                        }
                                    }
                                    testing = false
                                }
                            },
                            enabled = !testing && endpoint.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (testing) "TESTOWANIE…" else "TESTUJ POŁĄCZENIE") }
                        Text(info, color = Color.LightGray, fontSize = 11.sp)
                        Text("APK nie zawiera wag GGUF. Wymaga osobno uruchomionego lokalnego llama-server.", color = Color(0xFFFFD180), fontSize = 11.sp)
                    }
                }
                else -> {
                    Column(Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
                        Text("EVOLUTION CORE", color = cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Rozwój w kontrolowanym cyklu:", color = Color.LightGray, fontSize = 12.sp)
                        listOf(
                            "ANALIZA — rozpoznanie błędu lub brakującej funkcji.",
                            "PROPOZYCJA — przygotowanie opisu zmiany lub patcha.",
                            "TESTY — kompilacja, testy i kontrola regresji.",
                            "AKCEPTACJA — użytkownik przegląda zmianę i ją zatwierdza.",
                            "WYDANIE — powtarzalny build APK przez CI."
                        ).forEach { item ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF101B2C))) {
                                Text(item, color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                            }
                        }
                        Text("Cortana nie wykonuje kodu wygenerowanego przez model, nie podmienia samodzielnie APK, nie instaluje aktualizacji bez zgody i nie uzyskuje uprawnień administratora.", color = Color(0xFFFFD180), fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Rozpoznawanie mowy zależy od usługi dostępnej na Androidzie. Tryb offline ASR/TTS nie jest gwarantowany.", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (clearChatDialog) AlertDialog(
        onDismissRequest = { clearChatDialog = false },
        title = { Text("Usunąć historię czatu?") },
        text = { Text("Wiadomości zostaną usunięte. Notatki pamięci pozostaną.") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) { store.clearMessages() }
                    messages.clear()
                    val welcome = ChatLine("assistant", "Historia czatu wyczyszczona. Notatki pamięci pozostały.")
                    messages.add(welcome)
                    withContext(Dispatchers.IO) { store.addMessage(welcome) }
                    clearChatDialog = false
                }
            }) { Text("USUŃ HISTORIĘ") }
        },
        dismissButton = { TextButton(onClick = { clearChatDialog = false }) { Text("ANULUJ") } }
    )

    if (clearNotesDialog) AlertDialog(
        onDismissRequest = { clearNotesDialog = false },
        title = { Text("Usunąć wszystkie notatki?") },
        text = { Text("Historia czatu nie zostanie naruszona.") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) { store.clearNotes() }
                    notes.clear()
                    clearNotesDialog = false
                }
            }) { Text("USUŃ NOTATKI") }
        },
        dismissButton = { TextButton(onClick = { clearNotesDialog = false }) { Text("ANULUJ") } }
    )
}
