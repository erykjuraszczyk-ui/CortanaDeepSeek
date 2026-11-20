# Lokalny silnik AI — Cortana DeepSeek 1.0.0

## Serwer na tym samym telefonie

1. Przygotuj Termux i lokalny build llama.cpp odpowiedni dla architektury telefonu.
2. Pobierz model GGUF z zaufanego źródła. Zacznij od modelu 1.5B–3B Q4.
3. Uruchom serwer:

    llama-server -m /ścieżka/model.gguf --host 127.0.0.1 --port 8080 -c 2048 -ngl 0

4. W Cortana DeepSeek → Ustawienia wpisz http://127.0.0.1:8080.
5. Zapisz ustawienia i wybierz Testuj połączenie.

## Serwer na komputerze w tej samej sieci

1. Uruchom llama-server na komputerze i skonfiguruj nasłuchiwanie w sieci lokalnej.
2. Ogranicz zaporę do prywatnej sieci LAN. Nie przekierowuj portu na routerze.
3. W aplikacji wpisz prywatny adres IP komputera, np. http://192.168.1.50:8080.
4. Zapisz ustawienia i przetestuj połączenie.

## Rozwiązywanie problemów

- Connection refused: serwer nie działa lub nasłuchuje na innym porcie/interfejsie.
- HTTP 404 w teście: serwer nie udostępnia endpointu /health; sprawdź wersję i konfigurację llama.cpp.
- HTTP 400/500 przy czacie: sprawdź identyfikator modelu i endpoint /v1/chat/completions.
- Public host blocked: aplikacja dopuszcza tylko localhost i prywatne numeryczne adresy IP.

APK nie zawiera modelu GGUF ani runtime llama.cpp. Inferencja tekstowa jest lokalna po uruchomieniu serwera. Dostępność polskiego ASR/TTS oraz trybu offline zależy od usług Androida.
