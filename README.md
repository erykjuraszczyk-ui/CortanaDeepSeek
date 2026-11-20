# Cortana DeepSeek — Local AI Android

Natywna aplikacja Android 13+ (minSdk 33), Kotlin + Jetpack Compose. Tryb Local AI nie wysyła rozmów do DeepSeek API ani chmury.

## v0.1.0
- Holograficzny ekran i czat.
- Klient lokalnego serwera llama.cpp, zgodnego z OpenAI API: http://127.0.0.1:8080/v1/chat/completions.
- Brak klucza API i telemetrii.
- Evolution Core: propozycje zmian, testy i ręczna akceptacja; bez samomodyfikacji APK.

## Lokalny model
APK jest klientem lokalnego silnika, nie zawiera wag. Model GGUF i llama-server uruchamiasz lokalnie. Na RedMagic 7 (12 GB RAM) zacznij od modelu 1.5B–3B Q4; większe mogą być wolne i pamięciożerne. Przykładowe uruchomienie: llama-server -m /ścieżka/model.gguf --host 127.0.0.1 --port 8080 -c 2048 -ngl 0. Nie wystawiaj portu do internetu.

## Budowanie
GitHub Actions: Actions → Build CortanaDeepSeek APK → Run workflow. Artefakt CortanaDeepSeek-debug zawiera APK debug. Wymagany JDK 17 i Android SDK 35.

## Bezpieczeństwo
Nie przechowuj sekretów w repo. Aktualizacje instaluj z zaufanego źródła. Kod wygenerowany przez AI wymaga przeglądu i testów.