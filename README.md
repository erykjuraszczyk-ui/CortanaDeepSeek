# Cortana DeepSeek — Local AI Android

Natywna aplikacja Android 13+ (minSdk 33), Kotlin + Jetpack Compose. Tryb Local AI korzysta z lokalnego serwera zgodnego z OpenAI API.

## v0.2.0
- Holograficzny ekran i czat.
- Lokalny klient llama.cpp: `http://127.0.0.1:8080/v1/chat/completions`.
- **Local Memory:** historia rozmów zapisywana w prywatnej bazie SQLite na urządzeniu.
- Ostatnie 500 wiadomości jest odtwarzanych po ponownym uruchomieniu aplikacji.
- Przycisk „Wyczyść pamięć” usuwa lokalną historię po potwierdzeniu.
- Bez synchronizacji historii z chmurą; brak klucza API i telemetrii.
- Evolution Core: propozycje zmian, testy i ręczna akceptacja; bez samomodyfikacji APK.

## Lokalny model
APK jest klientem lokalnego silnika, nie zawiera wag GGUF ani runtime llama.cpp. Model i `llama-server` uruchamiasz osobno. Na RedMagic 7 (12 GB RAM) zacznij od modelu 1.5B–3B Q4; większe mogą być wolne i pamięciożerne.

Przykład uruchomienia w środowisku z llama.cpp:
```bash
llama-server -m /ścieżka/model.gguf --host 127.0.0.1 --port 8080 -c 2048 -ngl 0
```
Nie wystawiaj portu serwera do internetu.

## Budowanie
GitHub Actions → workflow **Build CortanaDeepSeek APK** → Run workflow. Artefakt `CortanaDeepSeek-debug` zawiera APK debug. Wymagany JDK 17 i Android SDK 35.

## Bezpieczeństwo i prywatność
- Historia pozostaje w prywatnym katalogu danych aplikacji.
- Odinstalowanie aplikacji usuwa jej lokalną bazę.
- „Wyczyść pamięć” usuwa historię rozmów.
- Nie zapisuj sekretów w repozytorium.
- Kod wygenerowany przez AI wymaga przeglądu i testów; aktualizacje instaluj z zaufanego źródła.

## Ograniczenia tej wersji
Pamięć jest lokalnym archiwum rozmów, a nie mechanizmem semantycznego przypominania. APK nie zawiera jeszcze wbudowanego silnika GGUF, rozmowy głosowej ani hologramu 3D.
