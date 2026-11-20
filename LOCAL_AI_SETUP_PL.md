# Lokalny silnik AI — Cortana DeepSeek 1.0.0

## Serwer na tym samym telefonie

1. Przygotuj lokalny build llama.cpp dla architektury telefonu.
2. Pobierz model GGUF z zaufanego źródła; zacznij od 1.5B–3B Q4.
3. Uruchom:

~~~bash
llama-server -m /ścieżka/model.gguf --host 127.0.0.1 --port 8080 -c 2048 -ngl 0
~~~

4. W Cortana DeepSeek → Ustawienia wpisz http://127.0.0.1:8080.
5. Wybierz Zapisz ustawienia i Testuj połączenie.

## Serwer na komputerze w tej samej sieci

1. Uruchom llama-server na komputerze i ogranicz zaporą dostęp do sieci lokalnej.
2. Nie przekierowuj portu na routerze i nie wystawiaj serwera do internetu.
3. W aplikacji wpisz prywatny adres IP komputera, np. http://192.168.1.50:8080.
4. Zapisz ustawienia i przetestuj połączenie.

## Problemy

- Connection refused: serwer nie działa, port jest inny lub interfejs nasłuchu jest nieprawidłowy.
- HTTP 404 w teście: serwer nie udostępnia /health albo ma inną konfigurację.
- HTTP 400/500 przy czacie: sprawdź nazwę modelu i endpoint /v1/chat/completions.
- Public host blocked: aplikacja akceptuje tylko localhost, nazwy .local i prywatne adresy IP.

APK nie zawiera modelu GGUF ani runtime llama.cpp. Inferencja tekstowa jest lokalna po uruchomieniu serwera. Offline ASR/TTS zależy od usług systemowych Androida.
