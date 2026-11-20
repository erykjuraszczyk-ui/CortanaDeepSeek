# Cortana DeepSeek 1.0.0 — Local AI for Android

Finalne wydanie Android 13+ (API 33–35), Kotlin + Jetpack Compose. Aplikacja łączy się z lokalnym silnikiem llama.cpp zgodnym z API OpenAI.

## Funkcje 1.0.0

- Lokalny czat AI — bez wysyłania promptów do publicznego API.
- Historia SQLite — do 500 wiadomości zachowanych po zamknięciu aplikacji.
- Pamięć jawna — notatki zapisane świadomie przez użytkownika; każdą można usunąć osobno lub skasować wszystkie.
- Trwałe ustawienia — endpoint, identyfikator modelu i instrukcja systemowa.
- Test serwera przez endpoint /health.
- Opcjonalne rozpoznawanie polskiej mowy i odczyt odpowiedzi przez Android TTS.
- Evolution Core — kontrolowany proces propozycji zmian, testów i zatwierdzania.
- Blokowanie publicznych hostów; dozwolone localhost, .local i lokalne/prywatne adresy IP.

## Ważne ograniczenie modelu

APK nie zawiera wag GGUF ani skompilowanego runtime llama.cpp. Na urządzeniu musi działać osobny lokalny serwer kompatybilny z API OpenAI. Wydanie 1.0.0 nie jest samodzielnym pakietem z wbudowanym modelem. Model nie wykonuje kodu systemowego ani nie podmienia aplikacji.

Na urządzeniu z 12 GB RAM zacznij od modelu 1.5B–3B w kwantyzacji Q4. Większe modele mogą być wolniejsze i zużywać więcej pamięci.

Przykład uruchomienia serwera:

~~~bash
llama-server -m /ścieżka/model.gguf --host 127.0.0.1 --port 8080 -c 2048 -ngl 0
~~~

Ustaw w aplikacji http://127.0.0.1:8080. Dla komputera w tej samej sieci użyj jego prywatnego IP. Nie wystawiaj portu do internetu.

## Budowanie i instalacja

GitHub Actions → Build CortanaDeepSeek APK → Run workflow.

- Artefakt CortanaDeepSeek-1.0.0-debug zawiera instalowalny debug APK podpisany kluczem CI.
- Artefakt CortanaDeepSeek-1.0.0-release-unsigned jest niepodpisany i wymaga podpisania własnym kluczem przed dystrybucją.

Wymagania buildu: JDK 17, Android SDK 35, Gradle 8.9.

## Prywatność

- Wiadomości i notatki pozostają w prywatnej bazie aplikacji.
- Brak synchronizacji chmurowej i publicznego klucza API.
- Klient blokuje publiczne hosty; nie podąża za przekierowaniami HTTP.
- Mikrofon jest używany dopiero po akcji użytkownika i przyznaniu zgody.
- Rozpoznawanie mowy i TTS mogą zależeć od usług systemowych; offline ASR/TTS nie jest gwarantowany.
- Odinstalowanie usuwa lokalną bazę aplikacji.

## Samodoskonalenie

Evolution Core realizuje cykl: analiza → propozycja patcha → testy → przegląd → zgoda → nowy build. Model nie wykonuje kodu, nie ma dostępu do powłoki i nie instaluje samodzielnie aktualizacji.
