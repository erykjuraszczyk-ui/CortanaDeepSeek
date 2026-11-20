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
class MainActivity : ComponentActivity() { override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { CortanaApp() } } }

@Composable fun CortanaApp() {
 val cyan=Color(0xFF54E8FF); val bg=Color(0xFF050B18)
 var endpoint by remember { mutableStateOf("http://127.0.0.1:8080") }; var model by remember { mutableStateOf("local-model") }
 var input by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; var status by remember { mutableStateOf("LOCAL CORE • OCZEKUJE NA SILNIK") }
 val messages=remember { mutableStateListOf(ChatLine("assistant","Cortana DeepSeek gotowa. Uruchom lokalny llama-server, aby rozpocząć rozmowę offline.")) }
 val scope=rememberCoroutineScope(); val client=remember { OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).readTimeout(180,TimeUnit.SECONDS).build() }
 MaterialTheme(colorScheme=darkColorScheme(primary=cyan,background=bg,surface=Color(0xFF101B2C))) {
  Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF07172A),bg))).padding(16.dp)) {
   Text("CORTANA",color=cyan,fontSize=29.sp,letterSpacing=7.sp); Text("DEEPSEEK • LOCAL INTELLIGENCE",color=Color.LightGray,fontSize=11.sp)
   Spacer(Modifier.height(14.dp))
   Box(Modifier.fillMaxWidth().height(130.dp).background(Color(0x1826DFFF),RoundedCornerShape(24.dp)),contentAlignment=Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("◉",color=cyan,fontSize=64.sp); Text("HOLOGRAPHIC CORE",color=cyan,fontSize=10.sp,letterSpacing=3.sp) } }
   Text(status,color=cyan,fontSize=10.sp)
   OutlinedTextField(endpoint,{endpoint=it},label={Text("Lokalny endpoint")},singleLine=true,modifier=Modifier.fillMaxWidth())
   OutlinedTextField(model,{model=it},label={Text("Nazwa modelu")},singleLine=true,modifier=Modifier.fillMaxWidth())
   LazyColumn(Modifier.weight(1f).fillMaxWidth()) { items(messages) { line -> Card(Modifier.fillMaxWidth().padding(vertical=4.dp),colors=CardDefaults.cardColors(containerColor=if(line.role=="user") Color(0xFF12344A) else Color(0xFF101B2C))) { Column(Modifier.padding(12.dp)) { Text(if(line.role=="user") "TY" else "CORTANA",color=cyan,fontSize=10.sp); Text(line.text,color=Color.White) } } } }
   OutlinedTextField(input,{input=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Napisz do Cortany…")})
   Button(onClick={
    val prompt=input.trim(); if(prompt.isNotEmpty() && !busy) { messages.add(ChatLine("user",prompt)); input=""; busy=true; status="LOCAL CORE • PRZETWARZANIE"
     scope.launch { val answer=withContext(Dispatchers.IO) { try {
      val history=JSONArray().put(JSONObject().put("role","system").put("content","Jesteś Cortaną, polskojęzyczną asystentką AI. Odpowiadaj jasno i rzeczowo."))
      messages.takeLast(20).forEach { history.put(JSONObject().put("role",it.role).put("content",it.text)) }
      val payload=JSONObject().put("model",model).put("messages",history).put("temperature",0.7)
      val req=Request.Builder().url(endpoint.trimEnd('/')+"/v1/chat/completions").post(payload.toString().toRequestBody("application/json".toMediaType())).build()
      client.newCall(req).execute().use { response -> if(!response.isSuccessful) error("HTTP ${response.code}"); JSONObject(response.body?.string()?:"{}").getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content") }
     } catch(e:Exception) { "Brak połączenia z lokalnym silnikiem: ${e.message}. Uruchom llama-server na 127.0.0.1:8080." } }; messages.add(ChatLine("assistant",answer)); busy=false; status="LOCAL CORE • GOTOWY" }
    }
   },enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text(if(busy) "PRZETWARZANIE…" else "WYŚLIJ DO CORTANY") }
   Text("OFFLINE INFERENCE • NO CLOUD API • v0.1.0",color=Color.Gray,fontSize=9.sp,modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=8.dp))
  }
 }
}
