# Cortana DeepSeek 1.0.0 — Local AI for Android

Natywna aplikacja Android 13+ (API 33–35), Kotlin + Jetpack Compose. To klient lokalnego silnika AI zgodnego z API OpenAI.

## Funkcje wydania 1.0.0

- Lokalny czat AI — komunikacja z lokalnym llama-server.
- Historia SQLite — do 500 ostatnich wiadomości zapisywanych w prywatnej bazie aplikacji.
- Pamięć jawna — notatki dodawane świadomie przez użytkownika; można usuwać pojedyncze wpisy lub całą pamięć.
- Ustawienia trwałe — adres serwera, identyfikator modelu i prompt systemowy są zapamiętywane.
- Test połączenia — kontrola endpointu /health.
- Mowa — opcjonalne rozpoznawanie polskiej mowy i odczytywanie odpowiedzi przez Android TTS. Dostępność i działanie offline zależą od usług zainstalowanych w telefonie.
- Evolution Core — kontrolowany proces analizy, propozycji patcha, testowania i zatwierdzania zmian.
- Ochrona sieciowa — blokowanie publicznych hostów; dozwolone jest localhost i prywatne adresy IP.

## Wymagany silnik lokalny

APK nie zawiera wag GGUF ani runtime llama.cpp. Na urządzeniu musi działać osobny llama-server kompatybilny z /v1/chat/completions.

Na urządzeniu z 12 GB RAM rozpocznij od modelu 1.5B–3B w kwantyzacji Q4. Większe modele mogą być wolniejsze i zużywać więcej pamięci.

Przykład uruchomienia:

    llama-server -m /ścieżka/model.gguf --host 127.0.0.1 --port 8080 -c 2048 -ngl 0

W aplikacji ustaw http://127.0.0.1:8080. Dla serwera na komputerze w tej samej sieci użyj jego prywatnego adresu IP, np. http://192.168.1.50:8080. Nie wystawiaj portu do internetu.

## Budowanie

GitHub Actions → Build CortanaDeepSeek APK → Run workflow.

- CortanaDeepSeek-1.0.0-debug — testowy APK debug.
- CortanaDeepSeek-1.0.0-release-unsigned — APK release bez podpisu; przed dystrybucją wymaga podpisania własnym kluczem.

CI uruchamia JVM unit tests, assembleDebug i assembleRelease. Wymagania: JDK 17, Android SDK 35, Gradle 8.9.

## Prywatność

- Historia i notatki pozostają w prywatnej bazie na urządzeniu.
- Brak synchronizacji chmurowej i klucza publicznego API.
- Klient sieciowy odrzuca publiczne hosty.
- O mikrofon aplikacja prosi dopiero po użyciu przycisku głosowego.
- ASR/TTS może korzystać z usług systemowych; pełny offline dla głosu nie jest gwarantowany.
- Odinstalowanie aplikacji usuwa jej lokalną bazę.

## Evolution Core

Model nie wykonuje kodu, nie uruchamia poleceń powłoki i nie instaluje aktualizacji. Zmiany przechodzą przez: analiza → propozycja → testy → przegląd → akceptacja → CI. Klucz podpisujący wydania musi być przechowywany poza repozytorium.

## Ograniczenia

Interfejs holograficzny jest stylizacją 2D Compose, nie renderowanym hologramem 3D. Aplikacja nie zawiera jeszcze silnika GGUF osadzonego w APK przez JNI/NDK.
