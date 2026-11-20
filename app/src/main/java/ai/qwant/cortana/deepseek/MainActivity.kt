package ai.qwant.cortana.deepseek

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
    val preferences = remember {
        context.getSharedPreferences("cortana_settings", android.content.Context.MODE_PRIVATE)
    }
    val cyan = Color(0xFF54E8FF)
    val background = Color(0xFF050B18)
    val defaultPrompt = "Jesteś Cortaną — polskojęzyczną, rzeczową i pomocną asystentką AI. Odpowiadaj jasno, oznaczaj niepewność i nie zmyślaj faktów."

    var endpoint by remember { mutableStateOf(preferences.getString("endpoint", "http://127.0.0.1:8080") ?: "http://127.0.0.1:8080") }
    var model by remember { mutableStateOf(preferences.getString("model", "local-model") ?: "local-model") }
    var systemPrompt by remember { mutableStateOf(preferences.getString("system_prompt", defaultPrompt) ?: defaultPrompt) }
    var input by remember { mutableStateOf("") }
    var memoryDraft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("LOCAL CORE • INICJALIZACJA") }
    var connectionResult by remember { mutableStateOf("") }
    var speechStatus by remember { mutableStateOf("Mowa: gotowa do użycia") }
    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var checkingConnection by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showClearNotesDialog by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }

    val messages = remember { mutableStateListOf<ChatLine>() }
    val notes = remember { mutableStateListOf<MemoryNote>() }
    val scope = rememberCoroutineScope()

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { speechStatus = "Mowa: słucham" }
            override fun onBeginningOfSpeech() { speechStatus = "Mowa: rozpoznawanie" }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { speechStatus = "Mowa: przetwarzanie" }
            override fun onError(error: Int) { speechStatus = "Mowa: błąd rozpoznawania (" + error + ")" }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    input = listOf(input.trim(), spoken.trim()).filter { it.isNotBlank() }.joinToString(" ")
                    speechStatus = "Mowa: tekst dodany"
                } else {
                    speechStatus = "Mowa: brak rozpoznanego tekstu"
                }
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose { speechRecognizer?.destroy() }
    }

    val textToSpeech = remember(context) {
        TextToSpeech(context) { result -> ttsReady = result == TextToSpeech.SUCCESS }
    }

    LaunchedEffect(ttsReady) {
        if (ttsReady) {
            val result = textToSpeech.setLanguage(Locale.forLanguageTag("pl-PL"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = false
                speechStatus = "Mowa: brak polskiego głosu TTS"
            }
        }
    }

    DisposableEffect(textToSpeech) {
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    val startRecognition: () -> Unit = {
        if (speechRecognizer == null) {
            speechStatus = "Mowa: brak usługi rozpoznawania"
        } else {
            try {
                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pl-PL")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
                speechRecognizer.startListening(speechIntent)
            } catch (error: Exception) {
                speechStatus = "Mowa: " + (error.message ?: "nie można uruchomić")
            }
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecognition()
        else speechStatus = "Mowa: nie przyznano dostępu do mikrofonu"
    }

    LaunchedEffect(store) {
        try {
            val savedMessages = withContext(Dispatchers.IO) { store.loadMessages() }
            val savedNotes = withContext(Dispatchers.IO) { store.loadNotes() }
            if (savedMessages.isEmpty()) {
                val welcome = ChatLine(
                    "assistant",
                    "Cortana DeepSeek 1.0.0 jest gotowa. Ustaw lokalny serwer w zakładce Ustawienia i rozpocznij rozmowę."
                )
                messages.add(welcome)
                withContext(Dispatchers.IO) { store.addMessage(welcome) }
            } else {
                messages.addAll(savedMessages)
            }
            notes.addAll(savedNotes)
            ready = true
            status = "LOCAL CORE • GOTOWA"
        } catch (error: Exception) {
            status = "LOCAL CORE • BŁĄD PAMIĘCI"
            connectionResult = error.message ?: "Nie można odczytać bazy danych."
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = cyan,
            background = background,
            surface = Color(0xFF101B2C),
            secondary = Color(0xFF8B7CFF)
        )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF07172A), background)))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CORTANA", color = cyan, fontSize = 27.sp, letterSpacing = 6.sp, fontWeight = FontWeight.Bold)
                    Text("DEEPSEEK • LOCAL INTELLIGENCE", color = Color.LightGray, fontSize = 10.sp, letterSpacing = 1.sp)
                }
                Text("1.0.0", color = cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(9.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color(0x1826DFFF), Color(0x182D6BBD), Color(0x1826DFFF))),
                        RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("◉", color = cyan, fontSize = 46.sp)
                    Text("HOLOGRAPHIC INTERFACE", color = cyan, fontSize = 9.sp, letterSpacing = 3.sp)
                }
            }
            Text(status, color = cyan, fontSize = 10.sp, modifier = Modifier.padding(vertical = 6.dp))

            val tabNames = listOf("CZAT", "PAMIĘĆ", "USTAWIENIA", "EVOLUTION")
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = cyan) {
                tabNames.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label, fontSize = 8.sp, maxLines = 1) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 7.dp)) {
                        items(messages) { line ->
                            Card(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (line.role == "user") Color(0xFF12344A) else Color(0xFF101B2C)
                                )
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(if (line.role == "user") "TY" else "CORTANA", color = cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        if (line.role == "assistant") {
                                            TextButton(
                                                onClick = {
                                                    if (ttsReady) textToSpeech.speak(line.text, TextToSpeech.QUEUE_FLUSH, null, "cortana-voice")
                                                    else speechStatus = "TTS nie jest dostępny na urządzeniu"
                                                },
                                                enabled = ttsReady,
                                                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)
                                            ) { Text("ODCZYTAJ", color = cyan, fontSize = 8.sp) }
                                        }
                                    }
                                    Text(line.text, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Text(speechStatus, color = Color.LightGray, fontSize = 9.sp, modifier = Modifier.padding(vertical = 2.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Napisz do Cortany…") },
                        enabled = ready && !busy,
                        maxLines = 4
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    startRecognition()
                                } else {
                                    microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            enabled = ready && !busy && speechRecognizer != null,
                            modifier = Modifier.weight(0.9f)
                        ) { Text("MIKROFON", fontSize = 9.sp) }

                        Button(
                            onClick = {
                                val prompt = input.trim()
                                if (prompt.isNotEmpty() && ready && !busy) {
                                    val userLine = ChatLine("user", prompt)
                                    messages.add(userLine)
                                    input = ""
                                    busy = true
                                    status = "LOCAL CORE • PRZETWARZANIE"
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) { store.addMessage(userLine) }
                                            val savedNotes = withContext(Dispatchers.IO) { store.loadNotes() }
                                            val snapshot = messages.toList().takeLast(20)
                                            val answer = withContext(Dispatchers.IO) {
                                                LocalAiClient.chat(
                                                    endpoint = endpoint,
                                                    model = model,
                                                    systemPrompt = systemPrompt,
                                                    messages = snapshot,
                                                    memoryNotes = savedNotes.map { it.content }
                                                )
                                            }
                                            val assistantLine = ChatLine("assistant", answer)
                                            messages.add(assistantLine)
                                            withContext(Dispatchers.IO) { store.addMessage(assistantLine) }
                                            status = "LOCAL CORE • ODPOWIEDŹ GOTOWA"
                                        } catch (error: Exception) {
                                            val errorLine = ChatLine(
                                                "assistant",
                                                "Nie udało się uzyskać odpowiedzi: " +
                                                    (error.message ?: "nieznany błąd") +
                                                    ". Sprawdź lokalny serwer i ustawienia."
                                            )
                                            messages.add(errorLine)
                                            withContext(Dispatchers.IO) { store.addMessage(errorLine) }
                                            status = "LOCAL CORE • SPRAWDŹ POŁĄCZENIE"
                                        } finally {
                                            busy = false
                                        }
                                    }
                                }
                            },
                            enabled = ready && !busy && input.isNotBlank(),
                            modifier = Modifier.weight(1.1f)
                        ) { Text(if (!ready) "ŁADOWANIE" else if (busy) "MYŚLĘ…" else "WYŚLIJ", fontSize = 9.sp) }
                    }
                    TextButton(
                        onClick = { showClearChatDialog = true },
                        enabled = ready && !busy,
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("WYCZYŚĆ HISTORIĘ CZATU", color = Color.LightGray, fontSize = 8.sp) }
                }

                1 -> {
                    Column(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp)) {
                        Text("PAMIĘĆ CORTANY", color = cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Zapisuj tutaj informacje, które świadomie chcesz przekazać modelowi jako kontekst. Notatki pozostają na telefonie.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = memoryDraft,
                            onValueChange = { memoryDraft = it },
                            label = { Text("Nowa notatka pamięci") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        Button(
                            onClick = {
                                val clean = memoryDraft.trim()
                                if (clean.isNotEmpty()) {
                                    scope.launch {
                                        val id = withContext(Dispatchers.IO) { store.addNote(clean) }
                                        notes.add(0, MemoryNote(id, clean.take(3000), System.currentTimeMillis()))
                                        memoryDraft = ""
                                    }
                                }
                            },
                            enabled = memoryDraft.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("ZAPISZ NOTATKĘ") }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("NOTATKI (" + notes.size + ")", color = cyan, fontSize = 10.sp)
                            TextButton(onClick = { showClearNotesDialog = true }, enabled = notes.isNotEmpty()) {
                                Text("USUŃ WSZYSTKIE", color = Color.LightGray, fontSize = 8.sp)
                            }
                        }
                        LazyColumn(Modifier.weight(1f)) {
                            items(notes, key = { it.id }) { note ->
                                Card(
                                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101B2C))
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(note.content, color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp)
                                        TextButton(onClick = {
                                            scope.launch {
                                                withContext(Dispatchers.IO) { store.deleteNote(note.id) }
                                                notes.removeAll { it.id == note.id }
                                            }
                                        }) { Text("USUŃ", color = cyan, fontSize = 8.sp) }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    Column(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp)) {
                        Text("LOKALNY SILNIK AI", color = cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Aplikacja odrzuca publiczne hosty. Używaj localhost lub prywatnego adresu IP w swojej sieci.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = { endpoint = it },
                            label = { Text("Adres lokalnego serwera") },
                            placeholder = { Text("http://127.0.0.1:8080") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Identyfikator modelu") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = systemPrompt,
                            onValueChange = { systemPrompt = it },
                            label = { Text("Instrukcja systemowa") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6
                        )
                        Button(
                            onClick = {
                                preferences.edit()
                                    .putString("endpoint", endpoint.trim())
                                    .putString("model", model.trim())
                                    .putString("system_prompt", systemPrompt.trim())
                                    .apply()
                                status = "LOCAL CORE • USTAWIENIA ZAPISANE"
                                connectionResult = "Ustawienia zapisane lokalnie."
                            },
                            enabled = endpoint.isNotBlank() && model.isNotBlank() && systemPrompt.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("ZAPISZ USTAWIENIA") }

                        OutlinedButton(
                            onClick = {
                                checkingConnection = true
                                connectionResult = "Sprawdzanie serwera…"
                                scope.launch {
                                    connectionResult = withContext(Dispatchers.IO) {
                                        try {
                                            LocalAiClient.checkHealth(endpoint)
                                            "Serwer odpowiedział poprawnie."
                                        } catch (error: Exception) {
                                            "Test nieudany: " + (error.message ?: "brak odpowiedzi")
                                        }
                                    }
                                    checkingConnection = false
                                }
                            },
                            enabled = !checkingConnection && endpoint.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (checkingConnection) "TESTOWANIE…" else "TESTUJ POŁĄCZENIE") }
                        Text(connectionResult, color = Color.LightGray, fontSize = 10.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Wymagany jest osobno uruchomiony llama-server. APK nie zawiera wag GGUF ani runtime llama.cpp.",
                            color = Color(0xFFFFD180),
                            fontSize = 11.sp
                        )
                    }
                }

                else -> {
                    Column(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp)) {
                        Text("EVOLUTION CORE", color = cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Kontrolowany rozwój oprogramowania", color = Color.LightGray, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "1. ANALIZA — zgłoszenie błędu lub mierzalnej potrzeby.",
                            "2. PROPOZYCJA — przygotowanie patcha w osobnej gałęzi.",
                            "3. TESTY — kompilacja, testy i kontrola regresji.",
                            "4. ZATWIERDZENIE — użytkownik przegląda zmianę i decyduje.",
                            "5. WYDANIE — CI buduje nowy artefakt APK."
                        ).forEach { item ->
                            Card(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF101B2C))
                            ) { Text(item, color = Color.White, modifier = Modifier.padding(10.dp), fontSize = 11.sp) }
                        }
                        Text(
                            "Model nie uruchamia kodu, poleceń systemowych ani nie instaluje APK. Zmiany produkcyjne wymagają przeglądu i akceptacji.",
                            color = Color(0xFFFFD180),
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Rozpoznawanie i synteza mowy zależą od usług Androida. Lokalna inferencja tekstowa działa przez skonfigurowany serwer.",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Usunąć historię czatu?") },
            text = { Text("Wiadomości zostaną usunięte. Notatki pamięci pozostaną bez zmian.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { store.clearMessages() }
                        messages.clear()
                        val welcome = ChatLine("assistant", "Historia czatu została wyczyszczona. Notatki pamięci nadal są dostępne.")
                        messages.add(welcome)
                        withContext(Dispatchers.IO) { store.addMessage(welcome) }
                        showClearChatDialog = false
                        status = "LOCAL CORE • HISTORIA WYCZYSZCZONA"
                    }
                }) { Text("USUŃ HISTORIĘ") }
            },
            dismissButton = { TextButton(onClick = { showClearChatDialog = false }) { Text("ANULUJ") } }
        )
    }

    if (showClearNotesDialog) {
        AlertDialog(
            onDismissRequest = { showClearNotesDialog = false },
            title = { Text("Usunąć wszystkie notatki?") },
            text = { Text("Notatki przestaną być przekazywane do modelu. Historia czatu nie zostanie usunięta.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { store.clearNotes() }
                        notes.clear()
                        showClearNotesDialog = false
                    }
                }) { Text("USUŃ NOTATKI") }
            },
            dismissButton = { TextButton(onClick = { showClearNotesDialog = false }) { Text("ANULUJ") } }
        )
    }
}
