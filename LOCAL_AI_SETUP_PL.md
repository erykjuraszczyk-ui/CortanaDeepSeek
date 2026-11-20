# Lokalny silnik AI

1. Zainstaluj llama.cpp w Termux zgodnie z oficjalną dokumentacją projektu.
2. Pobierz legalnie udostępniony model GGUF; dla 12 GB RAM zacznij od 1.5B–3B Q4.
3. Uruchom: llama-server -m /ścieżka/model.gguf --host 127.0.0.1 --port 8080 -c 2048 -ngl 0
4. W aplikacji ustaw endpoint http://127.0.0.1:8080.
5. Nie wystawiaj portu 8080 do internetu.

APK nie zawiera wag modelu. To klient lokalnego serwera zgodnego z API OpenAI; integracja llama.cpp przez JNI/NDK bezpośrednio w procesie aplikacji jest osobnym etapem.
